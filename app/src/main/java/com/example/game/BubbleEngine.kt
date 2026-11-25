package com.example.game

import com.example.data.BoosterType
import com.example.data.BubbleCell
import com.example.data.BubbleColor
import com.example.data.EndReason
import com.example.data.GRID_COLS
import com.example.data.LevelConfig
import com.example.data.LevelGenerator
import com.example.data.LevelObjective
import com.example.data.SavedSession
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

enum class EnginePhase {
  READY,
  BUSY,
  ENDED,
}

data class Projectile(
  var x: Float,
  var y: Float,
  var vx: Float,
  var vy: Float,
  val speed: Float,
  val boosterType: BoosterType?,
  val color: BubbleColor,
  val fireHits: MutableSet<Int> = HashSet(),
  val fireRemoved: MutableList<RemovedCellInfo> = mutableListOf(),
)

data class RemovedCellInfo(
  val r: Int,
  val c: Int,
  val cell: BubbleCell,
  val x: Float,
  val y: Float,
)

data class PopEffect(
  val x: Float,
  val y: Float,
  val cell: BubbleCell,
  var delay: Float,
  var elapsed: Float = 0f,
  var burstSpawned: Boolean = false,
)

data class FallEffect(
  var x: Float,
  var y: Float,
  var vx: Float,
  var vy: Float,
  val cell: BubbleCell,
  var delay: Float,
)

data class Particle(
  var x: Float,
  var y: Float,
  var vx: Float,
  var vy: Float,
  var life: Float,
  val maxLife: Float,
  val color: BubbleColor?,
  val radius: Float,
)

data class FloatingText(
  var x: Float,
  var y: Float,
  val text: String,
  val isBig: Boolean,
  var elapsed: Float = 0f,
)

data class BeamEffect(
  val x: Float,
  val y: Float = 0f,
  val maxRadius: Float = 0f,
  val isRing: Boolean,
  var elapsed: Float = 0f,
)

data class SparkleEffect(
  val x: Float,
  val y: Float,
  var elapsed: Float = 0f,
)

data class TrajectoryPoint(val x: Float, val y: Float, val isTarget: Boolean)

interface EngineAudioCallbacks {
  fun onShoot(isFire: Boolean)
  fun onBounce()
  fun onStick()
  fun onPop(index: Int)
  fun onFall()
  fun onCombo(combo: Int)
  fun onBoom()
  fun onZap()
  fun onRainbow()
  fun onFreeze()
  fun onRowDrop()
  fun onSwap()
  fun onVibrate(ms: Long)
  fun onBubblesScored(bubblesCleared: Int, combo: Int)
  fun onSessionChanged(session: SavedSession)
  fun onGameEnded(won: Boolean, reason: EndReason, stars: Int, score: Int)
}

class BubbleEngine(
  val levelConfig: LevelConfig,
  bonusShots: Int,
  restoredSession: SavedSession?,
  private val callbacks: EngineAudioCallbacks,
) {
  var width: Float = 360f
    private set
  var height: Float = 600f
    private set
  var bubbleRadius: Float = 16f
    private set
  var gridOriginX: Float = 0f
    private set
  var gridOriginY: Float = 8f
    private set
  var rowHeight: Float = 27.7f
    private set
  var shooterX: Float = 180f
    private set
  var shooterY: Float = 540f
    private set
  var dangerY: Float = 470f
    private set
  var dangerRow: Int = 10
    private set

  val grid: MutableList<MutableList<BubbleCell?>> = if (restoredSession != null && restoredSession.grid.isNotEmpty()) {
    restoredSession.grid.map { it.toMutableList() }.toMutableList()
  } else {
    levelConfig.grid.map { it.toMutableList() }.toMutableList()
  }

  var parity: Int = restoredSession?.parity ?: 0
    private set
  var shotsLeft: Int = restoredSession?.shotsLeft ?: (levelConfig.shots + bonusShots)
    private set
  var shotsMax: Int = restoredSession?.shotsMax ?: (levelConfig.shots + bonusShots)
    private set
  var score: Int = restoredSession?.score ?: 0
    private set
  var removedCount: Int = restoredSession?.removedCount ?: 0
    private set
  var colorClearedCount: Int = restoredSession?.colorClearedCount ?: 0
    private set
  var combo: Int = restoredSession?.combo ?: 0
    private set
  var maxCombo: Int = restoredSession?.maxCombo ?: 0
    private set
  var missCount: Int = restoredSession?.missCount ?: 0
    private set
  var freezeShots: Int = restoredSession?.freezeShots ?: 0
    private set
  var usedBooster: Boolean = restoredSession?.usedBooster ?: false
    private set

  var currentColor: BubbleColor = restoredSession?.currentColor ?: BubbleColor.RED
    private set
  var nextColor: BubbleColor = restoredSession?.nextColor ?: BubbleColor.BLUE
    private set

  var armedBooster: BoosterType? = null
  var phase: EnginePhase = EnginePhase.READY
    private set
  var won: Boolean = false
    private set
  var endReason: EndReason = EndReason.NONE
    private set
  var continuesUsed: Int = 0
    private set
  var doubledCoins: Boolean = false
  var lastCoinsAwarded: Int = 0

  var projectile: Projectile? = null
    private set
  val pops = mutableListOf<PopEffect>()
  val falls = mutableListOf<FallEffect>()
  val particles = mutableListOf<Particle>()
  val floatingTexts = mutableListOf<FloatingText>()
  val beams = mutableListOf<BeamEffect>()
  val sparkles = mutableListOf<SparkleEffect>()

  var shiftY: Float = 0f
    private set
  var aimAngleRad: Float = (PI / 2.0).toFloat()
    private set
  var isAiming: Boolean = false
  var inCancelZone: Boolean = false
  var reloadTimer: Float = 0f
    private set
  var elapsedTime: Float = 0f
    private set
  private var sparkleCooldown: Float = 0.6f
  private var lastShotMs: Long = 0L

  init {
    if (restoredSession == null) {
      currentColor = pickRandomAvailableColor()
      nextColor = pickRandomAvailableColor()
    } else {
      fixShooterColors()
    }
    floatingTexts.add(
      FloatingText(
        x = width / 2f,
        y = height * 0.45f,
        text = if (levelConfig.isDaily) "Daily Challenge" else "Level ${levelConfig.levelNumber}",
        isBig = true,
      ),
    )
  }

  fun updateBounds(w: Float, h: Float) {
    if (w <= 0f || h <= 0f) return
    val changed = abs(w - width) > 1f || abs(h - height) > 1f
    width = w
    height = h
    bubbleRadius = max(8f, floor((w / (2f * GRID_COLS + 1f)) * 10f) / 10f)
    gridOriginX = (w - (2f * bubbleRadius * GRID_COLS + bubbleRadius)) / 2f
    gridOriginY = 8f
    rowHeight = bubbleRadius * 1.732f
    shooterX = w / 2f
    shooterY = h - bubbleRadius * 2.7f
    dangerY = shooterY - bubbleRadius * 3.6f
    dangerRow = max(6, floor((dangerY - gridOriginY - bubbleRadius) / rowHeight).toInt())
    if (changed && floatingTexts.size == 1 && elapsedTime < 0.25f) {
      val first = floatingTexts[0]
      floatingTexts[0] = first.copy(x = w / 2f, y = h * 0.45f)
    }
  }

  fun cellX(r: Int, c: Int): Float =
    gridOriginX + bubbleRadius + c * 2f * bubbleRadius + if (((r + parity) and 1) == 1) bubbleRadius else 0f

  fun cellY(r: Int): Float = gridOriginY + bubbleRadius + r * rowHeight + shiftY

  fun starsNow(): Int {
    if (shotsMax <= 0) return 1
    val ratio = shotsLeft.toFloat() / shotsMax.toFloat()
    return when {
      ratio >= 0.45f -> 3
      ratio >= 0.25f -> 2
      else -> 1
    }
  }

  fun remainingBubbleCount(): Int {
    var count = 0
    for (row in grid) {
      for (cell in row) {
        if (cell != null && !cell.isStone) count++
      }
    }
    return count
  }

  fun isDangerWarning(): Boolean {
    val startRow = max(0, dangerRow - 2)
    for (r in startRow until grid.size) {
      for (c in 0 until GRID_COLS) {
        if (grid[r][c] != null) return true
      }
    }
    return false
  }

  fun aimTowards(x: Float, y: Float) {
    val dx = x - shooterX
    val dy = shooterY - y
    var angle = atan2(dy, dx)
    if (dy < bubbleRadius * 0.4f) {
      angle = if (dx >= 0f) 0.2f else (PI - 0.2).toFloat()
    }
    aimAngleRad = angle.coerceIn(0.2f, (PI - 0.2).toFloat())
  }

  fun isTouchNearShooterOrNext(x: Float, y: Float): Boolean {
    val dShooter = hypot(x - shooterX, y - shooterY)
    val nextX = shooterX - bubbleRadius * 3.7f
    val nextY = shooterY + bubbleRadius * 0.9f
    val dNext = hypot(x - nextX, y - nextY)
    return dShooter < bubbleRadius * 1.55f || dNext < bubbleRadius * 1.35f
  }

  fun swapBubbles(): Boolean {
    if (phase != EnginePhase.READY || armedBooster != null) return false
    val tmp = currentColor
    currentColor = nextColor
    nextColor = tmp
    reloadTimer = 0.15f
    callbacks.onSwap()
    return true
  }

  fun activateFreeze(): Boolean {
    if (phase != EnginePhase.READY || freezeShots > 0) return false
    freezeShots = 5
    usedBooster = true
    callbacks.onFreeze()
    callbacks.onVibrate(30)
    floatingTexts.add(FloatingText(width / 2f, height * 0.4f, "Frozen!", isBig = true))
    callbacks.onSessionChanged(exportSession())
    return true
  }

  fun shoot(consumeBooster: (BoosterType) -> Boolean): Boolean {
    val now = System.currentTimeMillis()
    if (phase != EnginePhase.READY || projectile != null || shotsLeft <= 0 || now - lastShotMs < 140L) {
      return false
    }
    lastShotMs = now
    val booster = armedBooster
    if (booster != null) {
      if (!consumeBooster(booster)) {
        armedBooster = null
        return false
      }
      usedBooster = true
      armedBooster = null
    } else {
      val firedColor = currentColor
      currentColor = nextColor
      nextColor = pickRandomAvailableColor()
      projectile = Projectile(
        x = shooterX,
        y = shooterY,
        vx = cos(aimAngleRad),
        vy = -sin(aimAngleRad),
        speed = bubbleRadius * 52f,
        boosterType = null,
        color = firedColor,
      )
      shotsLeft--
      reloadTimer = 0.2f
      phase = EnginePhase.BUSY
      callbacks.onShoot(isFire = false)
      callbacks.onVibrate(8)
      return true
    }

    projectile = Projectile(
      x = shooterX,
      y = shooterY,
      vx = cos(aimAngleRad),
      vy = -sin(aimAngleRad),
      speed = bubbleRadius * 52f,
      boosterType = booster,
      color = currentColor,
    )
    shotsLeft--
    reloadTimer = 0.2f
    phase = EnginePhase.BUSY
    callbacks.onShoot(isFire = booster == BoosterType.FIRE)
    callbacks.onVibrate(12)
    return true
  }

  fun continueAfterExtraShots() {
    if (phase != EnginePhase.ENDED || won) return
    continuesUsed++
    shotsLeft += 5
    if (endReason == EndReason.DANGER_LINE) {
      val toClear = mutableListOf<LevelGenerator.GridPos>()
      val start = max(0, dangerRow - 4)
      for (r in start until grid.size) {
        for (c in 0 until GRID_COLS) {
          if (grid[r][c] != null) toClear.add(LevelGenerator.GridPos(r, c))
        }
      }
      toClear.forEachIndexed { idx, (r, c) -> removeCell(r, c, isPop = true, delay = idx * 0.015f) }
      LevelGenerator.findFloatingBubbles(grid, parity).forEachIndexed { idx, (r, c) ->
        removeCell(r, c, isPop = false, delay = 0.1f + idx * 0.02f)
      }
    }
    phase = EnginePhase.READY
    endReason = EndReason.NONE
    fixShooterColors()
    callbacks.onSessionChanged(exportSession())
  }

  fun computeTrajectory(): List<TrajectoryPoint> {
    val lx = gridOriginX + bubbleRadius
    val rx = width - gridOriginX - bubbleRadius
    var x = shooterX
    var y = shooterY
    var vx = cos(aimAngleRad)
    val vy = -sin(aimAngleRad)
    var bounces = 0
    val step = bubbleRadius * 0.4f
    val pts = mutableListOf<TrajectoryPoint>()

    for (i in 0 until 360) {
      x += vx * step
      y += vy * step
      if (x < lx) {
        x = lx
        vx = -vx
        bounces++
      } else if (x > rx) {
        x = rx
        vx = -vx
        bounces++
      }
      if (y <= gridOriginY + bubbleRadius) {
        pts.add(TrajectoryPoint(x, gridOriginY + bubbleRadius, isTarget = true))
        break
      }
      if (findHit(x, y) != null) {
        pts.add(TrajectoryPoint(x, y, isTarget = true))
        break
      }
      if (i > 3 && i % 3 == 0) {
        pts.add(TrajectoryPoint(x, y, isTarget = false))
      }
      if (bounces > 4) break
    }
    return pts
  }

  fun step(dtRaw: Float) {
    val dt = dtRaw.coerceIn(0f, 0.033f)
    elapsedTime += dt
    if (reloadTimer > 0f) reloadTimer = max(0f, reloadTimer - dt)
    if (shiftY != 0f) {
      shiftY += (-shiftY) * min(1f, dt * 9f)
      if (abs(shiftY) < 0.3f) shiftY = 0f
    }

    if (projectile != null) {
      stepProjectile(dt)
    }

    for (i in pops.indices.reversed()) {
      val p = pops[i]
      if (p.delay > 0f) {
        p.delay -= dt
        continue
      }
      if (!p.burstSpawned) {
        p.burstSpawned = true
        spawnBurst(p.x, p.y, if (p.cell.isStone) null else p.cell.color, 5)
      }
      p.elapsed += dt
      if (p.elapsed > 0.2f) pops.removeAt(i)
    }

    for (i in falls.indices.reversed()) {
      val f = falls[i]
      if (f.delay > 0f) {
        f.delay -= dt
        continue
      }
      f.vy += bubbleRadius * 95f * dt
      f.y += f.vy * dt
      f.x += f.vx * dt
      if (f.y > height + bubbleRadius * 2f) falls.removeAt(i)
    }

    for (i in particles.indices.reversed()) {
      val p = particles[i]
      p.life -= dt
      if (p.life <= 0f) {
        particles.removeAt(i)
        continue
      }
      p.x += p.vx * dt
      p.y += p.vy * dt
      p.vy += bubbleRadius * 30f * dt
    }

    for (i in floatingTexts.indices.reversed()) {
      val ft = floatingTexts[i]
      ft.elapsed += dt
      ft.y -= bubbleRadius * (if (ft.isBig) 0.5f else 1.3f) * dt
      if (ft.elapsed > (if (ft.isBig) 1.4f else 1.0f)) floatingTexts.removeAt(i)
    }

    for (i in beams.indices.reversed()) {
      val b = beams[i]
      b.elapsed += dt
      if (b.elapsed > 0.45f) beams.removeAt(i)
    }

    sparkleCooldown -= dt
    if (sparkleCooldown <= 0f) {
      sparkleCooldown = 0.55f + Random.nextFloat() * 0.55f
      if (grid.isNotEmpty()) {
        val r = Random.nextInt(grid.size)
        val c = Random.nextInt(GRID_COLS)
        if (grid[r][c] != null) {
          sparkles.add(SparkleEffect(cellX(r, c) - bubbleRadius * 0.3f, cellY(r) - bubbleRadius * 0.4f))
        }
      }
    }
    for (i in sparkles.indices.reversed()) {
      sparkles[i].elapsed += dt
      if (sparkles[i].elapsed > 0.5f) sparkles.removeAt(i)
    }
  }

  private fun stepProjectile(dt: Float) {
    val p = projectile ?: return
    val lx = gridOriginX + bubbleRadius
    val rx = width - gridOriginX - bubbleRadius
    var remaining = p.speed * dt

    while (remaining > 0f && projectile != null) {
      val stepDist = min(remaining, bubbleRadius * 0.4f)
      remaining -= stepDist
      p.x += p.vx * stepDist
      p.y += p.vy * stepDist

      if (p.x < lx) {
        p.x = lx
        p.vx = abs(p.vx)
        callbacks.onBounce()
      } else if (p.x > rx) {
        p.x = rx
        p.vx = -abs(p.vx)
        callbacks.onBounce()
      }

      if (p.boosterType == BoosterType.FIRE && Random.nextFloat() < 0.55f && particles.size < 220) {
        particles.add(
          Particle(
            x = p.x,
            y = p.y,
            vx = (Random.nextFloat() - 0.5f) * bubbleRadius * 4f,
            vy = bubbleRadius * 3f,
            life = 0.4f,
            maxLife = 0.4f,
            color = BubbleColor.ORANGE,
            radius = bubbleRadius * 0.22f,
          ),
        )
      }

      if (p.y <= gridOriginY + bubbleRadius) {
        p.y = gridOriginY + bubbleRadius
        landProjectile(null)
        return
      }

      val hit = findHit(p.x, p.y)
      if (hit != null) {
        if (p.boosterType == BoosterType.FIRE) {
          val key = hit.r * GRID_COLS + hit.c
          if (p.fireHits.add(key)) {
            val popped = popGroup(listOf(hit))
            p.fireRemoved.addAll(popped)
            if (p.fireHits.size >= 6) {
              projectile = null
              finishShot(p.fireRemoved)
              return
            }
          }
          continue
        }
        landProjectile(hit)
        return
      }
    }
  }

  private fun findHit(x: Float, y: Float): LevelGenerator.GridPos? {
    val limitSq = (bubbleRadius * 1.8f) * (bubbleRadius * 1.8f)
    val r0 = max(0, floor((y - gridOriginY - shiftY) / rowHeight).toInt() - 2)
    val r1 = min(grid.size - 1, r0 + 5)
    var best: LevelGenerator.GridPos? = null
    var bestDist = Float.MAX_VALUE
    for (r in r0..r1) {
      for (c in 0 until GRID_COLS) {
        if (grid[r][c] == null) continue
        val dx = cellX(r, c) - x
        val dy = cellY(r) - y
        val dSq = dx * dx + dy * dy
        if (dSq < limitSq && dSq < bestDist) {
          bestDist = dSq
          best = LevelGenerator.GridPos(r, c)
        }
      }
    }
    return best
  }

  private fun landProjectile(hit: LevelGenerator.GridPos?) {
    val p = projectile ?: return
    projectile = null

    when (p.boosterType) {
      BoosterType.LIGHTNING -> {
        beams.add(BeamEffect(x = p.x, isRing = false))
        callbacks.onZap()
        callbacks.onVibrate(40)
        val colTargets = mutableListOf<LevelGenerator.GridPos>()
        for (r in grid.indices) {
          for (c in 0 until GRID_COLS) {
            if (grid[r][c] != null && abs(cellX(r, c) - p.x) < bubbleRadius * 1.08f) {
              colTargets.add(LevelGenerator.GridPos(r, c))
            }
          }
        }
        finishShot(popGroup(colTargets))
        return
      }
      BoosterType.BOMB -> {
        triggerBoomFx(p.x, p.y, bubbleRadius * 3.2f)
        val radiusSq = (bubbleRadius * 3.2f) * (bubbleRadius * 3.2f)
        val blastTargets = mutableListOf<LevelGenerator.GridPos>()
        for (r in grid.indices) {
          for (c in 0 until GRID_COLS) {
            if (grid[r][c] != null) {
              val dx = cellX(r, c) - p.x
              val dy = cellY(r) - p.y
              if (dx * dx + dy * dy <= radiusSq) {
                blastTargets.add(LevelGenerator.GridPos(r, c))
              }
            }
          }
        }
        finishShot(popGroup(blastTargets))
        return
      }
      BoosterType.FIRE -> {
        finishShot(p.fireRemoved)
        return
      }
      else -> Unit
    }

    val pos = (if (hit != null) bestEmptyAdjacent(hit, p.x, p.y) else topEmptyCell(p.x))
      ?: nearestValidEmpty(p.x, p.y)
    val (r, c) = pos
    while (grid.size <= r) {
      grid.add(MutableList(GRID_COLS) { null })
    }

    if (p.boosterType == BoosterType.RAINBOW) {
      callbacks.onRainbow()
      val seen = HashSet<Int>().apply { add(r * GRID_COLS + c) }
      val toPop = mutableListOf(LevelGenerator.GridPos(r, c))
      val adjColors = mutableSetOf<BubbleColor>()
      for ((rr, cc) in LevelGenerator.adjacentCoords(r, c, parity)) {
        val adjCell = LevelGenerator.cellAt(grid, rr, cc)
        if (adjCell != null && !adjCell.isStone) adjColors.add(adjCell.color)
      }
      var bestColor: BubbleColor? = null
      var bestSize = 0
      for (col in adjColors) {
        grid[r][c] = BubbleCell(color = col)
        val cl = findColorCluster(r, c)
        if (cl.size >= 3) {
          for (cellPos in cl) {
            if (seen.add(cellPos.r * GRID_COLS + cellPos.c)) toPop.add(cellPos)
          }
        }
        if (cl.size > bestSize) {
          bestSize = cl.size
          bestColor = col
        }
      }
      if (toPop.size == 1 && bestColor != null) {
        grid[r][c] = BubbleCell(color = bestColor)
        for (cellPos in findColorCluster(r, c)) {
          if (seen.add(cellPos.r * GRID_COLS + cellPos.c)) toPop.add(cellPos)
        }
      }
      if (toPop.size > 1) {
        grid[r][c] = BubbleCell(color = bestColor ?: BubbleColor.RED)
        finishShot(popGroup(toPop))
        return
      }
      val fallback = presentColors().firstOrNull() ?: BubbleColor.RED
      grid[r][c] = BubbleCell(color = fallback)
      callbacks.onStick()
      finishShot(emptyList())
      return
    }

    grid[r][c] = BubbleCell(color = p.color)
    callbacks.onStick()
    val matched = findColorCluster(r, c)
    finishShot(if (matched.size >= 3) popGroup(matched) else emptyList())
  }

  private fun bestEmptyAdjacent(hit: LevelGenerator.GridPos, x: Float, y: Float): LevelGenerator.GridPos? {
    var best: LevelGenerator.GridPos? = null
    var bestDist = Float.MAX_VALUE
    for ((r, c) in LevelGenerator.adjacentCoords(hit.r, hit.c, parity)) {
      if (r < 0 || c < 0 || c >= GRID_COLS || LevelGenerator.cellAt(grid, r, c) != null) continue
      val dx = cellX(r, c) - x
      val dy = cellY(r) - y
      val dSq = dx * dx + dy * dy
      if (dSq < bestDist) {
        bestDist = dSq
        best = LevelGenerator.GridPos(r, c)
      }
    }
    return best
  }

  private fun topEmptyCell(x: Float): LevelGenerator.GridPos? {
    var best: LevelGenerator.GridPos? = null
    var bestDist = Float.MAX_VALUE
    for (c in 0 until GRID_COLS) {
      if (LevelGenerator.cellAt(grid, 0, c) != null) continue
      val dist = abs(cellX(0, c) - x)
      if (dist < bestDist) {
        bestDist = dist
        best = LevelGenerator.GridPos(0, c)
      }
    }
    return best
  }

  private fun nearestValidEmpty(x: Float, y: Float): LevelGenerator.GridPos {
    var best = LevelGenerator.GridPos(0, 0)
    var bestDist = Float.MAX_VALUE
    for (r in 0..grid.size) {
      for (c in 0 until GRID_COLS) {
        if (LevelGenerator.cellAt(grid, r, c) != null) continue
        val supported = r == 0 || LevelGenerator.adjacentCoords(r, c, parity).any { (rr, cc) ->
          LevelGenerator.cellAt(grid, rr, cc) != null
        }
        if (!supported) continue
        val dx = cellX(r, c) - x
        val dy = cellY(r) - y
        val dSq = dx * dx + dy * dy
        if (dSq < bestDist) {
          bestDist = dSq
          best = LevelGenerator.GridPos(r, c)
        }
      }
    }
    return best
  }

  fun findColorCluster(r: Int, c: Int): List<LevelGenerator.GridPos> {
    val target = LevelGenerator.cellAt(grid, r, c) ?: return emptyList()
    if (target.isStone) return emptyList()
    val seen = HashSet<Int>().apply { add(r * GRID_COLS + c) }
    val queue = ArrayDeque<LevelGenerator.GridPos>().apply { addLast(LevelGenerator.GridPos(r, c)) }
    val out = mutableListOf<LevelGenerator.GridPos>()
    while (queue.isNotEmpty()) {
      val (cr, cc) = queue.removeLast()
      out.add(LevelGenerator.GridPos(cr, cc))
      for ((nr, nc) in LevelGenerator.adjacentCoords(cr, cc, parity)) {
        val next = LevelGenerator.cellAt(grid, nr, nc) ?: continue
        if (next.isStone || next.color != target.color) continue
        val key = nr * GRID_COLS + nc
        if (seen.add(key)) queue.addLast(LevelGenerator.GridPos(nr, nc))
      }
    }
    return out
  }

  private fun popGroup(initial: List<LevelGenerator.GridPos>): List<RemovedCellInfo> {
    val seen = HashSet<Int>()
    val all = mutableListOf<LevelGenerator.GridPos>()
    for (pos in initial) {
      if (seen.add(pos.r * GRID_COLS + pos.c)) {
        all.add(pos)
      }
    }
    var i = 0
    while (i < all.size) {
      val (r, c) = all[i]
      val cell = LevelGenerator.cellAt(grid, r, c)
      if (cell != null && cell.hasBomb) {
        triggerBoomFx(cellX(r, c), cellY(r), bubbleRadius * 2.2f)
        for ((rr, cc) in LevelGenerator.adjacentCoords(r, c, parity)) {
          val neighbor = LevelGenerator.cellAt(grid, rr, cc) ?: continue
          val key = rr * GRID_COLS + cc
          if (seen.add(key)) {
            all.add(LevelGenerator.GridPos(rr, cc))
          }
        }
      }
      i++
    }
    all.sortBy { it.r }
    var idx = 0
    val removed = mutableListOf<RemovedCellInfo>()
    for ((r, c) in all) {
      if (LevelGenerator.cellAt(grid, r, c) != null) {
        removed.add(removeCell(r, c, isPop = true, delay = idx * 0.025f))
        idx++
      }
    }
    return removed
  }

  private fun removeCell(r: Int, c: Int, isPop: Boolean, delay: Float): RemovedCellInfo {
    val cell = grid[r][c]!!
    grid[r][c] = null
    val x = cellX(r, c)
    val y = cellY(r)
    if (isPop) {
      pops.add(PopEffect(x = x, y = y, cell = cell, delay = delay))
    } else {
      falls.add(
        FallEffect(
          x = x,
          y = y,
          vx = (Random.nextFloat() - 0.5f) * bubbleRadius * 5f,
          vy = -bubbleRadius * (5f + Random.nextFloat() * 6f),
          cell = cell,
          delay = delay,
        ),
      )
    }
    return RemovedCellInfo(r, c, cell, x, y)
  }

  private fun triggerBoomFx(x: Float, y: Float, rad: Float) {
    beams.add(BeamEffect(x = x, y = y, maxRadius = rad, isRing = true))
    callbacks.onBoom()
    callbacks.onVibrate(60)
  }

  private fun spawnBurst(x: Float, y: Float, color: BubbleColor?, count: Int) {
    for (i in 0 until count) {
      if (particles.size >= 220) break
      val angle = Random.nextFloat() * (2f * PI.toFloat())
      val speed = bubbleRadius * (3f + Random.nextFloat() * 6f)
      particles.add(
        Particle(
          x = x,
          y = y,
          vx = cos(angle) * speed,
          vy = sin(angle) * speed - bubbleRadius * 3f,
          life = 0.5f,
          maxLife = 0.5f,
          color = color,
          radius = bubbleRadius * (0.12f + Random.nextFloat() * 0.14f),
        ),
      )
    }
  }

  private fun finishShot(popped: List<RemovedCellInfo>) {
    val floating = LevelGenerator.findFloatingBubbles(grid, parity)
    val fell = floating.mapIndexed { idx, (r, c) ->
      removeCell(r, c, isPop = false, delay = 0.12f + idx * 0.02f)
    }
    trimEmptyBottomRows()

    val np = popped.size
    val nf = fell.size
    val wasFrozen = freezeShots > 0
    if (wasFrozen) freezeShots--

    if (np + nf > 0) {
      combo++
      if (combo > maxCombo) maxCombo = combo
      val base = np * 10 + max(0, np - 3) * 10 + nf * 25
      val multiplier = 1f + 0.15f * min(combo - 1, 8)
      val gained = (base * multiplier).roundToInt()
      score += gained

      val allRemoved = popped + fell
      removedCount += allRemoved.size
      val obj = levelConfig.objective
      if (obj is LevelObjective.ClearColor) {
        colorClearedCount += allRemoved.count { !it.cell.isStone && it.cell.color == obj.color }
      }

      val meanX = allRemoved.map { it.x }.average().toFloat()
      val meanY = allRemoved.map { it.y }.average().toFloat()
      floatingTexts.add(FloatingText(meanX, meanY, "+$gained", isBig = false))
      if (combo >= 2) {
        floatingTexts.add(FloatingText(width / 2f, meanY - bubbleRadius * 2f, "Combo x$combo!", isBig = true))
        callbacks.onCombo(combo)
      }
      for (i in 0 until min(4, np)) callbacks.onPop(i)
      if (nf > 0) callbacks.onFall()
      callbacks.onVibrate(if (np >= 5) 40L else 15L)
      callbacks.onBubblesScored(allRemoved.size, combo)
    } else {
      combo = 0
      if (levelConfig.dropEvery > 0 && !wasFrozen) {
        missCount++
        if (missCount >= levelConfig.dropEvery) {
          missCount = 0
          addTopRow()
        }
      }
    }

    fixShooterColors()
    evaluateEndConditions()
  }

  private fun addTopRow() {
    val available = presentColors().ifEmpty { levelConfig.palette }
    val newRow = MutableList<BubbleCell?>(GRID_COLS) {
      BubbleCell(color = available[Random.nextInt(available.size)])
    }
    grid.add(0, newRow)
    parity = parity xor 1
    shiftY = -rowHeight
    callbacks.onRowDrop()
    callbacks.onVibrate(30)
    floatingTexts.add(FloatingText(width / 2f, height * 0.3f, "New row!", isBig = true))
  }

  private fun evaluateEndConditions() {
    val nonStoneCount = remainingBubbleCount()
    var hasWon = nonStoneCount == 0
    if (!hasWon) {
      hasWon = when (val obj = levelConfig.objective) {
        LevelObjective.ClearAll -> false
        is LevelObjective.ReachScore -> score >= obj.target
        is LevelObjective.ClearColor -> colorClearedCount >= obj.target
        is LevelObjective.PopCount -> removedCount >= obj.target
      }
    }
    if (hasWon) {
      phase = EnginePhase.ENDED
      won = true
      endReason = EndReason.NONE
      for (i in 0 until 26) {
        spawnBurst(
          width * (0.2f + Random.nextFloat() * 0.6f),
          height * 0.35f,
          BubbleColor.fromIndex(i % 6),
          1,
        )
      }
      callbacks.onGameEnded(won = true, reason = EndReason.NONE, stars = starsNow(), score = score)
      return
    }
    if (hasReachedDangerLine()) {
      phase = EnginePhase.ENDED
      won = false
      endReason = EndReason.DANGER_LINE
      callbacks.onGameEnded(won = false, reason = EndReason.DANGER_LINE, stars = 0, score = score)
      return
    }
    if (shotsLeft <= 0) {
      phase = EnginePhase.ENDED
      won = false
      endReason = EndReason.OUT_OF_SHOTS
      callbacks.onGameEnded(won = false, reason = EndReason.OUT_OF_SHOTS, stars = 0, score = score)
      return
    }
    phase = EnginePhase.READY
    callbacks.onSessionChanged(exportSession())
  }

  private fun hasReachedDangerLine(): Boolean {
    for (r in dangerRow until grid.size) {
      for (c in 0 until GRID_COLS) {
        if (grid[r][c] != null) return true
      }
    }
    return false
  }

  private fun trimEmptyBottomRows() {
    while (grid.isNotEmpty() && grid.last().all { it == null }) {
      grid.removeAt(grid.lastIndex)
    }
  }

  fun presentColors(): List<BubbleColor> {
    val set = linkedSetOf<BubbleColor>()
    for (row in grid) {
      for (cell in row) {
        if (cell != null && !cell.isStone) set.add(cell.color)
      }
    }
    return set.toList()
  }

  private fun pickRandomAvailableColor(): BubbleColor {
    val list = presentColors().ifEmpty { levelConfig.palette }
    return list[Random.nextInt(list.size)]
  }

  private fun fixShooterColors() {
    val list = presentColors()
    if (list.isEmpty()) return
    if (currentColor !in list) currentColor = list[Random.nextInt(list.size)]
    if (nextColor !in list) nextColor = list[Random.nextInt(list.size)]
  }

  fun exportSession(todayKey: String = ""): SavedSession = SavedSession(
    levelNumber = levelConfig.levelNumber,
    isDaily = levelConfig.isDaily,
    dateKey = todayKey,
    grid = grid.map { it.toList() },
    parity = parity,
    shotsLeft = shotsLeft,
    shotsMax = shotsMax,
    score = score,
    removedCount = removedCount,
    colorClearedCount = colorClearedCount,
    combo = combo,
    maxCombo = maxCombo,
    missCount = missCount,
    freezeShots = freezeShots,
    currentColor = currentColor,
    nextColor = nextColor,
    usedBooster = usedBooster,
  )
}
