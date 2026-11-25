package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ModalType
import com.example.data.Screen
import com.example.ui.GameViewModel
import com.example.ui.components.CartoonSceneBackground
import com.example.ui.screens.AchievementsModal
import com.example.ui.screens.ConfirmResetModal
import com.example.ui.screens.DailyChallengeModal
import com.example.ui.screens.DailyRewardModal
import com.example.ui.screens.GameOverModal
import com.example.ui.screens.GameplayScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LeaderboardAndStatsModal
import com.example.ui.screens.LevelMapScreen
import com.example.ui.screens.LuckySpinModal
import com.example.ui.screens.PauseModal
import com.example.ui.screens.RenamePlayerModal
import com.example.ui.screens.SettingsModal
import com.example.ui.screens.ShopModal
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.VictoryModal
import com.example.ui.theme.BubbleBlitzTheme
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.GoldBright
import com.example.ui.theme.ToastBg

class MainActivity : ComponentActivity() {
  private val viewModel: GameViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(AndroidColor.parseColor("#150B45")),
      navigationBarStyle = SystemBarStyle.dark(AndroidColor.parseColor("#150B45")),
    )
    viewModel.adManager.attachActivity(this)

    setContent {
      BubbleBlitzTheme {
        BubbleBlitzApp(viewModel = viewModel)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.adManager.attachActivity(this)
    viewModel.onAppResume()
  }

  override fun onPause() {
    viewModel.onAppPause()
    super.onPause()
  }
}

@Composable
fun BubbleBlitzApp(viewModel: GameViewModel) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  BackHandler(
    enabled = uiState.screen != Screen.SPLASH || uiState.modal != ModalType.NONE,
  ) {
    viewModel.handleBackPressed()
  }

  Box(modifier = Modifier.fillMaxSize()) {
    if (uiState.screen != Screen.LEVEL_MAP) {
      CartoonSceneBackground(isNight = uiState.screen == Screen.GAMEPLAY)
    }

    when (uiState.screen) {
      Screen.SPLASH -> SplashScreen(
        onPlayClick = { viewModel.navigateTo(Screen.HOME) },
      )
      Screen.HOME -> HomeScreen(
        uiState = uiState,
        onContinueClick = { viewModel.continueCurrentLevel() },
        onNavigate = { viewModel.navigateTo(it) },
        onOpenModal = { viewModel.openModal(it) },
      )
      Screen.LEVEL_MAP -> LevelMapScreen(
        progress = uiState.progress,
        onBackHome = { viewModel.navigateTo(Screen.HOME) },
        onSelectLevel = { lvl -> viewModel.startLevel(lvl, isDaily = false, fresh = true) },
        onLockedLevelTap = { lvl -> viewModel.showToast("Complete Level ${lvl - 1} to unlock!") },
      )
      Screen.GAMEPLAY -> GameplayScreen(
        uiState = uiState,
        engine = viewModel.engine,
        onPauseClick = { viewModel.pauseGame() },
        onStepFrame = { dt -> viewModel.stepFrame(dt) },
        onPointerDown = { x, y, cancelY -> viewModel.onPointerDown(x, y, cancelY) },
        onPointerMove = { x, y, cancelY -> viewModel.onPointerMove(x, y, cancelY) },
        onPointerUp = { x, y, cancelY -> viewModel.onPointerUp(x, y, cancelY) },
        onPointerCancel = { viewModel.onPointerCancel() },
        onBoosterTap = { b -> viewModel.onBoosterTapped(b) },
        onSwapTap = { viewModel.swapShooterBubbles() },
      )
    }

    when (uiState.modal) {
      ModalType.NONE -> Unit
      ModalType.DAILY_REWARD -> DailyRewardModal(
        uiState = uiState,
        repository = viewModel.repository,
        onClaimDaily = { viewModel.claimDailyReward() },
        onWatchDailyBonusAd = { viewModel.watchAdForDailyBonus() },
        onClose = { viewModel.closeModal() },
      )
      ModalType.DAILY_CHALLENGE -> DailyChallengeModal(
        uiState = uiState,
        onStartDaily = { viewModel.startLevel(0, isDaily = true, fresh = true) },
        onClose = { viewModel.closeModal() },
      )
      ModalType.ACHIEVEMENTS -> AchievementsModal(
        uiState = uiState,
        repository = viewModel.repository,
        onClose = { viewModel.closeModal() },
      )
      ModalType.SHOP -> ShopModal(
        uiState = uiState,
        onBuyBooster = { b -> viewModel.buyBooster(b) },
        onWatchAdBooster = { viewModel.watchAdForFreeBooster() },
        onClose = { viewModel.closeModal() },
      )
      ModalType.LUCKY_SPIN -> LuckySpinModal(
        uiState = uiState,
        onSpinFree = { viewModel.spinFreeWheel() },
        onSpinAd = { viewModel.watchAdForExtraSpin() },
        onClose = { viewModel.closeModal() },
      )
      ModalType.LEADERBOARD -> LeaderboardAndStatsModal(
        uiState = uiState,
        repository = viewModel.repository,
        initialTabStats = false,
        onSelectTab = { viewModel.openModal(it) },
        onClose = { viewModel.closeModal() },
      )
      ModalType.STATS -> LeaderboardAndStatsModal(
        uiState = uiState,
        repository = viewModel.repository,
        initialTabStats = true,
        onSelectTab = { viewModel.openModal(it) },
        onClose = { viewModel.closeModal() },
      )
      ModalType.SETTINGS -> SettingsModal(
        uiState = uiState,
        onUpdateSettings = { viewModel.updateSettings(it) },
        onRequestReset = { viewModel.openModal(ModalType.CONFIRM_RESET) },
        onClose = { viewModel.closeModal() },
      )
      ModalType.PAUSE -> PauseModal(
        onResume = { viewModel.resumeGame() },
        onRestart = { viewModel.restartCurrentLevel() },
        onLevelMap = { viewModel.navigateTo(Screen.LEVEL_MAP) },
        onHome = { viewModel.goHomeFromGame() },
      )
      ModalType.VICTORY -> uiState.victoryState?.let { victory ->
        VictoryModal(
          victory = victory,
          adBusy = uiState.adBusy,
          onNextLevel = { viewModel.nextLevelFromVictory() },
          onDoubleCoins = { viewModel.watchAdForDoubleCoins() },
          onReplay = { viewModel.restartCurrentLevel() },
          onHome = { viewModel.goHomeFromGame() },
        )
      }
      ModalType.GAME_OVER -> uiState.gameOverState?.let { gameOver ->
        GameOverModal(
          gameOver = gameOver,
          adBusy = uiState.adBusy,
          onWatchExtraShots = { viewModel.watchAdForExtraShots() },
          onTryAgain = { viewModel.restartCurrentLevel() },
          onHome = { viewModel.goHomeFromGame() },
        )
      }
      ModalType.RENAME_PLAYER -> RenamePlayerModal(
        currentName = uiState.progress.playerName,
        onSave = { viewModel.renamePlayer(it) },
        onClose = { viewModel.closeModal() },
      )
      ModalType.CONFIRM_RESET -> ConfirmResetModal(
        onConfirmReset = { viewModel.resetAllProgress() },
        onCancel = { viewModel.openModal(ModalType.SETTINGS) },
      )
    }

    // Floating Toast Banner
    AnimatedVisibility(
      visible = uiState.toastMessage != null,
      enter = fadeIn() + slideInVertically { -it / 2 },
      exit = fadeOut() + slideOutVertically { -it / 2 },
      modifier = Modifier
        .align(Alignment.TopCenter)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(top = 64.dp, start = 20.dp, end = 20.dp),
    ) {
      uiState.toastMessage?.let { msg ->
        Text(
          text = msg,
          color = Color.White,
          fontFamily = FredokaFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier
            .shadow(10.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(ToastBg)
            .border(2.dp, GoldBright, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        )
      }
    }
  }
}
