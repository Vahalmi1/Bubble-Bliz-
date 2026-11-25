package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
  @Query("SELECT * FROM player_state WHERE id = 1")
  fun observePlayerState(): Flow<PlayerStateEntity?>

  @Query("SELECT * FROM player_state WHERE id = 1")
  suspend fun getPlayerState(): PlayerStateEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPlayerState(state: PlayerStateEntity)

  @Query("SELECT * FROM level_stars")
  fun observeLevelStars(): Flow<List<LevelStarEntity>>

  @Query("SELECT * FROM level_stars")
  suspend fun getLevelStars(): List<LevelStarEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertLevelStar(star: LevelStarEntity)

  @Query("DELETE FROM level_stars")
  suspend fun clearLevelStars()

  @Query("SELECT * FROM achievements")
  fun observeAchievements(): Flow<List<AchievementEntity>>

  @Query("SELECT * FROM achievements")
  suspend fun getAchievements(): List<AchievementEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAchievement(achievement: AchievementEntity)

  @Query("DELETE FROM achievements")
  suspend fun clearAchievements()

  @Query("SELECT * FROM score_records ORDER BY score DESC LIMIT 10")
  fun observeTopScores(): Flow<List<ScoreRecordEntity>>

  @Query("SELECT * FROM score_records ORDER BY score DESC LIMIT 10")
  suspend fun getTopScores(): List<ScoreRecordEntity>

  @Insert
  suspend fun insertScore(record: ScoreRecordEntity)

  @Query("DELETE FROM score_records WHERE id NOT IN (SELECT id FROM score_records ORDER BY score DESC LIMIT 10)")
  suspend fun trimScoresToTop10()

  @Query("DELETE FROM score_records")
  suspend fun clearScores()

  @Query("SELECT * FROM saved_session WHERE id = 1")
  fun observeSavedSession(): Flow<SavedSessionEntity?>

  @Query("SELECT * FROM saved_session WHERE id = 1")
  suspend fun getSavedSession(): SavedSessionEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSavedSession(session: SavedSessionEntity)

  @Query("DELETE FROM saved_session")
  suspend fun clearSavedSession()

  @Transaction
  suspend fun resetAllProgress(defaultState: PlayerStateEntity) {
    clearLevelStars()
    clearAchievements()
    clearScores()
    clearSavedSession()
    upsertPlayerState(defaultState)
  }
}
