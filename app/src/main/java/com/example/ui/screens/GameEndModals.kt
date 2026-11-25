package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EndReason
import com.example.data.GAME_VERSION
import com.example.data.GameSettings
import com.example.data.MAX_LEVELS
import com.example.data.MascotMood
import com.example.data.STARTIO_APP_ID
import com.example.ui.GameOverState
import com.example.ui.GameUiState
import com.example.ui.VictoryState
import com.example.ui.components.BubbleMascotCanvas
import com.example.ui.components.CoinIconCanvas
import com.example.ui.components.GlossyPillButton
import com.example.ui.components.GlossyStyle
import com.example.ui.components.drawBoosterBubble
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GreenLight
import com.example.ui.theme.NunitoFontFamily
import com.example.ui.theme.StarEmpty
import kotlinx.coroutines.delay

@Composable
fun SettingsModal(
  uiState: GameUiState,
  onUpdateSettings: ((GameSettings) -> GameSettings) -> Unit,
  onRequestReset: () -> Unit,
  onClose: () -> Unit,
) {
  val settings = uiState.progress.settings
  ModalOverlay(onDismiss = onClose) {
    ModalHeader(title = "Settings", onClose = onClose)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      SettingSwitchRow(
        label = "Sound Effects",
        checked = settings.sfxEnabled,
        onCheckedChange = { v -> onUpdateSettings { it.copy(sfxEnabled = v) } },
        testTag = "settings_sfx_switch",
      )
      SettingSliderRow(
        label = "SFX Volume",
        value = settings.sfxVolume,
        onValueChange = { v -> onUpdateSettings { it.copy(sfxVolume = v) } },
      )
      SettingSwitchRow(
        label = "Background Music",
        checked = settings.musicEnabled,
        onCheckedChange = { v -> onUpdateSettings { it.copy(musicEnabled = v) } },
        testTag = "settings_music_switch",
      )
      SettingSliderRow(
        label = "Music Volume",
        value = settings.musicVolume,
        onValueChange = { v -> onUpdateSettings { it.copy(musicVolume = v) } },
      )
      SettingSwitchRow(
        label = "Vibration",
        checked = settings.vibrationEnabled,
        onCheckedChange = { v -> onUpdateSettings { it.copy(vibrationEnabled = v) } },
        testTag = "settings_vibration_switch",
      )
    }

    Spacer(Modifier.height(12.dp))
    GlossyPillButton(
      text = "Reset Game Progress",
      style = GlossyStyle.RED,
      fontSize = 15.sp,
      minHeight = 48.dp,
      onClick = onRequestReset,
      testTag = "settings_reset_button",
    )

    HorizontalDivider(color = Color(0x1FFFFFFF), modifier = Modifier.padding(vertical = 10.dp))
    Text(
      text = "About Bitogin Tech",
      color = Color.White,
      fontFamily = FredokaFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 15.sp,
    )
    Text(
      text = "BUBBLE BLITZ is published by Bitogin Tech. Pop. Strategize. Conquer.\nVersion $GAME_VERSION",
      color = Color.White.copy(alpha = 0.88f),
      fontFamily = NunitoFontFamily,
      fontSize = 13.sp,
    )
    HorizontalDivider(color = Color(0x1FFFFFFF), modifier = Modifier.padding(vertical = 10.dp))
    Text(
      text = "Start.io Monetization (App ID: $STARTIO_APP_ID)\n${uiState.adStatusText}",
      color = Color.White.copy(alpha = 0.8f),
      fontFamily = NunitoFontFamily,
      fontSize = 12.sp,
    )
  }
}

@Composable
private fun SettingSwitchRow(
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(Color(0x33000000))
      .padding(horizontal = 12.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      color = Color.White,
      fontFamily = FredokaFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 15.sp,
      modifier = Modifier.weight(1f),
    )
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.testTag(testTag),
      colors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = Color(0xFF38C82E),
        uncheckedThumbColor = Color.White,
        uncheckedTrackColor = Color(0x77000000),
      ),
    )
  }
}

@Composable
private fun SettingSliderRow(
  label: String,
  value: Int,
  onValueChange: (Int) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(Color(0x33000000))
      .padding(horizontal = 12.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      color = Color.White,
      fontFamily = FredokaFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 15.sp,
      modifier = Modifier.weight(1f),
    )
    Slider(
      value = value.toFloat(),
      onValueChange = { onValueChange(it.toInt()) },
      valueRange = 0f..100f,
      colors = SliderDefaults.colors(
        thumbColor = GoldBright,
        activeTrackColor = GoldBright,
        inactiveTrackColor = Color(0x55FFFFFF),
      ),
      modifier = Modifier.width(135.dp),
    )
  }
}

@Composable
fun PauseModal(
  onResume: () -> Unit,
  onRestart: () -> Unit,
  onLevelMap: () -> Unit,
  onHome: () -> Unit,
) {
  ModalOverlay(onDismiss = onResume) {
    ModalHeader(title = "Game Paused", onClose = onResume)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      GlossyPillButton(
        text = "Resume",
        leadingIcon = Icons.Rounded.PlayArrow,
        style = GlossyStyle.GREEN,
        onClick = onResume,
        testTag = "pause_resume_button",
      )
      GlossyPillButton(
        text = "Restart Level",
        leadingIcon = Icons.Rounded.Refresh,
        style = GlossyStyle.BLUE,
        onClick = onRestart,
        testTag = "pause_restart_button",
      )
      GlossyPillButton(
        text = "Level Map",
        leadingIcon = Icons.Rounded.Map,
        style = GlossyStyle.PURPLE,
        onClick = onLevelMap,
        testTag = "pause_map_button",
      )
      GlossyPillButton(
        text = "Home",
        leadingIcon = Icons.Rounded.Home,
        style = GlossyStyle.RED,
        onClick = onHome,
        testTag = "pause_home_button",
      )
    }
  }
}

@Composable
fun VictoryModal(
  victory: VictoryState,
  adBusy: Boolean,
  onNextLevel: () -> Unit,
  onDoubleCoins: () -> Unit,
  onReplay: () -> Unit,
  onHome: () -> Unit,
) {
  val isFinalLevel = !victory.isDaily && victory.levelNumber >= MAX_LEVELS
  var starStage by remember(victory) { mutableStateOf(0) }
  LaunchedEffect(victory) {
    delay(180L)
    starStage = 1
    delay(220L)
    starStage = 2
    delay(220L)
    starStage = 3
  }

  ModalOverlay(onDismiss = onHome, dismissOnBackdrop = false) {
    // 3 Pop-in Stars
    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      for (i in 1..3) {
        val earned = i <= victory.stars && starStage >= i
        val scale by animateFloatAsState(
          targetValue = if (earned) 1f else 0.72f,
          animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
          label = "star_$i",
        )
        Icon(
          imageVector = Icons.Rounded.Star,
          contentDescription = null,
          tint = if (i <= victory.stars) GoldBright else StarEmpty,
          modifier = Modifier
            .size(54.dp)
            .scale(scale),
        )
      }
    }

    // Ribbon Header
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFF8A4DFF), Color(0xFF5320C0))))
        .border(3.dp, GoldBright, RoundedCornerShape(14.dp))
        .padding(vertical = 8.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "Congratulations!",
        color = Color(0xFFFFE36A),
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 25.sp,
      )
    }

    Spacer(Modifier.height(8.dp))
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = if (victory.isDaily) "Daily Challenge Completed!" else "Level ${victory.levelNumber} Completed!",
        color = Color.White,
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
      )
      Text(
        text = "%,d".format(victory.score),
        color = Color(0xFFFFE36A),
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
      )
      Text(
        text = "Your Rewards:",
        color = Color.White.copy(alpha = 0.9f),
        fontFamily = NunitoFontFamily,
        fontSize = 14.sp,
      )
      Spacer(Modifier.height(6.dp))
      Row(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CoinIconCanvas(Modifier.size(32.dp))
          Text(
            text = "+${victory.rewards.coins}",
            color = GoldBright,
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
          )
        }
        for (b in victory.rewards.bonusBoosters) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(32.dp)) {
              drawBoosterBubble(Offset(size.width / 2f, size.height / 2f), size.minDimension / 2f, b)
            }
            Text(
              text = "+1",
              color = GreenLight,
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
            )
          }
        }
      }

      if (isFinalLevel) {
        Spacer(Modifier.height(6.dp))
        Text(
          text = "You conquered Green Valley! More worlds coming soon.",
          color = Color(0xFF9FE8FF),
          fontFamily = NunitoFontFamily,
          fontSize = 13.sp,
          textAlign = TextAlign.Center,
        )
      }

      Spacer(Modifier.height(10.dp))
      GlossyPillButton(
        text = if (victory.isDaily || isFinalLevel) "HOME" else "NEXT LEVEL",
        leadingIcon = if (victory.isDaily || isFinalLevel) Icons.Rounded.Home else Icons.Rounded.PlayArrow,
        style = GlossyStyle.GREEN,
        onClick = onNextLevel,
        testTag = "victory_next_button",
      )
      Spacer(Modifier.height(6.dp))
      GlossyPillButton(
        text = if (victory.coinsDoubled) "Coins Doubled" else "Watch Ad – Double Your Coins",
        leadingIcon = Icons.Rounded.Movie,
        style = GlossyStyle.PURPLE,
        enabled = !victory.coinsDoubled && !adBusy,
        fontSize = 15.sp,
        minHeight = 46.dp,
        onClick = onDoubleCoins,
        testTag = "victory_double_coins_button",
      )
      if (!victory.isDaily) {
        Spacer(Modifier.height(6.dp))
        GlossyPillButton(
          text = "Replay Level",
          leadingIcon = Icons.Rounded.Refresh,
          style = GlossyStyle.BLUE,
          fontSize = 15.sp,
          minHeight = 46.dp,
          onClick = onReplay,
          testTag = "victory_replay_button",
        )
      }
      Spacer(Modifier.height(4.dp))
      BubbleMascotCanvas(mood = MascotMood.WINK, modifier = Modifier.size(86.dp))
    }
  }
}

@Composable
fun GameOverModal(
  gameOver: GameOverState,
  adBusy: Boolean,
  onWatchExtraShots: () -> Unit,
  onTryAgain: () -> Unit,
  onHome: () -> Unit,
) {
  val reasonText = if (gameOver.reason == EndReason.DANGER_LINE) {
    "The bubbles reached the danger line!"
  } else {
    "You ran out of shots!"
  }

  ModalOverlay(onDismiss = onHome, dismissOnBackdrop = false) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFFFF5A5A), Color(0xFFC01A2A))))
        .border(3.dp, Color(0xFFFFB3B3), RoundedCornerShape(14.dp))
        .padding(vertical = 8.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "Game Over!",
        color = Color.White,
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
      )
    }

    Spacer(Modifier.height(8.dp))
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(text = reasonText, color = Color.White, fontFamily = NunitoFontFamily, fontSize = 14.sp, textAlign = TextAlign.Center)
      Spacer(Modifier.height(4.dp))
      Text(text = "Your Score", color = Color.White.copy(alpha = 0.85f), fontFamily = NunitoFontFamily, fontSize = 13.sp)
      Text(
        text = "%,d".format(gameOver.score),
        color = Color(0xFFFFE36A),
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
      )
      Text(
        text = "Best Score: %,d · ${if (gameOver.isDaily) "Daily Challenge" else "Level ${gameOver.levelNumber}"}".format(gameOver.highScore),
        color = Color.White.copy(alpha = 0.9f),
        fontFamily = NunitoFontFamily,
        fontSize = 13.sp,
      )

      Spacer(Modifier.height(10.dp))
      if (gameOver.canWatchExtraShots) {
        GlossyPillButton(
          text = "WATCH AD – GET 5 EXTRA SHOTS",
          leadingIcon = Icons.Rounded.Movie,
          style = GlossyStyle.GREEN,
          enabled = !adBusy,
          fontSize = 14.sp,
          onClick = onWatchExtraShots,
          testTag = "gameover_extra_shots_ad_button",
        )
        Spacer(Modifier.height(6.dp))
      }
      GlossyPillButton(
        text = "TRY AGAIN",
        leadingIcon = Icons.Rounded.Refresh,
        style = GlossyStyle.PURPLE,
        onClick = onTryAgain,
        testTag = "gameover_retry_button",
      )
      Spacer(Modifier.height(6.dp))
      GlossyPillButton(
        text = "HOME",
        leadingIcon = Icons.Rounded.Home,
        style = GlossyStyle.BLUE,
        onClick = onHome,
        testTag = "gameover_home_button",
      )
      Spacer(Modifier.height(4.dp))
      BubbleMascotCanvas(mood = MascotMood.SAD, modifier = Modifier.size(84.dp))
    }
  }
}

@Composable
fun RenamePlayerModal(
  currentName: String,
  onSave: (String) -> Unit,
  onClose: () -> Unit,
) {
  var name by remember(currentName) { mutableStateOf(currentName) }
  ModalOverlay(onDismiss = onClose) {
    ModalHeader(title = "Player Name", onClose = onClose)
    OutlinedTextField(
      value = name,
      onValueChange = { if (it.length <= 18) name = it },
      singleLine = true,
      label = { Text("Display Name") },
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = GoldBright,
        unfocusedBorderColor = Color(0x88FFFFFF),
        focusedLabelColor = GoldBright,
        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
      ),
      modifier = Modifier.fillMaxWidth().testTag("rename_player_input"),
    )
    Spacer(Modifier.height(12.dp))
    GlossyPillButton(
      text = "SAVE",
      style = GlossyStyle.GREEN,
      onClick = { onSave(name) },
      testTag = "rename_player_save_button",
    )
  }
}

@Composable
fun ConfirmResetModal(
  onConfirmReset: () -> Unit,
  onCancel: () -> Unit,
) {
  ModalOverlay(onDismiss = onCancel) {
    ModalHeader(title = "Reset Progress?", onClose = onCancel)
    Text(
      text = "This will erase your unlocked levels, stars, coins, boosters, achievements, and local scores.",
      color = Color.White,
      fontFamily = NunitoFontFamily,
      fontSize = 14.sp,
    )
    Spacer(Modifier.height(12.dp))
    GlossyPillButton(
      text = "Reset Everything",
      style = GlossyStyle.RED,
      onClick = onConfirmReset,
      testTag = "confirm_reset_button",
    )
    Spacer(Modifier.height(6.dp))
    GlossyPillButton(
      text = "Cancel",
      style = GlossyStyle.PURPLE,
      onClick = onCancel,
    )
  }
}
