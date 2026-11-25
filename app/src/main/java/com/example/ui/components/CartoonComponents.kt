package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LevelGenerator
import com.example.data.MascotMood
import com.example.ui.theme.*
import kotlin.math.min

enum class GlossyStyle(
  val top: Color,
  val bottom: Color,
  val shadow: Color,
  val border: Color,
) {
  GREEN(GreenLight, GreenDark, GreenShadow, Color(0x55C4F8A0)),
  YELLOW(GoldBright, GoldWarm, GoldShadow, Color(0x66FFF3A0)),
  PURPLE(PurpleBtnLight, PurpleBtnDark, PurpleBtnShadow, Color(0x40FFFFFF)),
  RED(RedBtnLight, RedBtnDark, RedBtnShadow, Color(0x40FFFFFF)),
  BLUE(BlueBtnLight, BlueBtnDark, BlueBtnShadow, Color(0x40FFFFFF)),
}

@Composable
fun CartoonSceneBackground(
  isNight: Boolean,
  modifier: Modifier = Modifier,
) {
  val transition = rememberInfiniteTransition(label = "bg_twinkle")
  val twinkle by transition.animateFloat(
    initialValue = 0.55f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse),
    label = "twinkle",
  )

  Canvas(modifier = modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height

    // Sky gradient
    val skyColors = if (isNight) {
      listOf(Color(0xFF150A52), Color(0xFF3A1A85), Color(0xFF6B2FA8))
    } else {
      listOf(Color(0xFF2A2FD8), Color(0xFF4F7DFF), Color(0xFF93C4FF), Color(0xFFD8B4FF))
    }
    drawRect(brush = Brush.verticalGradient(skyColors))

    val rng = LevelGenerator.Mulberry32(if (isNight) 7 else 3)
    if (isNight) {
      for (i in 0 until 70) {
        val sx = (rng.nextDouble() * w).toFloat()
        val sy = (rng.nextDouble() * h * 0.7).toFloat()
        val rad = (rng.nextDouble() * 1.8 + 0.5).toFloat()
        val baseA = (0.3 + rng.nextDouble() * 0.6).toFloat()
        val alpha = if (i % 2 == 0) baseA * twinkle else baseA
        drawCircle(color = Color.White.copy(alpha = alpha.coerceIn(0.15f, 1f)), radius = rad, center = Offset(sx, sy))
      }
    } else {
      for (i in 0 until 6) {
        val cx = (rng.nextDouble() * w).toFloat()
        val cy = (h * (0.06 + rng.nextDouble() * 0.40)).toFloat()
        val sc = (0.8 + rng.nextDouble() * 0.7).toFloat()
        val a = (0.55 + rng.nextDouble() * 0.3).toFloat()
        drawCartoonCloud(cx, cy, sc, a)
      }
    }

    val hy = h * 0.70f
    // Distant hills
    val backHill = Path().apply {
      moveTo(0f, hy)
      cubicTo(w * 0.3f, hy - 70f, w * 0.6f, hy + 20f, w, hy - 50f)
      lineTo(w, h)
      lineTo(0f, h)
      close()
    }
    drawPath(backHill, color = if (isNight) Color(0xFF2A2A78) else Color(0xFF8FE0A0))

    // Left and right background castles
    val castleScale = min(1.35f, w / 340f)
    val cBody = if (isNight) Color(0xFF4A2F94) else Color(0xFFE1D3FF)
    val cRoof = if (isNight) Color(0xFF2A1A66) else Color(0xFF7B3FE4)
    val cWin = if (isNight) GoldBright else Color(0xFF7B3FE4)
    drawCartoonCastle(w * 0.14f, hy + 10f, castleScale, cBody, cRoof, cWin)
    drawCartoonCastle(w * 0.86f, hy - 14f, castleScale * 0.8f, cBody, cRoof, cWin)

    // Foreground meadow hill
    val frontHill = Path().apply {
      moveTo(0f, hy + 50f)
      cubicTo(w * 0.35f, hy + 10f, w * 0.65f, hy + 90f, w, hy + 30f)
      lineTo(w, h)
      lineTo(0f, h)
      close()
    }
    val meadowColors = if (isNight) {
      listOf(Color(0xFF1F5A45), Color(0xFF0F2F2C))
    } else {
      listOf(Color(0xFF5FD16A), Color(0xFF238F3A))
    }
    drawPath(frontHill, brush = Brush.verticalGradient(meadowColors, startY = hy, endY = h))

    // Cartoon trees
    val treeC1 = if (isNight) Color(0xFF1E6A4A) else Color(0xFF46C25A)
    val treeC2 = if (isNight) Color(0xFF134A35) else Color(0xFF2C9A44)
    for (i in 0 until 9) {
      val tx = (rng.nextDouble() * w).toFloat()
      val ty = (hy + 60f + rng.nextDouble() * (h - hy - 60f)).toFloat()
      val sc = (0.8 + rng.nextDouble() * 0.8).toFloat() * (w / 380f).coerceIn(0.8f, 1.4f)
      drawRect(
        color = Color(0xFF6A3D1A),
        topLeft = Offset(tx - 4f * sc, ty - 12f * sc),
        size = Size(8f * sc, 14f * sc),
      )
      drawCircle(color = treeC2, radius = 20f * sc, center = Offset(tx, ty - 26f * sc))
      drawCircle(color = treeC1, radius = 16f * sc, center = Offset(tx - 6f * sc, ty - 30f * sc))
    }

    if (!isNight) {
      val flowerColors = listOf(Color(0xFFFF6FB5), Color(0xFFFFE36A), Color.White, Color(0xFFFF8A3D))
      for (i in 0 until 28) {
        val fx = (rng.nextDouble() * w).toFloat()
        val fy = (hy + 50f + rng.nextDouble() * (h - hy - 50f)).toFloat()
        drawCircle(color = flowerColors[i % 4], radius = 3.2f, center = Offset(fx, fy))
      }
    }
  }
}

private fun DrawScope.drawCartoonCloud(cx: Float, cy: Float, scale: Float, alpha: Float) {
  val puffs = listOf(
    Triple(0f, 0f, 30f),
    Triple(28f, -8f, 26f),
    Triple(-28f, -4f, 22f),
    Triple(54f, 4f, 20f),
    Triple(-52f, 6f, 18f),
  )
  val c = Color.White.copy(alpha = alpha)
  for ((dx, dy, r) in puffs) {
    drawCircle(color = c, radius = r * scale, center = Offset(cx + dx * scale, cy + dy * scale))
  }
}

private fun DrawScope.drawCartoonCastle(
  cx: Float,
  baseY: Float,
  s: Float,
  body: Color,
  roof: Color,
  window: Color,
) {
  fun tower(px: Float, w: Float, h: Float, rh: Float) {
    drawRect(color = body, topLeft = Offset(px - w / 2f, baseY - h), size = Size(w, h))
    val roofPath = Path().apply {
      moveTo(px - w / 2f - 4f * s, baseY - h)
      lineTo(px, baseY - h - rh)
      lineTo(px + w / 2f + 4f * s, baseY - h)
      close()
    }
    drawPath(roofPath, color = roof)
    drawRect(color = window, topLeft = Offset(px - 3f * s, baseY - h + 12f * s), size = Size(6f * s, 10f * s))
  }

  drawRect(color = body, topLeft = Offset(cx - 34f * s, baseY - 60f * s), size = Size(68f * s, 60f * s))
  tower(cx - 34f * s, 22f * s, 100f * s, 40f * s)
  tower(cx + 34f * s, 22f * s, 100f * s, 40f * s)
  tower(cx, 26f * s, 130f * s, 50f * s)
  drawRect(color = window, topLeft = Offset(cx - 16f * s, baseY - 40f * s), size = Size(6f * s, 10f * s))
  drawRect(color = window, topLeft = Offset(cx + 10f * s, baseY - 40f * s), size = Size(6f * s, 10f * s))
}

@Composable
fun GlossyPillButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  style: GlossyStyle = GlossyStyle.GREEN,
  subtitle: String? = null,
  leadingIcon: ImageVector? = null,
  enabled: Boolean = true,
  fontSize: TextUnit = 19.sp,
  minHeight: Dp = 52.dp,
  testTag: String? = null,
) {
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val pressOffset by animateFloatAsState(if (pressed && enabled) 3f else 0f, label = "btn_press")

  val shape = RoundedCornerShape(999.dp)
  val bgColors = if (enabled) {
    listOf(style.top, style.bottom)
  } else {
    listOf(Color(0xFF6B697D), Color(0xFF474659))
  }
  val shadowColor = if (enabled) style.shadow else Color(0xFF282735)

  Box(
    modifier = modifier
      .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
      .defaultMinSize(minHeight = minHeight)
      .padding(bottom = 5.dp),
  ) {
    // 3D bottom bevel shadow
    Box(
      modifier = Modifier
        .matchParentSize()
        .offset(y = 5.dp)
        .clip(shape)
        .background(shadowColor),
    )

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = minHeight)
        .offset(y = pressOffset.dp)
        .clip(shape)
        .background(Brush.verticalGradient(bgColors))
        .border(2.5.dp, if (enabled) style.border else Color(0x33FFFFFF), shape)
        .clickable(
          interactionSource = interaction,
          indication = null,
          enabled = enabled,
          onClick = onClick,
        )
        .padding(horizontal = 20.dp, vertical = 10.dp),
      contentAlignment = Alignment.Center,
    ) {
      // Glossy top specular bar
      Box(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .fillMaxWidth(0.86f)
          .height(16.dp)
          .offset(y = (-6).dp)
          .clip(shape)
          .background(
            Brush.verticalGradient(
              listOf(Color.White.copy(alpha = 0.48f), Color.White.copy(alpha = 0.04f)),
            ),
          ),
      )

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          if (leadingIcon != null) {
            Icon(
              imageVector = leadingIcon,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
          }
          Text(
            text = text,
            color = Color.White,
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge.copy(
              shadow = Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 3f), 2f),
            ),
          )
        }
        if (subtitle != null) {
          Text(
            text = subtitle,
            color = Color.White.copy(alpha = 0.95f),
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
          )
        }
      }
    }
  }
}

@Composable
fun GlossyActionRow(
  title: String,
  subtitle: String,
  icon: ImageVector,
  style: GlossyStyle,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  badgeText: String? = null,
  testTag: String? = null,
) {
  val shape = RoundedCornerShape(22.dp)
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val pressOffset by animateFloatAsState(if (pressed) 3f else 0f, label = "row_press")

  Box(
    modifier = modifier
      .fillMaxWidth()
      .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
      .padding(bottom = 5.dp),
  ) {
    Box(
      modifier = Modifier
        .matchParentSize()
        .offset(y = 5.dp)
        .clip(shape)
        .background(style.shadow),
    )
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 60.dp)
        .offset(y = pressOffset.dp)
        .clip(shape)
        .background(Brush.verticalGradient(listOf(style.top, style.bottom)))
        .border(2.dp, style.border, shape)
        .clickable(
          interactionSource = interaction,
          indication = null,
          onClick = onClick,
        )
        .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
      Box(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .fillMaxWidth(0.92f)
          .height(18.dp)
          .offset(y = (-7).dp)
          .clip(RoundedCornerShape(18.dp))
          .background(
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)),
          ),
      )

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = title,
              color = Color.White,
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
              style = MaterialTheme.typography.titleMedium.copy(
                shadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 2f), 1f),
              ),
            )
            if (badgeText != null) {
              Spacer(Modifier.width(8.dp))
              Text(
                text = badgeText,
                color = Color.White,
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                modifier = Modifier
                  .clip(CircleShape)
                  .background(Color(0xFFFF3B3B))
                  .padding(horizontal = 8.dp, vertical = 2.dp),
              )
            }
          }
          Text(
            text = subtitle,
            color = Color.White.copy(alpha = 0.92f),
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(28.dp),
        )
      }
    }
  }
}

@Composable
fun GlossyCircleButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = 48.dp,
  testTag: String? = null,
) {
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val pressOffset by animateFloatAsState(if (pressed) 2f else 0f, label = "circ_press")

  Box(
    modifier = modifier
      .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
      .size(size)
      .padding(bottom = 3.dp),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier
        .matchParentSize()
        .offset(y = 3.dp)
        .clip(CircleShape)
        .background(Color(0xFF2A0F78)),
    )
    Box(
      modifier = Modifier
        .fillMaxSize()
        .offset(y = pressOffset.dp)
        .clip(CircleShape)
        .background(
          Brush.radialGradient(
            colors = listOf(Color(0xFFB88CFF), Color(0xFF6A2FE0), Color(0xFF3D17A0)),
            center = Offset(30f, 20f),
          ),
        )
        .border(2.dp, Color(0x40FFFFFF), CircleShape)
        .clickable(
          interactionSource = interaction,
          indication = null,
          onClick = onClick,
        ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = Color.White,
        modifier = Modifier.size(size * 0.52f),
      )
    }
  }
}

@Composable
fun BottomMenuCircleButton(
  label: String,
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  testTag: String? = null,
) {
  Column(
    modifier = modifier
      .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
      .defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .padding(4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier = Modifier
        .size(56.dp)
        .shadow(6.dp, CircleShape)
        .clip(CircleShape)
        .background(
          Brush.radialGradient(
            colors = listOf(Color(0xFFB88CFF), Color(0xFF5A25D0), Color(0xFF2F0F88)),
          ),
        )
        .border(2.5.dp, Color(0x40FFFFFF), CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = GoldBright,
        modifier = Modifier.size(28.dp),
      )
    }
    Spacer(Modifier.height(4.dp))
    Text(
      text = label,
      color = Color.White,
      fontFamily = FredokaFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 13.sp,
      textAlign = TextAlign.Center,
      style = MaterialTheme.typography.labelSmall.copy(
        shadow = Shadow(Color.Black.copy(alpha = 0.7f), Offset(0f, 2f), 2f),
      ),
    )
  }
}

@Composable
fun CoinIconCanvas(modifier: Modifier = Modifier.size(24.dp)) {
  Canvas(modifier = modifier) {
    val r = size.minDimension / 2f
    val c = Offset(size.width / 2f, size.height / 2f)
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(Color(0xFFFFF7A8), Color(0xFFFFC400), Color(0xFFB87800)),
        center = Offset(c.x - r * 0.25f, c.y - r * 0.25f),
        radius = r * 1.1f,
      ),
      radius = r - 1f,
      center = c,
    )
    drawCircle(color = Color(0xFFFFF3A0), radius = r - 1.5f, center = c, style = Stroke(2.2f))
    drawCircle(color = Color(0xFFD98B00), radius = r * 0.48f, center = c, style = Stroke(1.6f))
  }
}

@Composable
fun CoinPillButton(
  coins: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .testTag("coin_shop_pill")
      .defaultMinSize(minHeight = 48.dp)
      .clip(CircleShape)
      .background(Brush.verticalGradient(listOf(Color(0xFF6B3FD9), Color(0xFF3D1CA0))))
      .border(2.dp, Color(0x55FFFFFF), CircleShape)
      .clickable(onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    CoinIconCanvas(Modifier.size(24.dp))
    Text(
      text = "%,d".format(coins),
      color = Color.White,
      fontFamily = FredokaFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 15.sp,
    )
    Box(
      modifier = Modifier
        .size(24.dp)
        .clip(CircleShape)
        .background(Color(0xFF38C82E)),
      contentAlignment = Alignment.Center,
    ) {
      Text("+", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
  }
}

@Composable
fun BubbleMascotCanvas(
  mood: MascotMood,
  modifier: Modifier = Modifier.size(120.dp),
) {
  Canvas(modifier = modifier) {
    val s = size.minDimension / 200f
    // Ground shadow
    drawOval(
      color = Color.Black.copy(alpha = 0.2f),
      topLeft = Offset(38f * s, 183f * s),
      size = Size(124f * s, 14f * s),
    )
    // Glossy purple body
    val bodyBrush = Brush.radialGradient(
      colors = listOf(Color(0xFFE7B3FF), Color(0xFFA23DF0), Color(0xFF561AA0)),
      center = Offset(70f * s, 56f * s),
      radius = 145f * s,
    )
    drawCircle(brush = bodyBrush, radius = 86f * s, center = Offset(100f * s, 100f * s))
    drawCircle(
      color = Color(0xFF4A1590),
      radius = 86f * s,
      center = Offset(100f * s, 100f * s),
      style = Stroke(width = 4f * s),
    )
    // Specular highlight
    rotate(-30f, pivot = Offset(62f * s, 48f * s)) {
      drawOval(
        color = Color.White.copy(alpha = 0.45f),
        topLeft = Offset(36f * s, 34f * s),
        size = Size(52f * s, 28f * s),
      )
    }

    fun drawEye(cx: Float) {
      drawOval(
        color = Color.White,
        topLeft = Offset((cx - 17f) * s, 69f * s),
        size = Size(34f * s, 42f * s),
      )
      drawOval(
        color = Color(0xFF3A0D6E),
        topLeft = Offset((cx - 17f) * s, 69f * s),
        size = Size(34f * s, 42f * s),
        style = Stroke(width = 3f * s),
      )
      val py = if (mood == MascotMood.SAD) 98f else 95f
      drawCircle(color = Color(0xFF2A0A50), radius = 11f * s, center = Offset((cx + 2f) * s, py * s))
      drawCircle(color = Color.White, radius = 4.5f * s, center = Offset((cx + 6f) * s, (py - 6f) * s))
    }

    if (mood == MascotMood.WINK) {
      val wink = Path().apply {
        moveTo(55f * s, 92f * s)
        quadraticTo(72f * s, 76f * s, 89f * s, 92f * s)
      }
      drawPath(wink, color = Color(0xFF3A0D6E), style = Stroke(width = 7f * s, cap = StrokeCap.Round))
    } else {
      drawEye(72f)
    }
    drawEye(128f)

    // Rosy cheeks
    drawCircle(color = Color(0xFFFF6FB5).copy(alpha = 0.55f), radius = 11f * s, center = Offset(46f * s, 118f * s))
    drawCircle(color = Color(0xFFFF6FB5).copy(alpha = 0.55f), radius = 11f * s, center = Offset(154f * s, 118f * s))

    // Mouth
    if (mood == MascotMood.SAD) {
      val frown = Path().apply {
        moveTo(76f * s, 148f * s)
        quadraticTo(100f * s, 124f * s, 124f * s, 148f * s)
      }
      drawPath(frown, color = Color(0xFF3A0D6E), style = Stroke(width = 6f * s, cap = StrokeCap.Round))
      val tear = Path().apply {
        moveTo(60f * s, 112f * s)
        quadraticTo(62f * s, 124f * s, 56f * s, 130f * s)
        quadraticTo(50f * s, 124f * s, 52f * s, 114f * s)
        close()
      }
      drawPath(tear, color = Color(0xFF5AC8FF))
    } else {
      val smile = Path().apply {
        moveTo(68f * s, 122f * s)
        quadraticTo(100f * s, 170f * s, 132f * s, 122f * s)
        close()
      }
      drawPath(smile, color = Color(0xFF7A0F3A))
      drawPath(smile, color = Color(0xFF3A0D6E), style = Stroke(width = 4f * s, join = StrokeJoin.Round))
      val tongue = Path().apply {
        moveTo(84f * s, 142f * s)
        quadraticTo(100f * s, 132f * s, 116f * s, 142f * s)
        quadraticTo(100f * s, 158f * s, 84f * s, 142f * s)
      }
      drawPath(tongue, color = Color(0xFFFF7AA8))

      // Thumbs up hand
      drawRoundRect(
        color = Color.White,
        topLeft = Offset(144f * s, 132f * s),
        size = Size(36f * s, 34f * s),
        cornerRadius = CornerRadius(14f * s, 14f * s),
      )
      drawRoundRect(
        color = Color(0xFFC9B9E8),
        topLeft = Offset(144f * s, 132f * s),
        size = Size(36f * s, 34f * s),
        cornerRadius = CornerRadius(14f * s, 14f * s),
        style = Stroke(width = 3f * s),
      )
      drawRoundRect(
        color = Color.White,
        topLeft = Offset(152f * s, 106f * s),
        size = Size(15f * s, 36f * s),
        cornerRadius = CornerRadius(7f * s, 7f * s),
      )
      drawRoundRect(
        color = Color(0xFFC9B9E8),
        topLeft = Offset(152f * s, 106f * s),
        size = Size(15f * s, 36f * s),
        cornerRadius = CornerRadius(7f * s, 7f * s),
        style = Stroke(width = 3f * s),
      )
    }
  }
}

@Composable
fun TreasureChestCanvas(modifier: Modifier = Modifier.size(64.dp, 52.dp)) {
  Canvas(modifier = modifier) {
    val sx = size.width / 100f
    val sy = size.height / 80f
    drawOval(color = Color.Black.copy(alpha = 0.25f), topLeft = Offset(10f * sx, 69f * sy), size = Size(80f * sx, 10f * sy))
    drawRoundRect(
      color = Color(0xFFA45A1F),
      topLeft = Offset(10f * sx, 34f * sy),
      size = Size(80f * sx, 38f * sy),
      cornerRadius = CornerRadius(6f * sx, 6f * sy),
    )
    val lid = Path().apply {
      moveTo(10f * sx, 38f * sy)
      quadraticTo(50f * sx, -4f * sy, 90f * sx, 38f * sy)
      close()
    }
    drawPath(lid, color = Color(0xFFC26F2A))
    drawPath(lid, color = Color(0xFF5A2C0A), style = Stroke(width = 2.6f * sx))
    drawRect(color = Color(0xFFFFC400), topLeft = Offset(20f * sx, 34f * sy), size = Size(8f * sx, 38f * sy))
    drawRect(color = Color(0xFFFFC400), topLeft = Offset(72f * sx, 34f * sy), size = Size(8f * sx, 38f * sy))
    drawRect(color = Color(0xFFFFC400), topLeft = Offset(10f * sx, 38f * sy), size = Size(80f * sx, 8f * sy))
    drawRoundRect(
      color = GoldBright,
      topLeft = Offset(40f * sx, 36f * sy),
      size = Size(20f * sx, 22f * sy),
      cornerRadius = CornerRadius(4f * sx, 4f * sy),
    )
    drawCircle(color = Color(0xFF5A2C0A), radius = 4f * sx, center = Offset(50f * sx, 47f * sy))
  }
}

@Composable
fun PlayerAvatarCanvas(modifier: Modifier = Modifier.size(52.dp)) {
  Canvas(modifier = modifier) {
    val s = size.minDimension / 100f
    drawCircle(color = Color(0xFF7BD1FF), radius = 50f * s, center = Offset(50f * s, 50f * s))
    drawCircle(color = Color(0xFFC98A5B), radius = 30f * s, center = Offset(50f * s, 58f * s))
    val hair = Path().apply {
      moveTo(19f * s, 48f * s)
      quadraticTo(18f * s, 12f * s, 50f * s, 12f * s)
      quadraticTo(82f * s, 12f * s, 81f * s, 48f * s)
      quadraticTo(70f * s, 30f * s, 50f * s, 30f * s)
      quadraticTo(30f * s, 30f * s, 19f * s, 48f * s)
      close()
    }
    drawPath(hair, color = Color(0xFF2B1B12))
    drawCircle(color = Color(0xFF222222), radius = 4f * s, center = Offset(40f * s, 58f * s))
    drawCircle(color = Color(0xFF222222), radius = 4f * s, center = Offset(60f * s, 58f * s))
    val smile = Path().apply {
      moveTo(38f * s, 70f * s)
      quadraticTo(50f * s, 82f * s, 62f * s, 70f * s)
    }
    drawPath(smile, color = Color(0xFF7A2A1A), style = Stroke(width = 4f * s, cap = StrokeCap.Round))
  }
}
