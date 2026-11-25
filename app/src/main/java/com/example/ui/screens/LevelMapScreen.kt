package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LevelGenerator
import com.example.data.MAX_LEVELS
import com.example.data.PlayerProgress
import com.example.ui.components.GlossyCircleButton
import com.example.ui.components.TreasureChestCanvas
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.GoldBright
import com.example.ui.theme.NunitoFontFamily
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun LevelMapScreen(
  progress: PlayerProgress,
  onBackHome: () -> Unit,
  onSelectLevel: (Int) -> Unit,
  onLockedLevelTap: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val scrollState = rememberScrollState()
  val scope = rememberCoroutineScope()
  val density = LocalDensity.current

  val gapDp = 104.dp
  val totalHeightDp = remember { (MAX_LEVELS * 104 + 360).dp }
  val gapPx = with(density) { gapDp.toPx() }
  val totalHeightPx = with(density) { totalHeightDp.toPx() }
  val bottomAnchorPx = with(density) { 130.dp.toPx() }

  fun nodeY(level: Int): Float = totalHeightPx - bottomAnchorPx - (level - 1) * gapPx

  val currentPulse by rememberInfiniteTransition(label = "node_pulse").animateFloat(
    initialValue = 1.0f,
    targetValue = 1.08f,
    animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
    label = "cur_scale",
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF3A9D3F)),
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val mapWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(360f)
      val viewportHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(600f)

      fun nodeX(level: Int): Float =
        mapWidthPx / 2f + sin((level - 1) * 0.6f) * mapWidthPx * 0.27f

      fun pathXAtY(y: Float): Float {
        val t = (totalHeightPx - bottomAnchorPx - y) / gapPx + 1f
        return mapWidthPx / 2f + sin((t - 1f) * 0.6f) * mapWidthPx * 0.27f
      }

      fun scrollToCurrent(animate: Boolean) {
        val target = (nodeY(progress.unlockedLevel) - viewportHeightPx * 0.55f)
          .roundToInt()
          .coerceIn(0, scrollState.maxValue.coerceAtLeast(0))
        scope.launch {
          if (animate) scrollState.animateScrollTo(target) else scrollState.scrollTo(target)
        }
      }

      LaunchedEffect(progress.unlockedLevel, scrollState.maxValue) {
        val target = (nodeY(progress.unlockedLevel) - viewportHeightPx * 0.55f)
          .roundToInt()
          .coerceIn(0, scrollState.maxValue.coerceAtLeast(0))
        scrollState.scrollTo(target)
      }

      val riverLevels = remember { listOf(17, 37, 57, 77, 95) }
      val trees = remember(mapWidthPx, totalHeightPx) {
        val rng = LevelGenerator.Mulberry32(99)
        val out = mutableListOf<Triple<Float, Float, Float>>()
        repeat(150) {
          val y = (rng.nextDouble() * (totalHeightPx - 80f) + 40f).toFloat()
          val x = (rng.nextDouble() * mapWidthPx).toFloat()
          val nearPath = kotlin.math.abs(x - pathXAtY(y)) < with(density) { 64.dp.toPx() }
          val nearRiver = riverLevels.any { k -> kotlin.math.abs(y - (nodeY(k) - gapPx / 2f)) < with(density) { 40.dp.toPx() } }
          if (!nearPath && !nearRiver) {
            out.add(Triple(x, y, (0.85 + rng.nextDouble() * 0.45).toFloat()))
          }
        }
        out
      }

      Box(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(scrollState),
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(totalHeightDp),
        ) {
          // Green Valley Terrain + Rivers + Winding Path + Bridges + Trees
          Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
              brush = Brush.verticalGradient(
                0.0f to Color(0xFFA9E3FF),
                0.04f to Color(0xFFBFF0A0),
                0.10f to Color(0xFF7FD66B),
                0.40f to Color(0xFF4CBF4C),
                1.0f to Color(0xFF3AA84A),
              ),
            )

            // Rivers
            for (k in riverLevels) {
              val ry = nodeY(k) - gapPx / 2f
              val riverPath = Path().apply {
                moveTo(-20f, ry)
                quadraticTo(size.width * 0.25f, ry - 36f, size.width * 0.5f, ry)
                quadraticTo(size.width * 0.75f, ry + 36f, size.width + 20f, ry)
              }
              drawPath(riverPath, color = Color(0xFF2F8FD8), style = Stroke(width = 50.dp.toPx(), cap = StrokeCap.Round))
              drawPath(riverPath, color = Color(0xFF58C4F5), style = Stroke(width = 38.dp.toPx(), cap = StrokeCap.Round))
              drawPath(
                riverPath,
                color = Color.White.copy(alpha = 0.5f),
                style = Stroke(
                  width = 3.dp.toPx(),
                  pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 22f)),
                ),
              )
            }

            // Winding Sandy Path
            val roadPath = Path().apply {
              moveTo(nodeX(1), nodeY(1) + 80.dp.toPx())
              lineTo(nodeX(1), nodeY(1))
              for (i in 2..MAX_LEVELS) {
                val px = nodeX(i - 1)
                val py = nodeY(i - 1)
                val cx = (px + nodeX(i)) / 2f
                val cy = (py + nodeY(i)) / 2f
                quadraticTo(px, py, cx, cy)
              }
              lineTo(nodeX(MAX_LEVELS), nodeY(MAX_LEVELS))
            }
            drawPath(
              roadPath,
              color = Color(0xFFA86F2E),
              style = Stroke(width = 40.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawPath(
              roadPath,
              color = Color(0xFFECC784),
              style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawPath(
              roadPath,
              color = Color(0xB3FFF3CF),
              style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 18f)),
              ),
            )

            // Wooden Bridges across rivers
            for (k in riverLevels) {
              val by = nodeY(k) - gapPx / 2f
              val bx = pathXAtY(by)
              val angleRad = atan2(cos((k + 0.5f - 1f) * 0.6f) * 0.6f * size.width * 0.27f, gapPx)
              val angleDeg = (angleRad * 180.0 / PI).toFloat()
              val bw = 52.dp.toPx()
              val bh = 72.dp.toPx()
              rotate(angleDeg, pivot = Offset(bx, by)) {
                drawRoundRect(
                  color = Color(0xFFA9743A),
                  topLeft = Offset(bx - bw / 2f, by - bh / 2f),
                  size = Size(bw, bh),
                  cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                )
                drawRoundRect(
                  color = Color(0xFF6E4520),
                  topLeft = Offset(bx - bw / 2f, by - bh / 2f),
                  size = Size(bw, bh),
                  cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                  style = Stroke(width = 3.dp.toPx()),
                )
                for (plank in listOf(-24f, -12f, 0f, 12f, 24f)) {
                  val py = by + plank.dp.toPx()
                  drawLine(
                    color = Color(0xFF6E4520),
                    start = Offset(bx - bw / 2f, py),
                    end = Offset(bx + bw / 2f, py),
                    strokeWidth = 2.dp.toPx(),
                  )
                }
              }
            }

            // Lush cartoon trees
            for ((tx, ty, sc) in trees) {
              val trunkW = 8.dp.toPx() * sc
              val trunkH = 14.dp.toPx() * sc
              drawRect(
                color = Color(0xFF6A3D1A),
                topLeft = Offset(tx - trunkW / 2f, ty - trunkH),
                size = Size(trunkW, trunkH),
              )
              drawCircle(
                color = Color(0xFF288D3E),
                radius = 18.dp.toPx() * sc,
                center = Offset(tx, ty - trunkH - 12.dp.toPx() * sc),
              )
              drawCircle(
                color = Color(0xFF47C55C),
                radius = 14.dp.toPx() * sc,
                center = Offset(tx - 4.dp.toPx() * sc, ty - trunkH - 16.dp.toPx() * sc),
              )
            }
          }

          // Milestone Treasure Chests every 10 levels
          for (m in 10..MAX_LEVELS step 10) {
            val mx = nodeX(m)
            val offsetX = if (mx < mapWidthPx / 2f) with(density) { 64.dp.toPx() } else -with(density) { 64.dp.toPx() }
            val chestHalf = with(density) { 28.dp.toPx() }
            val my = nodeY(m) - chestHalf
            TreasureChestCanvas(
              modifier = Modifier
                .offset { IntOffset((mx + offsetX - chestHalf).roundToInt(), my.roundToInt()) }
                .size(56.dp, 46.dp),
            )
          }

          // All 100 Level Nodes
          for (lvl in 1..MAX_LEVELS) {
            val isUnlocked = lvl <= progress.unlockedLevel
            val isCurrent = lvl == progress.unlockedLevel
            val isMilestone = lvl % 10 == 0
            val stars = progress.stars[lvl] ?: 0
            val nodeSizeDp = if (isCurrent) 70.dp else 56.dp
            val nodeSizePx = with(density) { nodeSizeDp.toPx() }
            val nx = nodeX(lvl)
            val ny = nodeY(lvl)

            Box(
              modifier = Modifier
                .offset { IntOffset((nx - nodeSizePx / 2f).roundToInt(), (ny - nodeSizePx / 2f).roundToInt()) }
                .size(nodeSizeDp),
              contentAlignment = Alignment.Center,
            ) {
              // Star Rating Badge above completed/unlocked nodes
              if (stars > 0) {
                Row(
                  modifier = Modifier.offset(y = (-nodeSizeDp / 2) - 8.dp),
                  horizontalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                  for (s in 1..3) {
                    Icon(
                      imageVector = Icons.Rounded.Star,
                      contentDescription = null,
                      tint = if (s <= stars) GoldBright else Color(0xFF3A2A66),
                      modifier = Modifier.size(16.dp),
                    )
                  }
                }
              }

              val nodeBrush = when {
                isCurrent -> Brush.radialGradient(listOf(Color(0xFFFFF4A0), Color(0xFFFFC400), Color(0xFFE07B00)))
                isUnlocked -> Brush.radialGradient(listOf(Color(0xFF8FD0FF), Color(0xFF2A7CFF), Color(0xFF1646B8)))
                else -> Brush.radialGradient(listOf(Color(0xFFD0D0D8), Color(0xFF8A8A99), Color(0xFF5A5A6A)))
              }
              val ringColor = when {
                isCurrent -> Color.White
                isMilestone && isUnlocked -> Color(0xFFFF9D00)
                isUnlocked -> Color(0xFFFFD54A)
                else -> Color(0xFFAAAAAA)
              }

              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .then(if (isCurrent) Modifier.scale(currentPulse) else Modifier)
                  .testTag("level_node_$lvl")
                  .shadow(if (isCurrent) 12.dp else 6.dp, CircleShape)
                  .clip(CircleShape)
                  .background(nodeBrush)
                  .border(if (isCurrent) 4.dp else 3.5.dp, ringColor, CircleShape)
                  .clickable {
                    if (isUnlocked) onSelectLevel(lvl) else onLockedLevelTap(lvl)
                  },
                contentAlignment = Alignment.Center,
              ) {
                if (isUnlocked) {
                  Text(
                    text = "$lvl",
                    color = Color.White,
                    fontFamily = FredokaFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (isCurrent) 26.sp else 20.sp,
                    style = TextStyle(
                      shadow = Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, 2f), 2f),
                    ),
                  )
                } else {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                      imageVector = Icons.Rounded.Lock,
                      contentDescription = "Locked level $lvl",
                      tint = Color(0xFFE6E6EE),
                      modifier = Modifier.size(18.dp),
                    )
                    Text(
                      text = "$lvl",
                      color = Color(0xFFE6E6EE),
                      fontFamily = FredokaFontFamily,
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp,
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Floating Top Map Header (Back button + Wooden Green Valley Sign)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        GlossyCircleButton(
          icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
          contentDescription = "Back to Home",
          onClick = onBackHome,
          testTag = "map_back_button",
        )
        Spacer(Modifier.width(12.dp))
        Column(
          modifier = Modifier
            .weight(1f)
            .padding(end = 48.dp)
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFC98A45), Color(0xFF8A5222))))
            .border(3.dp, Color(0xFF5D3513), RoundedCornerShape(14.dp))
            .padding(vertical = 6.dp, horizontal = 12.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = "World 1",
            color = Color(0xFFFFE9B8),
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
          )
          Text(
            text = "Green Valley",
            color = Color.White,
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
          )
        }
      }

      // Bottom-Left Jump to Current Level Pill
      Row(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(start = 12.dp, bottom = 12.dp)
          .testTag("map_jump_current_button")
          .shadow(6.dp, CircleShape)
          .clip(CircleShape)
          .background(Brush.verticalGradient(listOf(Color(0xFFFF9D00), Color(0xFFD06800))))
          .border(2.dp, Color(0xFFFFF3A0), CircleShape)
          .clickable { scrollToCurrent(animate = true) }
          .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Rounded.MyLocation, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
          text = "Current",
          color = Color.White,
          fontFamily = FredokaFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
        )
      }

      // Bottom-Right Total Stars Pill
      Row(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(end = 12.dp, bottom = 12.dp)
          .shadow(6.dp, CircleShape)
          .clip(CircleShape)
          .background(Brush.verticalGradient(listOf(Color(0xFF6B3FD9), Color(0xFF3D1CA0))))
          .border(2.dp, Color(0x66FFFFFF), CircleShape)
          .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Rounded.Star, contentDescription = null, tint = GoldBright, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(4.dp))
        Text(
          text = "${progress.totalStars}/${MAX_LEVELS * 3}",
          color = GoldBright,
          fontFamily = FredokaFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 17.sp,
        )
      }
    }
  }
}
