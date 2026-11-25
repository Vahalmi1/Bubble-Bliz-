package com.example.data

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

const val GAME_VERSION = "1.2.0"
const val GRID_COLS = 10
const val MAX_LEVELS = 100
const val STARTIO_APP_ID = "209961859"

enum class Screen {
  SPLASH,
  HOME,
  LEVEL_MAP,
  GAMEPLAY,
}

enum class ModalType {
  NONE,
  DAILY_REWARD,
  DAILY_CHALLENGE,
  ACHIEVEMENTS,
  SHOP,
  LUCKY_SPIN,
  LEADERBOARD,
  STATS,
  SETTINGS,
  PAUSE,
  VICTORY,
  GAME_OVER,
  RENAME_PLAYER,
  CONFIRM_RESET,
}

enum class MascotMood {
  HAPPY,
  WINK,
  SAD,
}

enum class BubbleColor(
  val index: Int,
  val displayName: String,
  val light: Color,
  val mid: Color,
  val dark: Color,
) {
  RED(0, "red", BubbleRedLight, BubbleRedMid, BubbleRedDark),
  YELLOW(1, "yellow", BubbleYellowLight, BubbleYellowMid, BubbleYellowDark),
  GREEN(2, "green", BubbleGreenLight, BubbleGreenMid, BubbleGreenDark),
  BLUE(3, "blue", BubbleBlueLight, BubbleBlueMid, BubbleBlueDark),
  PURPLE(4, "purple", BubblePurpleLight, BubblePurpleMid, BubblePurpleDark),
  ORANGE(5, "orange", BubbleOrangeLight, BubbleOrangeMid, BubbleOrangeDark);

  companion object {
    fun fromIndex(idx: Int): BubbleColor = entries.getOrElse(idx) { RED }
  }
}

enum class BoosterType(
  val key: String,
  val title: String,
  val description: String,
  val price: Int,
) {
  BOMB("bomb", "Bomb", "Explodes and removes nearby bubbles.", 150),
  RAINBOW("rainbow", "Rainbow", "Matches any adjacent bubble color.", 200),
  LIGHTNING("lightning", "Lightning", "Zaps away a whole column of bubbles.", 250),
  FIRE("fire", "Fire", "Burns through up to 6 bubbles in its path.", 300),
  FREEZE("freeze", "Freeze", "Stops new rows from dropping for 5 shots.", 200);

  companion object {
    fun fromKey(key: String): BoosterType? = entries.firstOrNull { it.key == key }
  }
}

sealed interface LevelObjective {
  data object ClearAll : LevelObjective
  data class ReachScore(val target: Int) : LevelObjective
  data class ClearColor(val color: BubbleColor, val target: Int) : LevelObjective
  data class PopCount(val target: Int) : LevelObjective
}

enum class EndReason {
  NONE,
  DANGER_LINE,
  OUT_OF_SHOTS,
}

enum class AdPlacement(val key: String, val label: String) {
  EXTRA_SHOTS("extra_shots", "Extra Shots (+5)"),
  DOUBLE_COINS("double_coins", "Double Victory Coins"),
  FREE_BOOSTER("free_booster", "Free Random Booster"),
  DAILY_BONUS("daily_bonus", "Daily +100 Bonus Coins"),
  EXTRA_SPIN("extra_spin", "Extra Lucky Spin"),
}

data class BubbleCell(
  val color: BubbleColor = BubbleColor.RED,
  val isStone: Boolean = false,
  val hasBomb: Boolean = false,
)

data class LevelConfig(
  val levelNumber: Int,
  val grid: List<List<BubbleCell?>>,
  val objective: LevelObjective,
  val shots: Int,
  val palette: List<BubbleColor>,
  val dropEvery: Int,
  val bubbleCount: Int,
  val pattern: String,
  val topColor: BubbleColor,
  val noBooster: Boolean = false,
  val isDaily: Boolean = false,
  val dailyType: Int = 0,
)

data class DailyRewardItem(
  val day: Int,
  val label: String,
  val coins: Int = 0,
  val fixedBoosters: Map<BoosterType, Int> = emptyMap(),
  val randomBoosters: Int = 0,
  val isChest: Boolean = false,
)

data class AchievementDef(
  val id: String,
  val title: String,
  val description: String,
  val rewardCoins: Int,
  val target: Int,
)

data class SpinSegment(
  val id: Int,
  val label: String,
  val weight: Int,
  val sliceColor: Color,
  val coins: Int = 0,
  val booster: BoosterType? = null,
  val bonusShots: Int = 0,
)

data class BoosterInventory(
  val bomb: Int = 3,
  val rainbow: Int = 2,
  val lightning: Int = 2,
  val fire: Int = 1,
  val freeze: Int = 1,
) {
  operator fun get(type: BoosterType): Int = when (type) {
    BoosterType.BOMB -> bomb
    BoosterType.RAINBOW -> rainbow
    BoosterType.LIGHTNING -> lightning
    BoosterType.FIRE -> fire
    BoosterType.FREEZE -> freeze
  }

  fun plus(type: BoosterType, delta: Int): BoosterInventory = when (type) {
    BoosterType.BOMB -> copy(bomb = (bomb + delta).coerceAtLeast(0))
    BoosterType.RAINBOW -> copy(rainbow = (rainbow + delta).coerceAtLeast(0))
    BoosterType.LIGHTNING -> copy(lightning = (lightning + delta).coerceAtLeast(0))
    BoosterType.FIRE -> copy(fire = (fire + delta).coerceAtLeast(0))
    BoosterType.FREEZE -> copy(freeze = (freeze + delta).coerceAtLeast(0))
  }

  fun addAll(map: Map<BoosterType, Int>): BoosterInventory {
    var inv = this
    for ((k, v) in map) inv = inv.plus(k, v)
    return inv
  }
}

data class GameSettings(
  val sfxEnabled: Boolean = true,
  val musicEnabled: Boolean = true,
  val vibrationEnabled: Boolean = true,
  val sfxVolume: Int = 80,
  val musicVolume: Int = 55,
)

data class PlayerStats(
  val bubblesPopped: Int = 0,
  val boostersUsed: Int = 0,
  val maxCombo: Int = 0,
  val gamesPlayed: Int = 0,
  val wins: Int = 0,
  val dailyWins: Int = 0,
  val totalScore: Long = 0L,
  val coinsEarned: Long = 0L,
  val weekKey: String = "",
  val weekScore: Long = 0L,
  val adBoostDate: String = "",
  val adBoostCount: Int = 0,
  val dailyBonusAdDate: String = "",
)

data class ScoreEntry(
  val score: Int,
  val levelNumber: Int, // 0 = Daily Challenge
  val dateKey: String,
)

data class SavedSession(
  val levelNumber: Int,
  val isDaily: Boolean,
  val dateKey: String,
  val grid: List<List<BubbleCell?>>,
  val parity: Int,
  val shotsLeft: Int,
  val shotsMax: Int,
  val score: Int,
  val removedCount: Int,
  val colorClearedCount: Int,
  val combo: Int,
  val maxCombo: Int,
  val missCount: Int,
  val freezeShots: Int,
  val currentColor: BubbleColor,
  val nextColor: BubbleColor,
  val usedBooster: Boolean,
)

data class PlayerProgress(
  val playerName: String = "Player",
  val currentLevel: Int = 1,
  val unlockedLevel: Int = 1,
  val coins: Int = 300,
  val inventory: BoosterInventory = BoosterInventory(),
  val highScore: Int = 0,
  val stars: Map<Int, Int> = emptyMap(),
  val unlockedAchievements: Map<String, Long> = emptyMap(),
  val dailyLastDate: String = "",
  val dailyStreakDay: Int = 0,
  val challengeDate: String = "",
  val challengeCompleted: Boolean = false,
  val spinDate: String = "",
  val spinFreeUsed: Int = 0,
  val spinAdUsed: Int = 0,
  val bonusShotsNextLevel: Int = 0,
  val settings: GameSettings = GameSettings(),
  val stats: PlayerStats = PlayerStats(),
  val topScores: List<ScoreEntry> = emptyList(),
  val maxSeenDate: String = "",
  val activeSession: SavedSession? = null,
) {
  val completedLevelsCount: Int get() = stars.values.count { it > 0 }
  val totalStars: Int get() = stars.values.sum()
  val hasResumeSession: Boolean
    get() = activeSession != null && !activeSession.isDaily && activeSession.levelNumber == currentLevel
}

object GameCatalog {
  val DAILY_REWARDS = listOf(
    DailyRewardItem(1, "100 Coins", coins = 100),
    DailyRewardItem(2, "150 Coins", coins = 150),
    DailyRewardItem(3, "1 Bomb", fixedBoosters = mapOf(BoosterType.BOMB to 1)),
    DailyRewardItem(4, "250 Coins", coins = 250),
    DailyRewardItem(5, "2 Boosters", randomBoosters = 2),
    DailyRewardItem(6, "400 Coins", coins = 400),
    DailyRewardItem(
      day = 7,
      label = "Treasure!",
      coins = 500,
      fixedBoosters = mapOf(
        BoosterType.BOMB to 1,
        BoosterType.RAINBOW to 1,
        BoosterType.LIGHTNING to 1,
        BoosterType.FIRE to 1,
        BoosterType.FREEZE to 1,
      ),
      isChest = true,
    ),
  )

  val ACHIEVEMENTS = listOf(
    AchievementDef("pop1", "First Pop", "Pop your first bubble", 25, 1),
    AchievementDef("win1", "First Victory", "Complete a level", 50, 1),
    AchievementDef("lv10", "10 Levels Completed", "Complete 10 levels", 100, 10),
    AchievementDef("lv25", "25 Levels Completed", "Complete 25 levels", 250, 25),
    AchievementDef("lv100", "100 Levels Completed", "Conquer Green Valley", 1000, 100),
    AchievementDef("b1000", "Bubble Crusher", "Destroy 1,000 bubbles", 300, 1000),
    AchievementDef("combo10", "10 Combo", "Pop with 10 shots in a row", 150, 10),
    AchievementDef("star3", "3-Star Master", "Earn 3 stars on 10 levels", 250, 10),
    AchievementDef("boost", "Booster Expert", "Use 25 boosters", 200, 25),
    AchievementDef("daily1", "Daily Challenger", "Complete a Daily Challenge", 100, 1),
  )

  val SPIN_SEGMENTS = listOf(
    SpinSegment(0, "50", 26, Color(0xFF7B3FE4), coins = 50),
    SpinSegment(1, "Bomb", 10, Color(0xFFFF7A00), booster = BoosterType.BOMB),
    SpinSegment(2, "100", 18, Color(0xFF1E7BFF), coins = 100),
    SpinSegment(3, "Rainbow", 10, Color(0xFF2FBF2F), booster = BoosterType.RAINBOW),
    SpinSegment(4, "Lightning", 10, Color(0xFFE5212B), booster = BoosterType.LIGHTNING),
    SpinSegment(5, "+3 Shots", 12, Color(0xFF9B3DF0), bonusShots = 3),
    SpinSegment(6, "200", 6, Color(0xFFFFC400), coins = 200),
    SpinSegment(7, "Fire", 8, Color(0xFF00A6C8), booster = BoosterType.FIRE),
  )
}
