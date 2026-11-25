package com.example.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BoosterType
import com.example.data.GameCatalog
import com.example.data.GameRepository
import com.example.data.LevelGenerator
import com.example.data.MAX_LEVELS
import com.example.data.ModalType
import com.example.ui.GameUiState
import com.example.ui.components.CoinIconCanvas
import com.example.ui.components.GlossyCircleButton
import com.example.ui.components.GlossyPillButton
import com.example.ui.components.GlossyStyle
import com.example.ui.components.TreasureChestCanvas
import com.example.ui.components.drawBoosterBubble
import com.example.ui.theme.BrightViolet
import com.example.ui.theme.CardDarkPurple
import com.example.ui.theme.CardLavenderBorder
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GreenLight
import com.example.ui.theme.NunitoFontFamily
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun ModalOverlay(
  onDismiss: () -> Unit,
  dismissOnBackdrop: Boolean = true,
  content: @Composable ColumnScope.() -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xBD0A0428))
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = { if (dismissOnBackdrop) onDismiss() },
      )
      .padding(14.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 460.dp)
        .fillMaxWidth()
        .heightIn(max = 680.dp)
        .shadow(16.dp, RoundedCornerShape(26.dp))
        .clip(RoundedCornerShape(26.dp))
        .background(Brush.verticalGradient(listOf(BrightViolet, CardDarkPurple)))
        .border(3.dp, CardLavenderBorder, RoundedCornerShape(26.dp))
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = {},
        )
        .verticalScroll(rememberScrollState())
        .padding(14.dp),
      content = content,
    )
  }
}

@Composable
fun ModalHeader(
  title: String,
  onClose: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = title,
      color = Color(0xFFFFE36A),
      fontFamily = FredokaFontFamily,
      fontWeight = FontWeight.ExtraBold,
      fontSize = 24.sp,
    )
    GlossyCircleButton(
      icon = Icons.Rounded.Close,
      contentDescription = "Close",
      onClick = onClose,
      size = 44.dp,
      testTag = "modal_close_button",
    )
  }
}

@Composable
fun DailyRewardModal(
  uiState: GameUiState,
  repository: GameRepository,
  onClaimDaily: () -> Unit,
  onWatchDailyBonusAd: () -> Unit,
  onClose: () -> Unit,
) {
  val progress = uiState.progress
  val today = uiState.todayKey
  val claimed = today.isNotBlank() && progress.dailyLastDate == today
  val nextDay = if (claimed) progress.dailyStreakDay else repository.nextDailyRewardDay(progress, today)
  val bonusDone = today.isNotBlank() && progress.stats.dailyBonusAdDate == today

  ModalOverlay(onDismiss = onClose) {
    ModalHeader(title = "Daily Reward", onClose = onClose)

    // 7-Day grid (4 + 3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      val rows = GameCatalog.DAILY_REWARDS.chunked(4)
      for (row in rows) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          for (item in row) {
            val isGot = if (claimed) item.day <= nextDay else item.day < nextDay
            val isNow = !claimed && item.day == nextDay
            Column(
              modifier = Modifier
                .weight(1f)
                .alpha(if (isGot) 0.58f else 1f)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isNow) Color(0x33FFD93A) else Color(0x40000000))
                .border(
                  width = 2.dp,
                  color = if (isNow) GoldBright else Color(0x22FFFFFF),
                  shape = RoundedCornerShape(14.dp),
                )
                .padding(vertical = 8.dp, horizontal = 4.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text(
                text = "Day ${item.day}",
                color = Color(0xFFFFE36A),
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
              )
              Spacer(Modifier.height(4.dp))
              when {
                item.isChest -> TreasureChestCanvas(Modifier.size(34.dp, 28.dp))
                item.fixedBoosters.isNotEmpty() -> {
                  val b = item.fixedBoosters.keys.first()
                  Canvas(Modifier.size(28.dp)) {
                    drawBoosterBubble(Offset(size.width / 2f, size.height / 2f), size.minDimension / 2f, b)
                  }
                }
                item.randomBoosters > 0 -> {
                  Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = GoldBright, modifier = Modifier.size(28.dp))
                }
                else -> CoinIconCanvas(Modifier.size(28.dp))
              }
              Spacer(Modifier.height(4.dp))
              Text(
                text = item.label,
                color = Color.White,
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (isGot) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = "Claimed", tint = GreenLight, modifier = Modifier.size(16.dp))
              }
            }
          }
          if (row.size < 4) {
            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
          }
        }
      }
    }

    Spacer(Modifier.height(12.dp))
    if (!claimed) {
      GlossyPillButton(
        text = "CLAIM DAY $nextDay",
        style = GlossyStyle.YELLOW,
        onClick = onClaimDaily,
        testTag = "daily_claim_button",
      )
    } else {
      Text(
        text = "Come back tomorrow for Day ${(nextDay % 7) + 1}!",
        color = Color.White,
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
      )
      if (!bonusDone) {
        Spacer(Modifier.height(6.dp))
        GlossyPillButton(
          text = "Watch Ad: +100 Bonus Coins",
          leadingIcon = Icons.Rounded.Movie,
          style = GlossyStyle.GREEN,
          enabled = !uiState.adBusy,
          fontSize = 15.sp,
          onClick = onWatchDailyBonusAd,
          testTag = "daily_bonus_ad_button",
        )
      }
    }
  }
}

@Composable
fun DailyChallengeModal(
  uiState: GameUiState,
  onStartDaily: () -> Unit,
  onClose: () -> Unit,
) {
  val today = uiState.todayKey
  val done = today.isNotBlank() && uiState.progress.challengeDate == today && uiState.progress.challengeCompleted
  val desc = remember(today) {
    if (today.isBlank()) "Clear today's puzzle" else LevelGenerator.objectiveDescription(LevelGenerator.dailyLevelForDate(today))
  }

  ModalOverlay(onDismiss = onClose) {
    ModalHeader(title = "Daily Challenge", onClose = onClose)
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = GoldBright, modifier = Modifier.size(56.dp))
      Spacer(Modifier.height(8.dp))
      Text(
        text = desc,
        color = Color(0xFFFFE36A),
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(6.dp))
      Text(
        text = "Reward: 200 Coins + 1 Free Booster",
        color = Color.White,
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
      )
      if (done) {
        Spacer(Modifier.height(6.dp))
        Text(
          text = "Completed today – practice for fun, rewards are claimed once per day.",
          color = Color(0xFF9FFF7A),
          fontFamily = NunitoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp,
          textAlign = TextAlign.Center,
        )
      }
      Spacer(Modifier.height(14.dp))
      GlossyPillButton(
        text = if (done) "PRACTICE" else "PLAY",
        leadingIcon = Icons.Rounded.PlayArrow,
        style = GlossyStyle.GREEN,
        onClick = onStartDaily,
        testTag = "daily_challenge_play_button",
      )
    }
  }
}

@Composable
fun AchievementsModal(
  uiState: GameUiState,
  repository: GameRepository,
  onClose: () -> Unit,
) {
  val progress = uiState.progress
  ModalOverlay(onDismiss = onClose) {
    ModalHeader(title = "Achievements", onClose = onClose)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      for (ach in GameCatalog.ACHIEVEMENTS) {
        val unlocked = progress.unlockedAchievements.containsKey(ach.id)
        val curVal = repository.achievementProgressValue(ach.id, progress)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .alpha(if (unlocked) 1f else 0.72f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x33000000))
            .padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = if (unlocked) Icons.Rounded.CheckCircle else Icons.Rounded.Star,
            contentDescription = null,
            tint = if (unlocked) GreenLight else GoldBright,
            modifier = Modifier.size(32.dp),
          )
          Spacer(Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = ach.title,
              color = Color.White,
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
            )
            Text(
              text = "${ach.description} · +${ach.rewardCoins} coins",
              color = Color.White.copy(alpha = 0.85f),
              fontFamily = NunitoFontFamily,
              fontSize = 12.sp,
            )
            Spacer(Modifier.height(4.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x66000000)),
            ) {
              val pct = (curVal.toFloat() / ach.target).coerceIn(0f, 1f)
              Box(
                modifier = Modifier
                  .fillMaxWidth(pct)
                  .height(6.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(GreenLight),
              )
            }
          }
          Spacer(Modifier.width(8.dp))
          Text(
            text = if (unlocked) "DONE" else "$curVal/${ach.target}",
            color = if (unlocked) GreenLight else GoldBright,
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
          )
        }
      }
    }
  }
}

@Composable
fun ShopModal(
  uiState: GameUiState,
  onBuyBooster: (BoosterType) -> Unit,
  onWatchAdBooster: () -> Unit,
  onClose: () -> Unit,
) {
  val progress = uiState.progress
  val today = uiState.todayKey
  val usedToday = if (progress.stats.adBoostDate == today) progress.stats.adBoostCount else 0
  val leftToday = (3 - usedToday).coerceAtLeast(0)

  ModalOverlay(onDismiss = onClose) {
    ModalHeader(title = "Shop", onClose = onClose)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 10.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      CoinIconCanvas(Modifier.size(26.dp))
      Spacer(Modifier.width(8.dp))
      Text(
        text = "%,d".format(progress.coins),
        color = GoldBright,
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
      )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      for (booster in BoosterType.entries) {
        val owned = progress.inventory[booster]
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x33000000))
            .padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Canvas(modifier = Modifier.size(38.dp)) {
            drawBoosterBubble(Offset(size.width / 2f, size.height / 2f), size.minDimension / 2f, booster)
          }
          Spacer(Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "${booster.title} (x$owned)",
              color = Color.White,
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
            )
            Text(
              text = booster.description,
              color = Color.White.copy(alpha = 0.85f),
              fontFamily = NunitoFontFamily,
              fontSize = 12.sp,
            )
          }
          Spacer(Modifier.width(8.dp))
          GlossyPillButton(
            text = "${booster.price}",
            style = GlossyStyle.YELLOW,
            fontSize = 14.sp,
            minHeight = 44.dp,
            onClick = { onBuyBooster(booster) },
            testTag = "shop_buy_${booster.key}",
            modifier = Modifier.width(88.dp),
          )
        }
      }
    }

    Spacer(Modifier.height(12.dp))
    GlossyPillButton(
      text = "Watch Ad: Free Booster ($leftToday left)",
      leadingIcon = Icons.Rounded.Movie,
      style = GlossyStyle.GREEN,
      enabled = leftToday > 0 && !uiState.adBusy,
      fontSize = 15.sp,
      onClick = onWatchAdBooster,
      testTag = "shop_watch_ad_booster",
    )
  }
}

@Composable
fun LuckySpinModal(
  uiState: GameUiState,
  onSpinFree: () -> Unit,
  onSpinAd: () -> Unit,
  onClose: () -> Unit,
) {
  val progress = uiState.progress
  val today = uiState.todayKey
  val freeLeft = if (progress.spinDate == today) (1 - progress.spinFreeUsed).coerceAtLeast(0) else 1
  val adLeft = if (progress.spinDate == today) (2 - progress.spinAdUsed).coerceAtLeast(0) else 2

  val animatedRotation by animateFloatAsState(
    targetValue = uiState.wheelTargetRotationDeg,
    animationSpec = tween(durationMillis = 4200, easing = CubicBezierEasing(0.12f, 0.7f, 0.1f, 1f)),
    label = "wheel_spin",
  )

  val wheelPaint = remember {
    Paint().apply {
      isAntiAlias = true
      color = android.graphics.Color.WHITE
      textAlign = Paint.Align.CENTER
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
  }

  ModalOverlay(onDismiss = { if (!uiState.isWheelSpinning) onClose() }) {
    ModalHeader(title = "Lucky Spin", onClose = { if (!uiState.isWheelSpinning) onClose() })

    Box(
      modifier = Modifier
        .size(260.dp)
        .align(Alignment.CenterHorizontally)
        .padding(top = 8.dp),
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.size(240.dp)) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(color = Color(0xFF7A3FE0), radius = radius + 6.dp.toPx(), center = center)
        drawCircle(color = GoldBright, radius = radius + 3.dp.toPx(), center = center)

        rotate(degrees = animatedRotation, pivot = center) {
          val sweep = 45f
          for ((idx, seg) in GameCatalog.SPIN_SEGMENTS.withIndex()) {
            val startAngle = idx * sweep - 90f - sweep / 2f
            drawArc(
              color = seg.sliceColor,
              startAngle = startAngle,
              sweepAngle = sweep,
              useCenter = true,
              topLeft = Offset(center.x - radius, center.y - radius),
              size = Size(radius * 2f, radius * 2f),
            )
            val midAngleDeg = idx * sweep - 90f
            val rad = Math.toRadians(midAngleDeg.toDouble())
            val tx = center.x + (radius * 0.68f * cos(rad)).toFloat()
            val ty = center.y + (radius * 0.68f * sin(rad)).toFloat()
            rotate(degrees = idx * sweep, pivot = Offset(tx, ty)) {
              wheelPaint.textSize = 13.sp.toPx()
              drawContext.canvas.nativeCanvas.drawText(seg.label, tx, ty + 5.dp.toPx(), wheelPaint)
            }
          }
        }
      }

      // Center SPIN Hub
      Box(
        modifier = Modifier
          .size(62.dp)
          .shadow(4.dp, CircleShape)
          .clip(CircleShape)
          .background(Brush.radialGradient(listOf(Color(0xFFFFF4A0), Color(0xFFFFC400))))
          .border(3.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "SPIN",
          color = Color(0xFF7A3A00),
          fontFamily = FredokaFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 14.sp,
        )
      }

      // Top Red Pointer
      Canvas(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .offset(y = (-6).dp)
          .size(28.dp, 28.dp),
      ) {
        val ptr = Path().apply {
          moveTo(0f, 0f)
          lineTo(size.width, 0f)
          lineTo(size.width / 2f, size.height)
          close()
        }
        drawPath(ptr, color = Color(0xFFFF3B3B))
      }
    }

    Spacer(Modifier.height(14.dp))
    if (freeLeft > 0) {
      GlossyPillButton(
        text = "FREE SPIN",
        style = GlossyStyle.YELLOW,
        enabled = !uiState.isWheelSpinning,
        onClick = onSpinFree,
        testTag = "spin_free_button",
      )
    } else {
      GlossyPillButton(
        text = if (adLeft > 0) "Watch Ad for a Spin ($adLeft left)" else "Come back tomorrow",
        leadingIcon = if (adLeft > 0) Icons.Rounded.Movie else null,
        style = GlossyStyle.PURPLE,
        enabled = adLeft > 0 && !uiState.isWheelSpinning && !uiState.adBusy,
        fontSize = 15.sp,
        onClick = onSpinAd,
        testTag = "spin_ad_button",
      )
    }

    Spacer(Modifier.height(6.dp))
    Text(
      text = "1 free spin per day · up to 2 more with rewarded ads",
      color = Color.White.copy(alpha = 0.85f),
      fontFamily = NunitoFontFamily,
      fontSize = 12.sp,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
fun LeaderboardAndStatsModal(
  uiState: GameUiState,
  repository: GameRepository,
  initialTabStats: Boolean,
  onSelectTab: (ModalType) -> Unit,
  onClose: () -> Unit,
) {
  val progress = uiState.progress
  val wkKey = remember(uiState.todayKey) { repository.weekKeyFor(uiState.todayKey.ifBlank { repository.realDateKey() }) }
  val weekScore = if (progress.stats.weekKey == wkKey) progress.stats.weekScore else 0L
  val games = progress.stats.gamesPlayed
  val wins = progress.stats.wins
  val winRate = if (games > 0) (wins * 100f / games).toInt() else 0

  ModalOverlay(onDismiss = onClose) {
    ModalHeader(
      title = if (initialTabStats) "Player Statistics" else "Leaderboard",
      onClose = onClose,
    )

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      TabChip(
        text = "Leaderboard",
        selected = !initialTabStats,
        onClick = { onSelectTab(ModalType.LEADERBOARD) },
        modifier = Modifier.weight(1f),
      )
      TabChip(
        text = "Player Stats",
        selected = initialTabStats,
        onClick = { onSelectTab(ModalType.STATS) },
        modifier = Modifier.weight(1f),
      )
    }

    if (!initialTabStats) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "LOCAL",
          color = Color(0xFF143D08),
          fontFamily = FredokaFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 12.sp,
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(GreenLight)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = "Scores are stored on this device only.",
          color = Color.White.copy(alpha = 0.9f),
          fontFamily = NunitoFontFamily,
          fontSize = 13.sp,
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SummaryItemRow(icon = Icons.Rounded.EmojiEvents, label = "Best Score", value = "%,d".format(progress.highScore))
        SummaryItemRow(icon = Icons.Rounded.Map, label = "Highest Level Reached", value = "${progress.unlockedLevel} / $MAX_LEVELS")
        SummaryItemRow(icon = Icons.Rounded.DateRange, label = "Score This Week", value = "%,d".format(weekScore))
        SummaryItemRow(icon = Icons.Rounded.Star, label = "Total Stars", value = "${progress.totalStars} / ${MAX_LEVELS * 3}")
      }

      Spacer(Modifier.height(10.dp))
      Text(
        text = "Your Top Runs",
        color = Color(0xFFFFE36A),
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
      )
      Spacer(Modifier.height(6.dp))
      if (progress.topScores.isEmpty()) {
        Text(
          text = "Play a level to set your first score.",
          color = Color.White.copy(alpha = 0.85f),
          fontFamily = NunitoFontFamily,
          fontSize = 14.sp,
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          progress.topScores.take(5).forEachIndexed { index, entry ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x33000000))
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "#${index + 1}",
                color = GoldBright,
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                modifier = Modifier.width(36.dp),
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${progress.playerName} (You)",
                  color = Color.White,
                  fontFamily = FredokaFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                )
                Text(
                  text = "${if (entry.levelNumber > 0) "Level ${entry.levelNumber}" else "Daily Challenge"} · ${entry.dateKey}",
                  color = Color.White.copy(alpha = 0.8f),
                  fontFamily = NunitoFontFamily,
                  fontSize = 12.sp,
                )
              }
              Text(
                text = "%,d".format(entry.score),
                color = Color(0xFFFFE36A),
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
              )
            }
          }
        }
      }
    } else {
      val statPairs = listOf(
        "Games Played" to "%,d".format(games),
        "Wins ($winRate%)" to "%,d".format(wins),
        "Bubbles Popped" to "%,d".format(progress.stats.bubblesPopped),
        "Best Combo" to "x${progress.stats.maxCombo}",
        "Levels Done" to "${progress.completedLevelsCount} / $MAX_LEVELS",
        "Boosters Used" to "%,d".format(progress.stats.boostersUsed),
        "Total Score" to "%,d".format(progress.stats.totalScore),
        "Daily Wins" to "%,d".format(progress.stats.dailyWins),
      )
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in statPairs.chunked(2)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((k, v) in row) {
              Column(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(14.dp))
                  .background(Color(0x33000000))
                  .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
                  .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Text(text = k, color = Color.White.copy(alpha = 0.85f), fontFamily = NunitoFontFamily, fontSize = 12.sp)
                Text(text = v, color = Color(0xFFFFE36A), fontFamily = FredokaFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TabChip(
  text: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .defaultMinSize(minHeight = 44.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(
        if (selected) Brush.verticalGradient(listOf(GoldBright, Color(0xFFFF9D00)))
        else Brush.verticalGradient(listOf(Color(0x33000000), Color(0x33000000))),
      )
      .border(2.dp, if (selected) Color(0xFFFFF3A0) else Color(0x33FFFFFF), RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
      .padding(vertical = 8.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      color = if (selected) Color(0xFF4A1A00) else Color.White,
      fontFamily = FredokaFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 14.sp,
    )
  }
}

@Composable
private fun SummaryItemRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  value: String,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(Color(0x33000000))
      .padding(horizontal = 12.dp, vertical = 9.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(icon, contentDescription = null, tint = GoldBright, modifier = Modifier.size(24.dp))
    Spacer(Modifier.width(10.dp))
    Text(text = label, color = Color.White, fontFamily = FredokaFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
    Text(text = value, color = Color(0xFFFFE36A), fontFamily = FredokaFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
  }
}
