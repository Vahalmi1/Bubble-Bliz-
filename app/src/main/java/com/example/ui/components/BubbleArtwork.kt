package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.data.BoosterType
import com.example.data.BubbleCell
import com.example.data.BubbleColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun DrawScope.drawBubbleSphere(
  center: Offset,
  radius: Float,
  color: BubbleColor,
  alpha: Float = 1f,
) {
  if (radius <= 0.5f) return
  val brush = Brush.radialGradient(
    colors = listOf(color.light, color.mid, color.dark),
    center = Offset(center.x - radius * 0.30f, center.y - radius * 0.35f),
    radius = radius * 1.15f,
  )
  drawCircle(brush = brush, radius = radius - 0.5f, center = center, alpha = alpha)
  drawCircle(
    color = Color.White.copy(alpha = 0.42f * alpha),
    radius = radius - 1f,
    center = center,
    style = Stroke(width = (radius * 0.09f).coerceAtLeast(1.2f)),
  )
  drawBubbleGloss(center, radius, alpha)
}

fun DrawScope.drawBubbleGloss(center: Offset, radius: Float, alpha: Float = 1f) {
  // Lower rim bounce reflection
  drawArc(
    color = Color.White.copy(alpha = 0.28f * alpha),
    startAngle = 36f,
    sweepAngle = 108f,
    useCenter = false,
    topLeft = Offset(center.x - radius * 0.78f, center.y - radius * 0.78f),
    size = Size(radius * 1.56f, radius * 1.56f),
    style = Stroke(width = radius * 0.11f, cap = StrokeCap.Round),
  )
  // Top-left glossy highlight oval
  rotate(degrees = -34f, pivot = Offset(center.x - radius * 0.33f, center.y - radius * 0.42f)) {
    drawOval(
      color = Color.White.copy(alpha = 0.82f * alpha),
      topLeft = Offset(center.x - radius * 0.65f, center.y - radius * 0.60f),
      size = Size(radius * 0.64f, radius * 0.36f),
    )
  }
  // Tiny bottom-right specular dot
  drawCircle(
    color = Color.White.copy(alpha = 0.55f * alpha),
    radius = radius * 0.09f,
    center = Offset(center.x + radius * 0.40f, center.y + radius * 0.40f),
  )
}

fun DrawScope.drawStoneBubble(center: Offset, radius: Float, alpha: Float = 1f) {
  val brush = Brush.radialGradient(
    colors = listOf(Color(0xFFC9CCD6), Color(0xFF8A8FA0), Color(0xFF444857)),
    center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
    radius = radius * 1.1f,
  )
  drawCircle(brush = brush, radius = radius - 0.5f, center = center, alpha = alpha)
  drawCircle(
    color = Color(0xFF2A2D38).copy(alpha = alpha),
    radius = radius - 0.8f,
    center = center,
    style = Stroke(width = 1.6f),
  )
  val crackColor = Color(0xFF1E202D).copy(alpha = 0.65f * alpha)
  val crackPath = Path().apply {
    moveTo(center.x - radius * 0.5f, center.y - radius * 0.2f)
    lineTo(center.x - radius * 0.1f, center.y)
    lineTo(center.x - radius * 0.2f, center.y + radius * 0.5f)
    moveTo(center.x - radius * 0.1f, center.y)
    lineTo(center.x + radius * 0.45f, center.y + radius * 0.15f)
  }
  drawPath(crackPath, color = crackColor, style = Stroke(width = 1.6f, cap = StrokeCap.Round))
  rotate(-34f, pivot = Offset(center.x - radius * 0.33f, center.y - radius * 0.42f)) {
    drawOval(
      color = Color.White.copy(alpha = 0.4f * alpha),
      topLeft = Offset(center.x - radius * 0.60f, center.y - radius * 0.56f),
      size = Size(radius * 0.52f, radius * 0.28f),
    )
  }
}

fun DrawScope.drawBombEmblem(center: Offset, radius: Float, scale: Float, alpha: Float = 1f) {
  val r = radius * scale
  val bodyCenter = Offset(center.x, center.y + r * 0.1f)
  val bombRadius = r * 0.60f
  val grad = Brush.radialGradient(
    colors = listOf(Color(0xFF777777), Color(0xFF111111)),
    center = Offset(bodyCenter.x - r * 0.22f, bodyCenter.y - r * 0.28f),
    radius = r * 0.85f,
  )
  drawCircle(brush = grad, radius = bombRadius, center = bodyCenter, alpha = alpha)
  drawCircle(
    color = Color.White.copy(alpha = alpha),
    radius = bombRadius,
    center = bodyCenter,
    style = Stroke(width = 1.6f),
  )
  val fuse = Path().apply {
    moveTo(center.x + r * 0.2f, center.y - r * 0.42f)
    quadraticTo(center.x + r * 0.5f, center.y - r * 0.78f, center.x + r * 0.75f, center.y - r * 0.58f)
  }
  drawPath(fuse, color = Color(0xFFD8B070).copy(alpha = alpha), style = Stroke(width = (r * 0.14f).coerceAtLeast(2f), cap = StrokeCap.Round))
  val sparkCenter = Offset(center.x + r * 0.78f, center.y - r * 0.60f)
  drawCircle(color = Color(0xFFFFB300).copy(alpha = alpha), radius = r * 0.17f, center = sparkCenter)
  drawCircle(color = Color.White.copy(alpha = alpha), radius = r * 0.08f, center = sparkCenter)
  drawCircle(
    color = Color.White.copy(alpha = 0.7f * alpha),
    radius = r * 0.13f,
    center = Offset(center.x - r * 0.20f, center.y - r * 0.12f),
  )
}

fun DrawScope.drawBoosterBubble(center: Offset, radius: Float, booster: BoosterType, alpha: Float = 1f) {
  when (booster) {
    BoosterType.BOMB -> {
      drawBombEmblem(center, radius, scale = 1.0f, alpha = alpha)
      drawCircle(
        color = Color(0xFFFFD93A).copy(alpha = alpha),
        radius = radius - 1f,
        center = center,
        style = Stroke(width = 2f),
      )
    }
    BoosterType.RAINBOW -> {
      for (i in 0 until 6) {
        drawArc(
          color = BubbleColor.fromIndex(i).mid.copy(alpha = alpha),
          startAngle = i * 60f,
          sweepAngle = 60f,
          useCenter = true,
          topLeft = Offset(center.x - radius + 0.5f, center.y - radius + 0.5f),
          size = Size((radius - 0.5f) * 2f, (radius - 0.5f) * 2f),
        )
      }
      val sheen = Brush.radialGradient(
        colors = listOf(Color.White.copy(alpha = 0.55f * alpha), Color.Black.copy(alpha = 0.18f * alpha)),
        center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
        radius = radius,
      )
      drawCircle(brush = sheen, radius = radius - 0.5f, center = center)
      drawCircle(color = Color.White.copy(alpha = alpha), radius = radius - 1f, center = center, style = Stroke(2f))
      drawBubbleGloss(center, radius, alpha)
    }
    BoosterType.LIGHTNING -> {
      drawBubbleSphere(center, radius, BubbleColor.YELLOW, alpha)
      val bolt = Path().apply {
        moveTo(center.x + 0.15f * radius, center.y - 0.72f * radius)
        lineTo(center.x - 0.38f * radius, center.y + 0.06f * radius)
        lineTo(center.x - 0.02f * radius, center.y + 0.06f * radius)
        lineTo(center.x - 0.15f * radius, center.y + 0.72f * radius)
        lineTo(center.x + 0.40f * radius, center.y - 0.12f * radius)
        lineTo(center.x + 0.05f * radius, center.y - 0.12f * radius)
        close()
      }
      drawPath(bolt, color = Color.White.copy(alpha = alpha))
      drawPath(bolt, color = Color(0xFFE06A00).copy(alpha = alpha), style = Stroke(width = 1.8f, join = StrokeJoin.Round))
    }
    BoosterType.FIRE -> {
      drawBubbleSphere(center, radius, BubbleColor.ORANGE, alpha)
      val outerFlame = Path().apply {
        moveTo(center.x, center.y - radius * 0.7f)
        cubicTo(
          center.x + radius * 0.4f, center.y - radius * 0.3f,
          center.x + radius * 0.55f, center.y + radius * 0.1f,
          center.x + radius * 0.28f, center.y + radius * 0.5f,
        )
        cubicTo(
          center.x + radius * 0.15f, center.y + radius * 0.68f,
          center.x - radius * 0.15f, center.y + radius * 0.68f,
          center.x - radius * 0.28f, center.y + radius * 0.5f,
        )
        cubicTo(
          center.x - radius * 0.55f, center.y + radius * 0.1f,
          center.x - radius * 0.2f, center.y - radius * 0.25f,
          center.x, center.y - radius * 0.7f,
        )
      }
      drawPath(outerFlame, color = Color(0xFFFFE36A).copy(alpha = alpha))
      val innerFlame = Path().apply {
        moveTo(center.x, center.y - radius * 0.2f)
        cubicTo(
          center.x + radius * 0.2f, center.y + radius * 0.05f,
          center.x + radius * 0.25f, center.y + radius * 0.3f,
          center.x, center.y + radius * 0.5f,
        )
        cubicTo(
          center.x - radius * 0.25f, center.y + radius * 0.3f,
          center.x - radius * 0.2f, center.y + radius * 0.05f,
          center.x, center.y - radius * 0.2f,
        )
      }
      drawPath(innerFlame, color = Color.White.copy(alpha = alpha))
    }
    BoosterType.FREEZE -> {
      val iceBrush = Brush.radialGradient(
        colors = listOf(Color(0xFFE5FAFF), Color(0xFF4FC3F7), Color(0xFF0277BD)),
        center = Offset(center.x - radius * 0.3f, center.y - radius * 0.35f),
        radius = radius * 1.15f,
      )
      drawCircle(brush = iceBrush, radius = radius - 0.5f, center = center, alpha = alpha)
      for (i in 0 until 3) {
        val angle = (i * 60.0) * PI / 180.0
        val dx = (cos(angle) * radius * 0.58f).toFloat()
        val dy = (sin(angle) * radius * 0.58f).toFloat()
        drawLine(
          color = Color.White.copy(alpha = alpha),
          start = Offset(center.x - dx, center.y - dy),
          end = Offset(center.x + dx, center.y + dy),
          strokeWidth = (radius * 0.13f).coerceAtLeast(1.8f),
          cap = StrokeCap.Round,
        )
      }
      drawBubbleGloss(center, radius, alpha)
    }
  }
}

fun DrawScope.drawBubbleCell(
  center: Offset,
  radius: Float,
  cell: BubbleCell,
  scale: Float = 1f,
  alpha: Float = 1f,
) {
  withTransform({
    scale(scaleX = scale, scaleY = scale, pivot = center)
  }) {
    if (cell.isStone) {
      drawStoneBubble(center, radius, alpha)
    } else {
      drawBubbleSphere(center, radius, cell.color, alpha)
      if (cell.hasBomb) {
        drawBombEmblem(center, radius, scale = 0.62f, alpha = alpha)
      }
    }
  }
}
