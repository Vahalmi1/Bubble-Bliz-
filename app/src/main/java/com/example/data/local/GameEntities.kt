package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_state")
data class PlayerStateEntity(
  @PrimaryKey val id: Int = 1,
  val playerName: String = "Player",
  val currentLevel: Int = 1,
  val unlockedLevel: Int = 1,
  val coins: Int = 300,
  val bombCount: Int = 3,
  val rainbowCount: Int = 2,
  val lightningCount: Int = 2,
  val fireCount: Int = 1,
  val freezeCount: Int = 1,
  val highScore: Int = 0,
  val dailyLastDate: String = "",
  val dailyStreakDay: Int = 0,
  val challengeDate: String = "",
  val challengeCompleted: Boolean = false,
  val spinDate: String = "",
  val spinFreeUsed: Int = 0,
  val spinAdUsed: Int = 0,
  val bonusShotsNextLevel: Int = 0,
  val sfxEnabled: Boolean = true,
  val musicEnabled: Boolean = true,
  val vibrationEnabled: Boolean = true,
  val sfxVolume: Int = 80,
  val musicVolume: Int = 55,
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
  val maxSeenDate: String = "",
)

@Entity(tableName = "level_stars")
data class LevelStarEntity(
  @PrimaryKey val levelNumber: Int,
  val stars: Int,
)

@Entity(tableName = "achievements")
data class AchievementEntity(
  @PrimaryKey val achievementId: String,
  val unlockedAtMillis: Long,
)

@Entity(tableName = "score_records")
data class ScoreRecordEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val score: Int,
  val levelNumber: Int,
  val dateKey: String,
)

@Entity(tableName = "saved_session")
data class SavedSessionEntity(
  @PrimaryKey val id: Int = 1,
  val levelNumber: Int,
  val isDaily: Boolean,
  val dateKey: String,
  val gridJson: String,
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
  val currentColorIndex: Int,
  val nextColorIndex: Int,
  val usedBooster: Boolean,
)
