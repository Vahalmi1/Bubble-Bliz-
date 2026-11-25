package com.example.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BoosterType
import com.example.data.BubbleColor
import com.example.data.GRID_COLS
import com.example.data.LevelObjective
import com.example.game.BubbleEngine
import com.example.game.EnginePhase
import com.example.ui.GameUiState
import com.example.ui.components.GlossyCircleButton
import com.example.ui.components.drawBoosterBubble
import com.example.ui.components.drawBubbleCell
import com.example.ui.components.drawBubbleSphere
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.GoldBright
import com.example.ui.theme.HudDarkBg
import com.example.ui.theme.IceBlue
import com.example.ui.theme.NunitoFontFamily
import com.example.ui.theme.StarEmpty
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

@Composable
fun GameplayScreen(
  uiState: GameUiState,
  engine: BubbleEngine?,
  onPauseClick: () -> Unit,
  onStepFrame: (Float) -> Unit,
  onPointerDown: (Float, Float, Float) -> Unit,
  onPointerMove: (Float, Float, Float) -> Unit,
  onPointerUp: (Float, Float, Float) -> Unit,
  onPointerCancel: () -> Unit,
  onBoosterTap: (BoosterType) -> Unit,
  onSwapTap: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val latestPaused by rememberUpdatedState(uiState.isPaused)
  val latestEngine by rememberUpdatedState(engine)

  LaunchedEffect(engine, uiState.isPaused) {
    var lastNanos = 0L
    while (true) {
      withFrameNanos { nanos ->
        if (lastNanos != 0L && !latestPaused && latestEngine != null) {
          val dt = ((nanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.033f)
          onStepFrame(dt)
        }
        lastNanos = nanos
      }
    }
  }

  val hud = uiState.hud
  val cfg = uiState.activeLevelConfig
  val objectiveText = remember(cfg, hud.score, hud.remainingBubbles, hud.colorClearedCount, hud.removedCount) {
    when (val o = cfg?.objective) {
      is LevelObjective.ReachScore -> "Score %,d / %,d".format(hud.score, o.target)
      is LevelObjective.ClearColor -> "Pop ${o.color.displayName}: ${min(hud.colorClearedCount, o.target)} / ${o.target}"
      is LevelObjective.PopCount -> "Pop: ${min(hud.removedCount, o.target)} / ${o.target}"
      LevelObjective.ClearAll, null -> "Clear all (${hud.remainingBubbles} left)"
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .widthIn(max = 580.dp)
        .align(Alignment.Center),
    ) {
      // Top HUD
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        GlossyCircleButton(
          icon = Icons.Rounded.Pause,
          contentDescription = "Pause Game",
          onClick = onPauseClick,
          testTag = "game_pause_button",
        )

        HudCounterBox(label = "Score:", value = "%,d".format(hud.score))

        Row(
          modifier = Modifier.weight(1f),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          for (i in 1..3) {
            Icon(
              imageVector = Icons.Rounded.Star,
              contentDescription = null,
              tint = if (i <= hud.starsNow) GoldBright else StarEmpty,
              modifier = Modifier.size(28.dp),
            )
          }
        }

        HudCounterBox(label = "Shots:", value = "${hud.shotsLeft}")
      }

      // Objective & Drop Warning Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = if (hud.isDaily) "Daily" else "Level ${hud.levelNumber}",
          color = Color.White,
          fontFamily = FredokaFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp,
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF7B3FE4))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        )

        Row(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x66000000))
            .padding(horizontal = 10.dp, vertical = 3.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          val obj = cfg?.objective
          if (obj is LevelObjective.ClearColor) {
            Box(
              modifier = Modifier
                .size(11.dp)
                .clip(CircleShape)
                .background(obj.color.mid)
                .border(1.dp, Color(0x88FFFFFF), CircleShape),
            )
            Spacer(Modifier.width(6.dp))
          }
          Text(
            text = objectiveText,
            color = Color.White,
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        if (hud.freezeShots > 0) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.defaultMinSize(minWidth = 44.dp),
            horizontalArrangement = Arrangement.End,
          ) {
            Icon(Icons.Rounded.AcUnit, contentDescription = "Frozen", tint = IceBlue, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(2.dp))
            Text("${hud.freezeShots}", color = IceBlue, fontFamily = FredokaFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        } else if (hud.dropsIn > 0) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.defaultMinSize(minWidth = 44.dp),
            horizontalArrangement = Arrangement.End,
          ) {
            Icon(Icons.Rounded.ArrowDownward, contentDescription = "Row drop", tint = IceBlue, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(2.dp))
            Text("${hud.dropsIn}", color = IceBlue, fontFamily = FredokaFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        }
      }

      // Main Playfield Canvas
      val textPaintFill = remember {
        Paint().apply {
          isAntiAlias = true
          textAlign = Paint.Align.CENTER
          typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
      }
      val textPaintStroke = remember {
        Paint().apply {
          isAntiAlias = true
          textAlign = Paint.Align.CENTER
          typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
          style = Paint.Style.STROKE
          strokeJoin = Paint.Join.ROUND
        }
      }

      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(horizontal = 4.dp)
          .onSizeChanged { sz ->
            engine?.updateBounds(sz.width.toFloat(), sz.height.toFloat())
          }
          .pointerInput(engine) {
            awaitEachGesture {
              val down = awaitFirstDown(requireUnconsumed = false)
              val eng = engine ?: return@awaitEachGesture
              val cancelY = eng.shooterY - eng.bubbleRadius * 1.2f
              onPointerDown(down.position.x, down.position.y, cancelY)
              while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: break
                if (change.pressed) {
                  change.consume()
                  onPointerMove(change.position.x, change.position.y, cancelY)
                } else {
                  onPointerUp(change.position.x, change.position.y, cancelY)
                  break
                }
              }
              if (eng.isAiming) {
                onPointerCancel()
              }
            }
          },
      ) {
        Canvas(
          modifier = Modifier
            .fillMaxSize()
            .testTag("gameplay_canvas"),
        ) {
          // Reading lastCanvasVersion triggers crisp redraws every animation frame
          @Suppress("UNUSED_VARIABLE")
          val frameTick = uiState.lastCanvasVersion
          val eng = engine ?: return@Canvas
          eng.updateBounds(size.width, size.height)

          val r = eng.bubbleRadius
          val boardTop = 0f
          val boardBottom = eng.dangerY + r * 0.7f

          // Translucent arena container
          drawRoundRect(
            color = if (eng.freezeShots > 0) Color(0x3878C8FF) else Color(0x61140A50),
            topLeft = Offset(2f, boardTop),
            size = Size(size.width - 4f, boardBottom),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
          )
          drawRoundRect(
            color = if (eng.freezeShots > 0) Color(0xB3AAE6FF) else Color(0x24FFFFFF),
            topLeft = Offset(2f, boardTop),
            size = Size(size.width - 4f, boardBottom),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
            style = Stroke(width = 2.dp.toPx()),
          )

          // Clipped Bubble Grid
          clipRect(left = 0f, top = 0f, right = size.width, bottom = boardBottom) {
            for (rowIdx in eng.grid.indices) {
              val row = eng.grid[rowIdx]
              for (colIdx in 0 until GRID_COLS) {
                val cell = row[colIdx] ?: continue
                drawBubbleCell(
                  center = Offset(eng.cellX(rowIdx, colIdx), eng.cellY(rowIdx)),
                  radius = r,
                  cell = cell,
                )
              }
            }
          }

          // Danger Line
          val warn = eng.isDangerWarning()
          val dangerAlpha = if (warn) (0.55f + 0.35f * sin(eng.elapsedTime * 6f)) else 0.65f
          drawLine(
            color = Color(0xFFFF5064).copy(alpha = dangerAlpha),
            start = Offset(8f, eng.dangerY),
            end = Offset(size.width - 8f, eng.dangerY),
            strokeWidth = if (warn) 2.6.dp.toPx() else 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f)),
          )

          // Shimmer Sparkles
          for (s in eng.sparkles) {
            val a = (1f - s.elapsed / 0.5f).coerceIn(0f, 1f)
            val z = r * 0.35f * a
            val starPath = Path().apply {
              moveTo(s.x, s.y - z * 1.6f)
              lineTo(s.x + z * 0.4f, s.y - z * 0.4f)
              lineTo(s.x + z * 1.6f, s.y)
              lineTo(s.x + z * 0.4f, s.y + z * 0.4f)
              lineTo(s.x, s.y + z * 1.6f)
              lineTo(s.x - z * 0.4f, s.y + z * 0.4f)
              lineTo(s.x - z * 1.6f, s.y)
              lineTo(s.x - z * 0.4f, s.y - z * 0.4f)
              close()
            }
            drawPath(starPath, color = Color.White.copy(alpha = a))
          }

          // Falling Detached Bubbles
          for (f in eng.falls) {
            drawBubbleCell(center = Offset(f.x, f.y), radius = r, cell = f.cell)
          }

          // Popping Bubbles
          for (p in eng.pops) {
            if (p.delay > 0f) {
              drawBubbleCell(center = Offset(p.x, p.y), radius = r, cell = p.cell)
            } else {
              val k = (p.elapsed / 0.2f).coerceIn(0f, 1f)
              drawBubbleCell(
                center = Offset(p.x, p.y),
                radius = r,
                cell = p.cell,
                scale = 1f + k * 0.35f,
                alpha = 1f - k,
              )
            }
          }

          // Aim Trajectory Preview
          if (eng.phase == EnginePhase.READY && !latestPaused && !eng.inCancelZone) {
            val pts = eng.computeTrajectory()
            for (i in pts.indices) {
              val pt = pts[i]
              val alpha = if (pt.isTarget) 0.88f else (1f - (i.toFloat() / pts.size) * 0.72f).coerceAtLeast(0.22f)
              if (pt.isTarget) {
                drawCircle(
                  color = Color(0xFFFFE36A).copy(alpha = alpha),
                  radius = r * 0.48f,
                  center = Offset(pt.x, pt.y),
                  style = Stroke(width = 2.5.dp.toPx()),
                )
              } else {
                drawCircle(
                  color = Color.White.copy(alpha = alpha),
                  radius = r * 0.16f,
                  center = Offset(pt.x, pt.y),
                )
              }
            }
          } else if (eng.inCancelZone && eng.isAiming) {
            textPaintFill.color = android.graphics.Color.argb(230, 255, 220, 160)
            textPaintFill.textSize = r * 0.62f
            drawContext.canvas.nativeCanvas.drawText(
              "Release here to cancel",
              eng.shooterX,
              eng.shooterY - r * 2.5f,
              textPaintFill,
            )
          }

          // Cannon Pedestal & Rotating Barrel
          drawOval(
            color = Color(0x8C1E0A5A),
            topLeft = Offset(eng.shooterX - r * 2.6f, eng.shooterY + r * 0.5f),
            size = Size(r * 5.2f, r * 2.0f),
          )
          drawOval(
            brush = Brush.verticalGradient(
              listOf(Color(0xFFA45CFF), Color(0xFF4A1BA8)),
              startY = eng.shooterY + r * 0.4f,
              endY = eng.shooterY + r * 2.4f,
            ),
            topLeft = Offset(eng.shooterX - r * 2.2f, eng.shooterY + r * 0.35f),
            size = Size(r * 4.4f, r * 1.9f),
          )
          drawOval(
            color = GoldBright,
            topLeft = Offset(eng.shooterX - r * 2.2f, eng.shooterY + r * 0.35f),
            size = Size(r * 4.4f, r * 1.9f),
            style = Stroke(width = 2.5.dp.toPx()),
          )

          val aimDeg = ((PI / 2.0 - eng.aimAngleRad) * 180.0 / PI).toFloat()
          rotate(degrees = aimDeg, pivot = Offset(eng.shooterX, eng.shooterY)) {
            drawRoundRect(
              color = Color(0xFF7B3FE4),
              topLeft = Offset(eng.shooterX - r * 0.55f, eng.shooterY - r * 2.1f),
              size = Size(r * 1.1f, r * 2.3f),
              cornerRadius = CornerRadius(r * 0.4f, r * 0.4f),
            )
            drawRoundRect(
              color = GoldBright,
              topLeft = Offset(eng.shooterX - r * 0.55f, eng.shooterY - r * 2.1f),
              size = Size(r * 1.1f, r * 2.3f),
              cornerRadius = CornerRadius(r * 0.4f, r * 0.4f),
              style = Stroke(width = 2.2.dp.toPx()),
            )
          }

          // Armed glow + Loaded Bubble
          val armed = eng.armedBooster
          if (armed != null) {
            drawCircle(
              color = Color(0x59FFE65A),
              radius = r * 1.45f + sin(eng.elapsedTime * 8f) * 3f,
              center = Offset(eng.shooterX, eng.shooterY),
            )
          }
          val loadScale = if (eng.reloadTimer > 0f) 0.75f + 0.25f * (1f - eng.reloadTimer / 0.2f) else 1f
          val shooterCenter = Offset(eng.shooterX, eng.shooterY)
          if (armed != null) {
            drawBoosterBubble(shooterCenter, r * loadScale, armed)
          } else {
            drawBubbleSphere(shooterCenter, r * loadScale, eng.currentColor)
          }

          // Next Bubble Preview + Label
          val nextCenter = Offset(eng.shooterX - r * 3.7f, eng.shooterY + r * 0.9f)
          drawBubbleSphere(nextCenter, r * 0.72f, eng.nextColor)
          textPaintStroke.color = android.graphics.Color.argb(150, 0, 0, 0)
          textPaintStroke.strokeWidth = 4f
          textPaintStroke.textSize = r * 0.52f
          textPaintFill.color = android.graphics.Color.WHITE
          textPaintFill.textSize = r * 0.52f
          drawContext.canvas.nativeCanvas.drawText("NEXT", nextCenter.x, eng.shooterY - r * 0.15f, textPaintStroke)
          drawContext.canvas.nativeCanvas.drawText("NEXT", nextCenter.x, eng.shooterY - r * 0.15f, textPaintFill)

          // Active Projectile
          val proj = eng.projectile
          if (proj != null) {
            val pc = Offset(proj.x, proj.y)
            if (proj.boosterType != null) {
              drawBoosterBubble(pc, r, proj.boosterType)
            } else {
              drawBubbleSphere(pc, r, proj.color)
            }
          }

          // Shockwaves & Lightning Beams
          for (b in eng.beams) {
            val a = (1f - b.elapsed / 0.45f).coerceIn(0f, 1f)
            if (b.isRing) {
              drawCircle(
                color = Color(0xFFFFC83C).copy(alpha = a),
                radius = b.maxRadius * (0.4f + b.elapsed * 2.2f),
                center = Offset(b.x, b.y),
                style = Stroke(width = r * 0.5f * a + 1.5f),
              )
            } else {
              val beamW = r * a + 2f
              drawRect(
                color = Color(0xFFFFF078).copy(alpha = a * 0.85f),
                topLeft = Offset(b.x - beamW / 2f, 0f),
                size = Size(beamW, eng.dangerY + r),
              )
              drawRect(
                color = Color.White.copy(alpha = a),
                topLeft = Offset(b.x - 2.5f, 0f),
                size = Size(5f, eng.dangerY + r),
              )
            }
          }

          // Burst Particles
          for (p in eng.particles) {
            val a = (p.life / p.maxLife).coerceIn(0f, 1f)
            val col = p.color?.mid ?: Color(0xFFAABBCC)
            drawCircle(color = col.copy(alpha = a), radius = p.radius, center = Offset(p.x, p.y))
          }

          // Floating Score & Combo Callouts
          for (ft in eng.floatingTexts) {
            val maxDur = if (ft.isBig) 1.4f else 1.0f
            val a = ((1f - ft.elapsed / maxDur) * 255f).toInt().coerceIn(0, 255)
            val fontSize = r * (if (ft.isBig) 1.45f else 1.05f)
            textPaintStroke.color = android.graphics.Color.argb(a, 58, 26, 128)
            textPaintStroke.strokeWidth = 6f
            textPaintStroke.textSize = fontSize
            textPaintFill.color = if (ft.isBig) {
              android.graphics.Color.argb(a, 255, 227, 106)
            } else {
              android.graphics.Color.argb(a, 255, 255, 255)
            }
            textPaintFill.textSize = fontSize
            drawContext.canvas.nativeCanvas.drawText(ft.text, ft.x, ft.y, textPaintStroke)
            drawContext.canvas.nativeCanvas.drawText(ft.text, ft.x, ft.y, textPaintFill)
          }
        }
      }

      // Bottom Booster Tray + Swap Button
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        for (booster in BoosterType.entries) {
          val count = uiState.progress.inventory[booster]
          val isArmed = hud.armedBooster == booster
          BoosterTrayButton(
            booster = booster,
            count = count,
            isArmed = isArmed,
            onClick = { onBoosterTap(booster) },
          )
        }

        // Swap Current / Next Bubble Button
        Box(
          modifier = Modifier
            .testTag("swap_bubble_button")
            .size(52.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(
              Brush.radialGradient(
                colors = listOf(Color(0xFF5AA6FF), Color(0xFF1B4FD0)),
              ),
            )
            .border(2.5.dp, Color(0xFFB48CFF), CircleShape)
            .clickable(onClick = onSwapTap),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.SwapHoriz,
            contentDescription = "Swap Bubble",
            tint = Color.White,
            modifier = Modifier.size(28.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun HudCounterBox(label: String, value: String) {
  Column(
    modifier = Modifier
      .defaultMinSize(minWidth = 74.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(HudDarkBg)
      .border(2.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
      .padding(horizontal = 10.dp, vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = label,
      color = Color.White.copy(alpha = 0.85f),
      fontFamily = NunitoFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 11.sp,
    )
    Text(
      text = value,
      color = Color.White,
      fontFamily = FredokaFontFamily,
      fontWeight = FontWeight.ExtraBold,
      fontSize = 19.sp,
    )
  }
}

@Composable
private fun BoosterTrayButton(
  booster: BoosterType,
  count: Int,
  isArmed: Boolean,
  onClick: () -> Unit,
) {
  Box(
    modifier = Modifier
      .testTag("booster_${booster.key}")
      .offset(y = if (isArmed) (-6).dp else 0.dp)
      .size(52.dp)
      .alpha(if (count > 0) 1f else 0.55f),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .shadow(if (isArmed) 10.dp else 6.dp, CircleShape)
        .clip(CircleShape)
        .background(
          Brush.radialGradient(
            colors = listOf(Color(0xFF7A52E8), Color(0xFF3A1D9A)),
          ),
        )
        .border(
          width = if (isArmed) 3.5.dp else 2.5.dp,
          color = if (isArmed) GoldBright else Color(0xFFB48CFF),
          shape = CircleShape,
        )
        .clickable(onClick = onClick),
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.size(34.dp)) {
        drawBoosterBubble(
          center = Offset(size.width / 2f, size.height / 2f),
          radius = size.minDimension / 2f,
          booster = booster,
        )
      }
    }

    Box(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .offset(x = 4.dp, y = 4.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(GoldBright)
        .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
      Text(
        text = "x$count",
        color = Color(0xFF4A1A00),
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp,
      )
    }
  }
}
