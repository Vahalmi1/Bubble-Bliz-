package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ads.AdLoadState
import com.example.ads.StartIoAdManager
import com.example.audio.SoundEffect
import com.example.audio.SoundEngine
import com.example.data.AdPlacement
import com.example.data.BoosterType
import com.example.data.DailyClaimResult
import com.example.data.EndReason
import com.example.data.GameCatalog
import com.example.data.GameRepository
import com.example.data.GameSettings
import com.example.data.LevelConfig
import com.example.data.LevelGenerator
import com.example.data.MAX_LEVELS
import com.example.data.ModalType
import com.example.data.PlayerProgress
import com.example.data.Screen
import com.example.data.SpinSegment
import com.example.data.VictoryRewardsSummary
import com.example.data.local.AppDatabase
import com.example.game.BubbleEngine
import com.example.game.EngineAudioCallbacks
import com.example.game.EnginePhase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class HudSnapshot(
  val levelNumber: Int = 1,
  val isDaily: Boolean = false,
  val score: Int = 0,
  val shotsLeft: Int = 0,
  val shotsMax: Int = 1,
  val starsNow: Int = 3,
  val freezeShots: Int = 0,
  val dropsIn: Int = 0,
  val remainingBubbles: Int = 0,
  val removedCount: Int = 0,
  val colorClearedCount: Int = 0,
  val armedBooster: BoosterType? = null,
)

data class VictoryState(
  val levelNumber: Int,
  val isDaily: Boolean,
  val stars: Int,
  val score: Int,
  val rewards: VictoryRewardsSummary,
  val coinsDoubled: Boolean = false,
)

data class GameOverState(
  val levelNumber: Int,
  val isDaily: Boolean,
  val score: Int,
  val highScore: Int,
  val reason: EndReason,
  val canWatchExtraShots: Boolean,
)

data class GameUiState(
  val screen: Screen = Screen.SPLASH,
  val modal: ModalType = ModalType.NONE,
  val progress: PlayerProgress = PlayerProgress(),
  val todayKey: String = "",
  val secondsToMidnight: Long = 0L,
  val isPaused: Boolean = false,
  val hud: HudSnapshot = HudSnapshot(),
  val activeLevelConfig: LevelConfig? = null,
  val victoryState: VictoryState? = null,
  val gameOverState: GameOverState? = null,
  val wheelTargetRotationDeg: Float = 0f,
  val isWheelSpinning: Boolean = false,
  val adBusy: Boolean = false,
  val adStatusText: String = "",
  val toastMessage: String? = null,
  val lastCanvasVersion: Long = 0L,
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

  private val db = AppDatabase.getInstance(application)
  val repository = GameRepository(db.gameDao())
  val soundEngine = SoundEngine(application)
  val adManager = StartIoAdManager(application)

  private val _uiState = MutableStateFlow(GameUiState())
  val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

  var engine: BubbleEngine? = null
    private set

  private var toastSeq = 0L

  init {
    soundEngine.initialize()
    viewModelScope.launch {
      repository.ensureInitialized()
      val today = repository.effectiveTodayKey()
      _uiState.update {
        it.copy(
          todayKey = today,
          secondsToMidnight = repository.secondsToMidnight(),
          adStatusText = adManager.lastStatusMessage,
        )
      }
    }

    viewModelScope.launch {
      repository.progressFlow.collect { progress ->
        soundEngine.updateSettings(progress.settings)
        _uiState.update { it.copy(progress = progress) }
      }
    }

    viewModelScope.launch {
      while (true) {
        delay(1000L)
        val today = repository.realDateKey()
        _uiState.update {
          it.copy(
            todayKey = if (it.todayKey.isBlank() || today > it.todayKey) today else it.todayKey,
            secondsToMidnight = repository.secondsToMidnight(),
            adStatusText = adManager.lastStatusMessage,
          )
        }
      }
    }
  }

  fun onAppResume() {
    soundEngine.onResume()
    adManager.preloadRewarded()
  }

  fun onAppPause() {
    soundEngine.onPause()
    persistCurrentSessionNow()
  }

  override fun onCleared() {
    soundEngine.release()
    super.onCleared()
  }

  fun navigateTo(screen: Screen) {
    soundEngine.play(SoundEffect.CLICK)
    if (screen != Screen.GAMEPLAY) {
      _uiState.update { it.copy(screen = screen, modal = ModalType.NONE, isPaused = false) }
    } else {
      _uiState.update { it.copy(screen = screen, modal = ModalType.NONE) }
    }
  }

  fun openModal(modal: ModalType) {
    soundEngine.play(SoundEffect.CLICK)
    _uiState.update { it.copy(modal = modal, adStatusText = adManager.lastStatusMessage) }
  }

  fun closeModal() {
    soundEngine.play(SoundEffect.CLICK)
    val state = _uiState.value
    if (state.modal == ModalType.PAUSE) {
      _uiState.update { it.copy(modal = ModalType.NONE, isPaused = false) }
    } else {
      _uiState.update { it.copy(modal = ModalType.NONE) }
    }
  }

  fun handleBackPressed(): Boolean {
    val state = _uiState.value
    if (state.modal != ModalType.NONE) {
      if (state.modal == ModalType.VICTORY || state.modal == ModalType.GAME_OVER) {
        goHomeFromGame()
      } else {
        closeModal()
      }
      return true
    }
    return when (state.screen) {
      Screen.GAMEPLAY -> {
        pauseGame()
        true
      }
      Screen.LEVEL_MAP -> {
        navigateTo(Screen.HOME)
        true
      }
      Screen.HOME -> {
        navigateTo(Screen.SPLASH)
        true
      }
      Screen.SPLASH -> false
    }
  }

  fun goHomeFromGame() {
    persistCurrentSessionNow()
    _uiState.update {
      it.copy(
        screen = Screen.HOME,
        modal = ModalType.NONE,
        isPaused = false,
        victoryState = null,
        gameOverState = null,
      )
    }
  }

  fun showToast(message: String) {
    val seq = ++toastSeq
    _uiState.update { it.copy(toastMessage = message) }
    viewModelScope.launch {
      delay(2400L)
      if (toastSeq == seq) {
        _uiState.update { it.copy(toastMessage = null) }
      }
    }
  }

  fun continueCurrentLevel() {
    val level = _uiState.value.progress.currentLevel.coerceIn(1, MAX_LEVELS)
    startLevel(levelNumber = level, isDaily = false, fresh = false)
  }

  fun startLevel(levelNumber: Int, isDaily: Boolean = false, fresh: Boolean = false) {
    soundEngine.play(SoundEffect.CLICK)
    viewModelScope.launch {
      val today = repository.effectiveTodayKey()
      val progress = repository.getSnapshot()
      val levelConfig = if (isDaily) {
        LevelGenerator.dailyLevelForDate(today)
      } else {
        repository.setCurrentLevel(levelNumber)
        LevelGenerator.generateLevel(levelNumber, maxRows = 10)
      }

      val saved = progress.activeSession
      val canRestore = !fresh &&
        saved != null &&
        saved.isDaily == isDaily &&
        saved.levelNumber == levelConfig.levelNumber &&
        (!isDaily || saved.dateKey == today)

      val bonusShots = if (!canRestore && !isDaily) repository.consumeBonusShotsForLevel() else 0
      if (!canRestore) {
        repository.incrementGamesPlayed()
      }

      val newEngine = BubbleEngine(
        levelConfig = levelConfig,
        bonusShots = bonusShots,
        restoredSession = if (canRestore) saved else null,
        callbacks = createEngineCallbacks(today),
      )
      engine = newEngine

      _uiState.update {
        it.copy(
          screen = Screen.GAMEPLAY,
          modal = ModalType.NONE,
          isPaused = false,
          activeLevelConfig = levelConfig,
          victoryState = null,
          gameOverState = null,
          hud = buildHud(newEngine),
          lastCanvasVersion = it.lastCanvasVersion + 1L,
        )
      }
      repository.saveSession(newEngine.exportSession(today))
    }
  }

  fun pauseGame() {
    if (_uiState.value.screen != Screen.GAMEPLAY) return
    val eng = engine
    if (eng != null && eng.phase == EnginePhase.ENDED) return
    persistCurrentSessionNow()
    soundEngine.play(SoundEffect.CLICK)
    _uiState.update { it.copy(isPaused = true, modal = ModalType.PAUSE) }
  }

  fun resumeGame() {
    soundEngine.play(SoundEffect.CLICK)
    _uiState.update { it.copy(isPaused = false, modal = ModalType.NONE) }
  }

  fun restartCurrentLevel() {
    val cfg = _uiState.value.activeLevelConfig ?: return
    startLevel(levelNumber = cfg.levelNumber, isDaily = cfg.isDaily, fresh = true)
  }

  fun stepFrame(dtSeconds: Float) {
    val eng = engine ?: return
    if (_uiState.value.isPaused || _uiState.value.screen != Screen.GAMEPLAY) return
    val prevPhase = eng.phase
    eng.step(dtSeconds)
    if (prevPhase != eng.phase || eng.projectile != null || eng.pops.isNotEmpty() || eng.falls.isNotEmpty() || eng.particles.isNotEmpty() || eng.floatingTexts.isNotEmpty() || eng.beams.isNotEmpty()) {
      _uiState.update {
        it.copy(
          hud = buildHud(eng),
          lastCanvasVersion = it.lastCanvasVersion + 1L,
        )
      }
    }
  }

  fun onPointerDown(x: Float, y: Float, cancelThresholdY: Float) {
    val eng = engine ?: return
    if (_uiState.value.isPaused || eng.phase == EnginePhase.ENDED) return
    if (eng.isTouchNearShooterOrNext(x, y)) {
      if (eng.swapBubbles()) {
        refreshCanvas()
      }
      return
    }
    eng.isAiming = true
    eng.inCancelZone = y >= cancelThresholdY
    eng.aimTowards(x, y)
    refreshCanvas()
  }

  fun onPointerMove(x: Float, y: Float, cancelThresholdY: Float) {
    val eng = engine ?: return
    if (!eng.isAiming || _uiState.value.isPaused || eng.phase == EnginePhase.ENDED) return
    eng.inCancelZone = y >= cancelThresholdY
    eng.aimTowards(x, y)
    refreshCanvas()
  }

  fun onPointerUp(x: Float, y: Float, cancelThresholdY: Float) {
    val eng = engine ?: return
    if (!eng.isAiming) return
    eng.isAiming = false
    eng.inCancelZone = false
    eng.aimTowards(x, y)
    if (y < cancelThresholdY) {
      shootCurrent()
    } else {
      refreshCanvas()
    }
  }

  fun onPointerCancel() {
    val eng = engine ?: return
    eng.isAiming = false
    eng.inCancelZone = false
    refreshCanvas()
  }

  fun swapShooterBubbles() {
    val eng = engine ?: return
    if (eng.swapBubbles()) {
      refreshCanvas()
    }
  }

  fun onBoosterTapped(booster: BoosterType) {
    val eng = engine ?: return
    if (_uiState.value.isPaused || eng.phase != EnginePhase.READY) return
    if (eng.levelConfig.noBooster) {
      showToast("No boosters in this challenge")
      return
    }
    val count = _uiState.value.progress.inventory[booster]
    if (count <= 0) {
      showToast("None left – get more in the Shop!")
      return
    }
    if (booster == BoosterType.FREEZE) {
      if (eng.freezeShots > 0) {
        showToast("Already frozen")
        return
      }
      viewModelScope.launch {
        if (repository.consumeBooster(BoosterType.FREEZE)) {
          eng.activateFreeze()
          refreshCanvas()
          checkAchievementsAndNotify()
        }
      }
      return
    }
    eng.armedBooster = if (eng.armedBooster == booster) null else booster
    soundEngine.play(SoundEffect.CLICK)
    refreshCanvas()
  }

  private fun shootCurrent() {
    val eng = engine ?: return
    val armed = eng.armedBooster
    if (armed != null) {
      val currentCount = _uiState.value.progress.inventory[armed]
      if (currentCount <= 0) {
        eng.armedBooster = null
        showToast("Out of ${armed.title} boosters")
        refreshCanvas()
        return
      }
      val fired = eng.shoot { boosterType ->
        viewModelScope.launch {
          repository.consumeBooster(boosterType)
          checkAchievementsAndNotify()
        }
        true
      }
      if (fired) refreshCanvas()
    } else {
      if (eng.shoot { false }) refreshCanvas()
    }
  }

  private fun refreshCanvas() {
    val eng = engine ?: return
    _uiState.update {
      it.copy(
        hud = buildHud(eng),
        lastCanvasVersion = it.lastCanvasVersion + 1L,
      )
    }
  }

  private fun buildHud(eng: BubbleEngine): HudSnapshot {
    val cfg = eng.levelConfig
    return HudSnapshot(
      levelNumber = cfg.levelNumber,
      isDaily = cfg.isDaily,
      score = eng.score,
      shotsLeft = eng.shotsLeft,
      shotsMax = eng.shotsMax,
      starsNow = eng.starsNow(),
      freezeShots = eng.freezeShots,
      dropsIn = if (cfg.dropEvery > 0) (cfg.dropEvery - eng.missCount).coerceAtLeast(1) else 0,
      remainingBubbles = eng.remainingBubbleCount(),
      removedCount = eng.removedCount,
      colorClearedCount = eng.colorClearedCount,
      armedBooster = eng.armedBooster,
    )
  }

  private fun createEngineCallbacks(todayKey: String): EngineAudioCallbacks = object : EngineAudioCallbacks {
    override fun onShoot(isFire: Boolean) {
      soundEngine.play(if (isFire) SoundEffect.FIRE else SoundEffect.SHOOT)
    }

    override fun onBounce() = soundEngine.play(SoundEffect.BOUNCE)
    override fun onStick() = soundEngine.play(SoundEffect.COLLISION)
    override fun onPop(index: Int) = soundEngine.playPop(index)
    override fun onFall() = soundEngine.play(SoundEffect.POP_1)
    override fun onCombo(combo: Int) = soundEngine.play(SoundEffect.COMBO)
    override fun onBoom() = soundEngine.play(SoundEffect.BOMB)
    override fun onZap() = soundEngine.play(SoundEffect.LIGHTNING)
    override fun onRainbow() = soundEngine.play(SoundEffect.RAINBOW)
    override fun onFreeze() = soundEngine.play(SoundEffect.FREEZE)
    override fun onRowDrop() = soundEngine.play(SoundEffect.COLLISION)
    override fun onSwap() = soundEngine.play(SoundEffect.CLICK)
    override fun onVibrate(ms: Long) = soundEngine.vibrate(ms)

    override fun onBubblesScored(bubblesCleared: Int, combo: Int) {
      viewModelScope.launch {
        repository.recordBubblesAndCombo(bubblesCleared, combo)
      }
    }

    override fun onSessionChanged(session: com.example.data.SavedSession) {
      viewModelScope.launch {
        repository.saveSession(session.copy(dateKey = todayKey))
      }
    }

    override fun onGameEnded(won: Boolean, reason: EndReason, stars: Int, score: Int) {
      val eng = engine ?: return
      viewModelScope.launch {
        val summary = repository.recordGameFinished(
          levelNumber = eng.levelConfig.levelNumber,
          isDaily = eng.levelConfig.isDaily,
          won = won,
          score = score,
          starsEarned = stars,
        )
        if (!won) {
          soundEngine.play(SoundEffect.GAME_OVER)
          soundEngine.vibrate(120)
          delay(750L)
          val latestProgress = repository.getSnapshot()
          _uiState.update {
            it.copy(
              modal = ModalType.GAME_OVER,
              gameOverState = GameOverState(
                levelNumber = eng.levelConfig.levelNumber,
                isDaily = eng.levelConfig.isDaily,
                score = score,
                highScore = latestProgress.highScore,
                reason = reason,
                canWatchExtraShots = eng.continuesUsed < 2,
              ),
            )
          }
        } else {
          val rewardSummary = summary ?: VictoryRewardsSummary(0, emptyList(), false)
          eng.lastCoinsAwarded = rewardSummary.coins
          soundEngine.play(SoundEffect.VICTORY)
          if (rewardSummary.unlockedNewLevel) {
            delay(380L)
            soundEngine.play(SoundEffect.UNLOCK)
          } else {
            delay(380L)
          }
          soundEngine.vibrate(70)
          delay(420L)
          _uiState.update {
            it.copy(
              modal = ModalType.VICTORY,
              victoryState = VictoryState(
                levelNumber = eng.levelConfig.levelNumber,
                isDaily = eng.levelConfig.isDaily,
                stars = stars,
                score = score,
                rewards = rewardSummary,
                coinsDoubled = false,
              ),
            )
          }
          checkAchievementsAndNotify()
        }
      }
    }
  }

  private fun persistCurrentSessionNow() {
    val eng = engine ?: return
    if (eng.phase == EnginePhase.ENDED) return
    viewModelScope.launch {
      val today = repository.effectiveTodayKey()
      repository.saveSession(eng.exportSession(today))
    }
  }

  /* ---------------- Meta Features & Modals ---------------- */

  fun claimDailyReward() {
    viewModelScope.launch {
      when (val res = repository.claimDailyReward()) {
        DailyClaimResult.AlreadyClaimed -> showToast("Already claimed today")
        is DailyClaimResult.Success -> {
          soundEngine.play(if (res.item.isChest) SoundEffect.CHEST else SoundEffect.REWARD)
          soundEngine.vibrate(40)
          showToast("Day ${res.day}: ${res.item.label}!")
        }
      }
    }
  }

  fun buyBooster(booster: BoosterType) {
    viewModelScope.launch {
      val ok = repository.buyBooster(booster)
      if (ok) {
        soundEngine.play(SoundEffect.COIN)
        showToast("Bought ${booster.title}!")
      } else {
        soundEngine.vibrate(60)
        showToast("Not enough coins")
      }
    }
  }

  fun spinFreeWheel() {
    if (_uiState.value.isWheelSpinning) return
    viewModelScope.launch {
      val seg = repository.spinWheel(useAd = false)
      if (seg == null) {
        showToast("No free spin left today")
        return@launch
      }
      animateWheelTo(seg)
    }
  }

  fun watchAdForExtraSpin() {
    if (_uiState.value.isWheelSpinning) return
    val progress = _uiState.value.progress
    val today = _uiState.value.todayKey
    val usedAd = if (progress.spinDate == today) progress.spinAdUsed else 0
    if (usedAd >= 2) {
      showToast("Come back tomorrow for more spins")
      return
    }
    launchRewardedAd(AdPlacement.EXTRA_SPIN) {
      viewModelScope.launch {
        val seg = repository.spinWheel(useAd = true)
        if (seg != null) animateWheelTo(seg)
      }
    }
  }

  private fun animateWheelTo(segment: SpinSegment) {
    val currentRot = _uiState.value.wheelTargetRotationDeg
    val jitter = (Random.nextFloat() - 0.5f) * 24f
    val targetAngle = -(segment.id * 45f + 22.5f + jitter)
    val delta = (((targetAngle - currentRot) % 360f) + 360f) % 360f
    val newRot = currentRot + 1800f + delta

    _uiState.update { it.copy(wheelTargetRotationDeg = newRot, isWheelSpinning = true) }
    viewModelScope.launch {
      repeat(28) {
        delay(130L)
        if (_uiState.value.isWheelSpinning) soundEngine.play(SoundEffect.TICK)
      }
      delay(650L)
      _uiState.update { it.copy(isWheelSpinning = false) }
      soundEngine.play(SoundEffect.REWARD)
      soundEngine.vibrate(70)
      val prizeText = when {
        segment.coins > 0 -> "${segment.coins} coins"
        segment.booster != null -> "${segment.booster.title} booster"
        else -> "+${segment.bonusShots} Shots for next level"
      }
      showToast("You won $prizeText!")
    }
  }

  fun watchAdForExtraShots() {
    val eng = engine ?: return
    if (eng.phase != EnginePhase.ENDED || eng.won || eng.continuesUsed >= 2) return
    launchRewardedAd(AdPlacement.EXTRA_SHOTS) {
      eng.continueAfterExtraShots()
      soundEngine.play(SoundEffect.REWARD)
      _uiState.update {
        it.copy(
          modal = ModalType.NONE,
          gameOverState = null,
          hud = buildHud(eng),
          lastCanvasVersion = it.lastCanvasVersion + 1L,
        )
      }
      showToast("+5 Extra Shots!")
    }
  }

  fun watchAdForDoubleCoins() {
    val victory = _uiState.value.victoryState ?: return
    if (victory.coinsDoubled || victory.rewards.coins <= 0) return
    launchRewardedAd(AdPlacement.DOUBLE_COINS) {
      viewModelScope.launch {
        repository.grantDoubleVictoryCoins(victory.rewards.coins)
        soundEngine.play(SoundEffect.COIN)
        _uiState.update {
          it.copy(victoryState = victory.copy(coinsDoubled = true))
        }
        showToast("+${victory.rewards.coins} bonus coins!")
      }
    }
  }

  fun watchAdForFreeBooster() {
    val progress = _uiState.value.progress
    val today = _uiState.value.todayKey
    val used = if (progress.stats.adBoostDate == today) progress.stats.adBoostCount else 0
    if (used >= 3) {
      showToast("Daily ad booster limit reached")
      return
    }
    launchRewardedAd(AdPlacement.FREE_BOOSTER) {
      viewModelScope.launch {
        val granted = repository.grantRewardedAdBooster()
        if (granted != null) {
          soundEngine.play(SoundEffect.REWARD)
          showToast("Free ${granted.title} booster!")
        }
      }
    }
  }

  fun watchAdForDailyBonus() {
    val progress = _uiState.value.progress
    if (progress.stats.dailyBonusAdDate == _uiState.value.todayKey) {
      showToast("Daily bonus already claimed today")
      return
    }
    launchRewardedAd(AdPlacement.DAILY_BONUS) {
      viewModelScope.launch {
        if (repository.grantRewardedDailyBonusCoins()) {
          soundEngine.play(SoundEffect.COIN)
          showToast("+100 bonus coins!")
        }
      }
    }
  }

  private fun launchRewardedAd(placement: AdPlacement, onReward: () -> Unit) {
    if (_uiState.value.adBusy) return
    _uiState.update { it.copy(adBusy = true, adStatusText = "Loading rewarded video...") }
    adManager.showRewardedAd(
      placement = placement,
      onRewardEarned = {
        _uiState.update { it.copy(adBusy = false, adStatusText = adManager.lastStatusMessage) }
        onReward()
      },
      onFailedOrCancelled = { msg ->
        _uiState.update { it.copy(adBusy = false, adStatusText = adManager.lastStatusMessage) }
        showToast(msg)
      },
    )
  }

  fun renamePlayer(name: String) {
    viewModelScope.launch {
      repository.renamePlayer(name)
      soundEngine.play(SoundEffect.CLICK)
      _uiState.update { it.copy(modal = ModalType.NONE) }
    }
  }

  fun updateSettings(transform: (GameSettings) -> GameSettings) {
    viewModelScope.launch {
      repository.updateSettings(transform)
    }
  }

  fun resetAllProgress() {
    viewModelScope.launch {
      engine = null
      repository.resetAllProgress()
      soundEngine.play(SoundEffect.CLICK)
      _uiState.update {
        it.copy(
          screen = Screen.HOME,
          modal = ModalType.NONE,
          isPaused = false,
          victoryState = null,
          gameOverState = null,
        )
      }
      showToast("Progress reset")
    }
  }

  fun nextLevelFromVictory() {
    val v = _uiState.value.victoryState
    if (v == null || v.isDaily || v.levelNumber >= MAX_LEVELS) {
      goHomeFromGame()
    } else {
      startLevel(v.levelNumber + 1, isDaily = false, fresh = true)
    }
  }

  private suspend fun checkAchievementsAndNotify() {
    val unlocked = repository.checkAndUnlockAchievements()
    if (unlocked.isNotEmpty()) {
      soundEngine.play(SoundEffect.REWARD)
      val first = unlocked.first()
      showToast("Achievement: ${first.title} (+${first.rewardCoins} coins)")
    }
  }
}
