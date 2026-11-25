package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BubbleColor
import com.example.data.MascotMood
import com.example.ui.components.BubbleMascotCanvas
import com.example.ui.components.GlossyPillButton
import com.example.ui.components.GlossyStyle
import com.example.ui.components.drawBubbleSphere
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.GoldBright
import com.example.ui.theme.NunitoFontFamily
import kotlin.math.sin

private data class FloatingSplashBubble(
  val xFraction: Float,
  val radiusDp: Float,
  val phase: Float,
  val speed: Float,
  val color: BubbleColor,
)

@Composable
fun SplashScreen(
  onPlayClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val transition = rememberInfiniteTransition(label = "splash_anim")
  val bobY by transition.animateFloat(
    initialValue = -6f,
    targetValue = 6f,
    animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "mascot_bob",
  )
  val riseProgress by transition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
    label = "bubble_rise",
  )

  val floatingBubbles = remember {
    listOf(
      FloatingSplashBubble(0.12f, 22f, 0.1f, 1.0f, BubbleColor.RED),
      FloatingSplashBubble(0.28f, 16f, 0.4f, 1.3f, BubbleColor.YELLOW),
      FloatingSplashBubble(0.45f, 25f, 0.7f, 0.9f, BubbleColor.BLUE),
      FloatingSplashBubble(0.68f, 19f, 0.25f, 1.15f, BubbleColor.GREEN),
      FloatingSplashBubble(0.84f, 24f, 0.55f, 1.05f, BubbleColor.PURPLE),
      FloatingSplashBubble(0.92f, 15f, 0.85f, 1.4f, BubbleColor.ORANGE),
    )
  }

  Box(modifier = modifier.fillMaxSize()) {
    // Rising background bubbles
    Canvas(modifier = Modifier.fillMaxSize()) {
      for (fb in floatingBubbles) {
        val t = (riseProgress * fb.speed + fb.phase) % 1f
        val y = size.height + 60f - t * (size.height + 140f)
        val x = fb.xFraction * size.width + sin(t * 9f) * 18f
        drawBubbleSphere(
          center = Offset(x, y),
          radius = fb.radiusDp * density,
          color = fb.color,
          alpha = 0.82f,
        )
      }
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .widthIn(max = 560.dp)
        .align(Alignment.Center)
        .padding(horizontal = 20.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top-left Bitogin Tech Publisher Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Box(
          modifier = Modifier
            .size(46.dp)
            .shadow(4.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF7A52FF), Color(0xFF4326C9))))
            .border(2.5.dp, Color.White, RoundedCornerShape(14.dp)),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "B",
            color = Color.White,
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 26.sp,
          )
        }
        Column {
          Text(
            text = "Bitogin Tech",
            color = Color.White,
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            style = MaterialTheme.typography.titleMedium.copy(
              shadow = Shadow(Color.Black.copy(alpha = 0.4f), Offset(0f, 2f), 2f),
            ),
          )
          Text(
            text = "Play • Enjoy • Win",
            color = Color.White.copy(alpha = 0.9f),
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
          )
        }
      }

      // Tilted Cartoon BUBBLE BLITZ Title + Tagline
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.rotate(-3f),
      ) {
        StrokedCartoonTitle(
          text = "BUBBLE",
          fillBrush = Brush.verticalGradient(listOf(Color(0xFFFFF56A), Color(0xFFFFB300))),
          fontSizeSp = 58f,
        )
        StrokedCartoonTitle(
          text = "BLITZ",
          fillBrush = Brush.verticalGradient(listOf(Color.White, Color(0xFF8FD0FF))),
          fontSizeSp = 68f,
          modifier = Modifier.offset(y = (-8).dp),
        )
        Spacer(Modifier.height(2.dp))
        Box(
          modifier = Modifier
            .shadow(6.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF7B3FE4), Color(0xFF4A1BA8))))
            .border(2.5.dp, GoldBright, RoundedCornerShape(12.dp))
            .padding(horizontal = 18.dp, vertical = 6.dp),
        ) {
          Text(
            text = "Pop. Strategize. Conquer.",
            color = Color(0xFFFFE36A),
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            style = MaterialTheme.typography.titleMedium.copy(
              shadow = Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 2f), 2f),
            ),
          )
        }
      }

      // Bobbing Mascot
      BubbleMascotCanvas(
        mood = MascotMood.HAPPY,
        modifier = Modifier
          .size(165.dp)
          .offset(y = bobY.dp),
      )

      // Glossy PLAY button
      GlossyPillButton(
        text = "PLAY",
        leadingIcon = Icons.Rounded.PlayArrow,
        style = GlossyStyle.GREEN,
        fontSize = 26.sp,
        minHeight = 62.dp,
        onClick = onPlayClick,
        testTag = "splash_play_button",
        modifier = Modifier
          .widthIn(min = 220.dp, max = 280.dp)
          .padding(bottom = 12.dp),
      )
    }
  }
}

@Composable
private fun StrokedCartoonTitle(
  text: String,
  fillBrush: Brush,
  fontSizeSp: Float,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    // Deep shadow + thick purple outline layer
    Text(
      text = text,
      color = Color(0xFF26106E),
      fontFamily = FredokaFontFamily,
      fontWeight = FontWeight.ExtraBold,
      fontSize = fontSizeSp.sp,
      letterSpacing = 1.sp,
      modifier = Modifier.offset(y = 5.dp),
      style = TextStyle(
        drawStyle = Stroke(width = 18f, join = StrokeJoin.Round),
      ),
    )
    Text(
      text = text,
      color = Color(0xFF3C1AA6),
      fontFamily = FredokaFontFamily,
      fontWeight = FontWeight.ExtraBold,
      fontSize = fontSizeSp.sp,
      letterSpacing = 1.sp,
      style = TextStyle(
        drawStyle = Stroke(width = 16f, join = StrokeJoin.Round),
      ),
    )
    // Gradient fill layer
    Text(
      text = text,
      fontFamily = FredokaFontFamily,
      fontWeight = FontWeight.ExtraBold,
      fontSize = fontSizeSp.sp,
      letterSpacing = 1.sp,
      style = TextStyle(brush = fillBrush),
    )
  }
}
