package com.example.data

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

object LevelGenerator {

  /**
   * Deterministic Mulberry32 PRNG matching the original prototype so all 100 levels
   * and daily challenges have reproducible, balanced layouts.
   */
  class Mulberry32(seedInit: Int) {
    private var state: Int = seedInit

    fun nextDouble(): Double {
      state += 0x6D2B79F5
      var t = state
      t = (t xor (t ushr 15)) * (t or 1)
      t = t xor (t + ((t xor (t ushr 7)) * (t or 61)))
      val unsigned = (t xor (t ushr 14)).toLong() and 0xFFFFFFFFL
      return unsigned.toDouble() / 4294967296.0
    }

    fun nextInt(bound: Int): Int {
      if (bound <= 0) return 0
      return floor(nextDouble() * bound).toInt().coerceIn(0, bound - 1)
    }
  }

  data class GridPos(val r: Int, val c: Int)

  fun adjacentCoords(r: Int, c: Int, parity: Int): List<GridPos> {
    return if (((r + parity) and 1) == 1) {
      listOf(
        GridPos(r, c - 1),
        GridPos(r, c + 1),
        GridPos(r - 1, c),
        GridPos(r - 1, c + 1),
        GridPos(r + 1, c),
        GridPos(r + 1, c + 1),
      )
    } else {
      listOf(
        GridPos(r, c - 1),
        GridPos(r, c + 1),
        GridPos(r - 1, c - 1),
        GridPos(r - 1, c),
        GridPos(r + 1, c - 1),
        GridPos(r + 1, c),
      )
    }
  }

  fun cellAt(grid: List<List<BubbleCell?>>, r: Int, c: Int): BubbleCell? {
    if (r < 0 || r >= grid.size || c < 0 || c >= GRID_COLS) return null
    return grid[r][c]
  }

  fun findFloatingBubbles(grid: List<List<BubbleCell?>>, parity: Int): List<GridPos> {
    if (grid.isEmpty()) return emptyList()
    val seen = HashSet<Int>()
    val queue = ArrayDeque<GridPos>()
    for (c in 0 until GRID_COLS) {
      if (grid[0][c] != null) {
        seen.add(c)
        queue.addLast(GridPos(0, c))
      }
    }
    while (queue.isNotEmpty()) {
      val (r, c) = queue.removeLast()
      for ((rr, cc) in adjacentCoords(r, c, parity)) {
        if (cellAt(grid, rr, cc) == null) continue
        val key = rr * GRID_COLS + cc
        if (seen.add(key)) {
          queue.addLast(GridPos(rr, cc))
        }
      }
    }
    val floating = mutableListOf<GridPos>()
    for (r in grid.indices) {
      for (c in 0 until GRID_COLS) {
        if (grid[r][c] != null && !seen.contains(r * GRID_COLS + c)) {
          floating.add(GridPos(r, c))
        }
      }
    }
    return floating
  }

  fun generateLevel(
    levelNumber: Int,
    seedOverride: Int? = null,
    rowsOverride: Int? = null,
    maxRows: Int? = null,
    ncolOverride: Int? = null,
    patternOverride: String? = null,
    plain: Boolean = false,
    dropEveryOverride: Int? = null,
    kindOverride: String? = null,
    noBooster: Boolean = false,
  ): LevelConfig {
    val n = levelNumber.coerceIn(0, MAX_LEVELS)
    val rng = Mulberry32(seedOverride ?: (n * 7919 + 101))
    val ncol = ncolOverride ?: min(6, 4 + max(0, (n - 1) / 8))
    var rows = rowsOverride ?: min(10, 4 + max(0, (n - 1) / 10) + (if (n % 7 == 0) 1 else 0))
    if (maxRows != null) rows = min(rows, maxRows)
    rows = max(3, rows)

    val idx = mutableListOf(0, 1, 2, 3, 4, 5)
    for (i in 5 downTo 1) {
      val j = rng.nextInt(i + 1)
      val tmp = idx[i]
      idx[i] = idx[j]
      idx[j] = tmp
    }
    val palette = idx.take(ncol).map { BubbleColor.fromIndex(it) }
    val patterns = listOf("clusters", "pyramid", "stripes", "diamond", "checker", "waves", "columns", "ring")
    val pattern = patternOverride ?: if (n <= 2) "clusters" else patterns[(n * 3 + n / 8) % patterns.size]
    val mid = (GRID_COLS - 1) / 2.0

    fun hasCell(r: Int, c: Int): Boolean = when (pattern) {
      "pyramid" -> c >= r / 2 && c < GRID_COLS - r / 2
      "diamond" -> abs(c - mid) <= min(r, rows - 1 - r) + 2.5
      "checker" -> r < 2 || (r + c) % 2 == 0
      "waves" -> r < rows - 2 + (2.0 * sin(c * 0.8 + n)).roundToInt()
      "columns" -> r < 2 || c % 3 != 2
      "ring" -> !(r in 2..(rows - 2) && c in 3..6)
      else -> true
    }

    val grid = Array(rows) { arrayOfNulls<BubbleCell>(GRID_COLS) }
    for (r in 0 until rows) {
      for (c in 0 until GRID_COLS) {
        if (!hasCell(r, c)) continue
        val col: BubbleColor = if (pattern == "stripes") {
          var stripeCol = palette[(r + n / 3) % ncol]
          if (rng.nextDouble() < 0.12) stripeCol = palette[rng.nextInt(ncol)]
          stripeCol
        } else {
          val left = if (c > 0) grid[r][c - 1] else null
          val up = if (r > 0) grid[r - 1][c] else null
          val p = rng.nextDouble()
          when {
            left != null && p < 0.30 -> left.color
            up != null && p < 0.48 -> up.color
            else -> palette[rng.nextInt(ncol)]
          }
        }
        grid[r][c] = BubbleCell(color = col, isStone = false, hasBomb = false)
      }
    }

    var gridList = grid.map { it.toMutableList() }.toMutableList()
    for ((r, c) in findFloatingBubbles(gridList, 0)) {
      gridList[r][c] = null
    }

    fun candidates(): List<GridPos> {
      val list = mutableListOf<GridPos>()
      for (r in 1 until rows) {
        for (c in 0 until GRID_COLS) {
          val cell = gridList[r][c]
          if (cell != null && !cell.isStone && !cell.hasBomb) {
            list.add(GridPos(r, c))
          }
        }
      }
      return list
    }

    if (!plain && n >= 5) {
      var k = min(4, 1 + n / 25)
      while (k-- > 0) {
        val list = candidates()
        if (list.isEmpty()) break
        val (r, c) = list[rng.nextInt(list.size)]
        val existing = gridList[r][c] ?: continue
        gridList[r][c] = existing.copy(hasBomb = true)
      }
    }

    if (!plain && n >= 8) {
      var k = min(10, n / 9)
      while (k-- > 0) {
        val list = candidates()
        if (list.isEmpty()) break
        val (r, c) = list[rng.nextInt(list.size)]
        gridList[r][c] = BubbleCell(color = BubbleColor.RED, isStone = true, hasBomb = false)
      }
    }

    for ((r, c) in findFloatingBubbles(gridList, 0)) {
      gridList[r][c] = null
    }

    var cnt = 0
    val colorCounts = IntArray(6)
    for (row in gridList) {
      for (cell in row) {
        if (cell != null && !cell.isStone) {
          cnt++
          colorCounts[cell.color.index]++
        }
      }
    }

    val kinds = listOf("clear", "score", "color", "clear", "score", "clear")
    var kind = kindOverride ?: if (n <= 2) "clear" else kinds[n % 6]
    var topIdx = 0
    for (i in 1 until 6) {
      if (colorCounts[i] > colorCounts[topIdx]) topIdx = i
    }
    if (kind == "color" && colorCounts[topIdx] < 8) kind = "clear"

    var shots = ceil(cnt / (1.55 + 0.6 * min(n, 100) / 100.0)).toInt() + 6 + (if (n <= 10) 4 else 0)
    val objective: LevelObjective = when (kind) {
      "score" -> {
        val target = max(100, ((cnt * 9.5 / 50.0).roundToInt() * 50))
        shots = ceil(target / 32.0).toInt() + 7
        LevelObjective.ReachScore(target)
      }
      "color" -> LevelObjective.ClearColor(
        color = BubbleColor.fromIndex(topIdx),
        target = max(6, ceil(colorCounts[topIdx] * 0.75).toInt()),
      )
      else -> LevelObjective.ClearAll
    }

    val dropEvery = dropEveryOverride ?: if (n < 5) 0 else max(4, 9 - n / 12)
    return LevelConfig(
      levelNumber = n,
      grid = gridList.map { it.toList() },
      objective = objective,
      shots = shots,
      palette = palette,
      dropEvery = dropEvery,
      bubbleCount = cnt,
      pattern = pattern,
      topColor = BubbleColor.fromIndex(topIdx),
      noBooster = noBooster,
      isDaily = false,
    )
  }

  fun dailyLevelForDate(dateKey: String): LevelConfig {
    val seed = dateKey.replace("-", "").toIntOrNull() ?: 20260101
    val t = abs(seed) % 4
    val base = generateLevel(
      levelNumber = 0,
      seedOverride = seed,
      rowsOverride = when (t) {
        1, 2 -> 7
        3 -> 4
        else -> 5
      },
      ncolOverride = 4,
      patternOverride = "clusters",
      plain = true,
      dropEveryOverride = 0,
      kindOverride = "clear",
    )
    return when (t) {
      0 -> base.copy(
        objective = LevelObjective.PopCount(20),
        shots = 15,
        isDaily = true,
        dailyType = t,
      )
      1 -> base.copy(
        objective = LevelObjective.ReachScore(650),
        shots = 20,
        isDaily = true,
        dailyType = t,
      )
      2 -> base.copy(
        objective = LevelObjective.ClearColor(base.topColor, 10),
        shots = 25,
        isDaily = true,
        dailyType = t,
      )
      else -> base.copy(
        objective = LevelObjective.ClearAll,
        shots = ceil(base.bubbleCount / 1.5).toInt() + 8,
        noBooster = true,
        isDaily = true,
        dailyType = t,
      )
    }
  }

  fun objectiveDescription(level: LevelConfig): String = when (val o = level.objective) {
    is LevelObjective.PopCount -> "Clear ${o.target} bubbles within ${level.shots} shots"
    is LevelObjective.ReachScore -> "Score %,d points within ${level.shots} shots".format(o.target)
    is LevelObjective.ClearColor -> "Clear ${o.target} ${o.color.displayName} bubbles within ${level.shots} shots"
    LevelObjective.ClearAll -> if (level.noBooster) {
      "Clear the board without using boosters"
    } else {
      "Clear all bubbles"
    }
  }
}
