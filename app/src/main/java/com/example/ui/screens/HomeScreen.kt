package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameCatalog
import com.example.data.LevelGenerator
import com.example.data.MAX_LEVELS
import com.example.data.ModalType
import com.example.data.Screen
import com.example.ui.GameUiState
import com.example.ui.components.BottomMenuCircleButton
import com.example.ui.components.CoinPillButton
import com.example.ui.components.GlossyActionRow
import com.example.ui.components.GlossyCircleButton
import com.example.ui.components.GlossyPillButton
import com.example.ui.components.GlossyStyle
import com.example.ui.components.PlayerAvatarCanvas
import com.example.ui.components.TreasureChestCanvas
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.GoldBright
import com.example.ui.theme.NunitoFontFamily
import java.util.Locale

@Composable
fun HomeScreen(
  uiState: GameUiState,
  onContinueClick: () -> Unit,
  onNavigate: (Screen) -> Unit,
  onOpenModal: (ModalType) -> Unit,
  modifier: Modifier = Modifier,
) {
  val progress = uiState.progress
  val today = uiState.todayKey
  val dailyClaimed = today.isNotBlank() && progress.dailyLastDate == today
  val challengeDone = today.isNotBlank() && progress.challengeDate == today && progress.challengeCompleted
  val dailyDesc = remember(today) {
    if (today.isBlank()) "Play today's challenge" else LevelGenerator.objectiveDescription(LevelGenerator.dailyLevelForDate(today))
  }

  val pulseTransition = rememberInfiniteTransition(label = "daily_ready_pulse")
  val readyBorderAlpha by pulseTransition.animateFloat(
    initialValue = 0.55f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
    label = "pulse_alpha",
  )

  val countdownText = remember(uiState.secondsToMidnight, dailyClaimed) {
    if (!dailyClaimed) {
      "Tap to claim!"
    } else {
      val s = uiState.secondsToMidnight
      val h = s / 3600
      val m = (s / 60) % 60
      val sec = s % 60
      String.format(Locale.US, "%02d:%02d:%02d", h, m, sec)
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .widthIn(max = 560.dp)
        .align(Alignment.Center)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top Header: Avatar + Player Card + Coin Pill + Settings
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Box(
          modifier = Modifier
            .testTag("home_avatar_button")
            .size(52.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .border(3.dp, GoldBright, CircleShape)
            .clickable { onOpenModal(ModalType.RENAME_PLAYER) },
        ) {
          PlayerAvatarCanvas(Modifier.fillMaxSize())
        }

        Column(
          modifier = Modifier
            .weight(1f)
            .testTag("home_player_stats_card")
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xCC2A1A8F))
            .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(14.dp))
            .clickable { onOpenModal(ModalType.STATS) }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
          Text(
            text = progress.playerName,
            color = Color.White,
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "Level ${progress.currentLevel} · ${progress.completedLevelsCount}/$MAX_LEVELS Done ›",
            color = GoldBright,
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(Modifier.height(4.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(7.dp)
              .clip(RoundedCornerShape(5.dp))
              .background(Color(0x55000000)),
          ) {
            val pct = (progress.completedLevelsCount.toFloat() / MAX_LEVELS).coerceIn(0f, 1f)
            Box(
              modifier = Modifier
                .fillMaxWidth(pct)
                .height(7.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFFFFE36A), Color(0xFFFFA600)))),
            )
          }
        }

        CoinPillButton(
          coins = progress.coins,
          onClick = { onOpenModal(ModalType.SHOP) },
        )

        GlossyCircleButton(
          icon = Icons.Rounded.Settings,
          contentDescription = "Settings",
          onClick = { onOpenModal(ModalType.SETTINGS) },
          testTag = "home_settings_button",
        )
      }

      Spacer(Modifier.height(10.dp))

      // Daily Reward Treasure Box
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("home_daily_reward_card")
          .shadow(8.dp, RoundedCornerShape(22.dp))
          .clip(RoundedCornerShape(22.dp))
          .background(Brush.verticalGradient(listOf(Color(0xFF5A2BB8), Color(0xFF2E1580))))
          .border(
            width = 3.dp,
            color = if (!dailyClaimed) GoldBright.copy(alpha = readyBorderAlpha) else Color(0xFFA67CFF),
            shape = RoundedCornerShape(22.dp),
          )
          .clickable { onOpenModal(ModalType.DAILY_REWARD) }
          .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TreasureChestCanvas(Modifier.size(72.dp, 58.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Daily Reward",
            color = Color(0xFFFFE36A),
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 21.sp,
          )
          Text(
            text = if (dailyClaimed) "Come back tomorrow!" else "Your reward is ready!",
            color = Color.White,
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
          )
          Spacer(Modifier.height(4.dp))
          Text(
            text = countdownText,
            color = if (dailyClaimed) Color(0xFF9FE8FF) else GoldBright,
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0x66000000))
              .padding(horizontal = 10.dp, vertical = 3.dp),
          )
        }
      }

      Spacer(Modifier.height(10.dp))

      // Big Continue / Resume CTA
      GlossyPillButton(
        text = if (progress.hasResumeSession) "RESUME" else "CONTINUE",
        subtitle = "Level ${progress.currentLevel}",
        leadingIcon = Icons.Rounded.PlayArrow,
        style = GlossyStyle.YELLOW,
        fontSize = 26.sp,
        minHeight = 68.dp,
        onClick = onContinueClick,
        testTag = "home_continue_button",
      )

      Spacer(Modifier.height(8.dp))

      // Action Rows
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GlossyActionRow(
          title = "Daily Challenge",
          subtitle = if (challengeDone) "Completed today" else dailyDesc,
          icon = Icons.Rounded.CalendarMonth,
          style = GlossyStyle.PURPLE,
          badgeText = if (challengeDone) null else "NEW",
          onClick = { onOpenModal(ModalType.DAILY_CHALLENGE) },
          testTag = "home_daily_challenge_row",
        )
        GlossyActionRow(
          title = "Levels",
          subtitle = "Explore Green Valley (100 Levels)",
          icon = Icons.Rounded.Map,
          style = GlossyStyle.GREEN,
          onClick = { onNavigate(Screen.LEVEL_MAP) },
          testTag = "home_levels_row",
        )
        GlossyActionRow(
          title = "Achievements",
          subtitle = "${progress.unlockedAchievements.size}/${GameCatalog.ACHIEVEMENTS.size} Unlocked · Earn Coins",
          icon = Icons.Rounded.Star,
          style = GlossyStyle.BLUE,
          onClick = { onOpenModal(ModalType.ACHIEVEMENTS) },
          testTag = "home_achievements_row",
        )
      }

      Spacer(Modifier.height(10.dp))

      // Bottom Circular Actions: Shop, Lucky Spin, Leaderboard
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        BottomMenuCircleButton(
          label = "Shop",
          icon = Icons.Rounded.ShoppingCart,
          onClick = { onOpenModal(ModalType.SHOP) },
          testTag = "home_shop_button",
        )
        BottomMenuCircleButton(
          label = "Lucky Spin",
          icon = Icons.Rounded.Casino,
          onClick = { onOpenModal(ModalType.LUCKY_SPIN) },
          testTag = "home_spin_button",
        )
        BottomMenuCircleButton(
          label = "Leaderboard",
          icon = Icons.Rounded.Leaderboard,
          onClick = { onOpenModal(ModalType.LEADERBOARD) },
          testTag = "home_leaderboard_button",
        )
      }
    }
  }
}
