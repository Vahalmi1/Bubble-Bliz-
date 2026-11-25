package com.example.data

import com.example.data.local.AchievementEntity
import com.example.data.local.GameDao
import com.example.data.local.LevelStarEntity
import com.example.data.local.PlayerStateEntity
import com.example.data.local.SavedSessionEntity
import com.example.data.local.ScoreRecordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.json.JSONArray
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class GameRepository(
  private val dao: GameDao,
  private val clockMillis: () -> Long = { System.currentTimeMillis() },
) {

  val progressFlow: Flow<PlayerProgress> = combine(
    dao.observePlayerState(),
    dao.observeLevelStars(),
    dao.observeAchievements(),
    dao.observeTopScores(),
    dao.observeSavedSession(),
  ) { state, stars, achievements, scores, session ->
    mapProgress(state, stars, achievements, scores, session)
  }

  suspend fun ensureInitialized(): PlayerProgress {
    val existing = dao.getPlayerState()
    if (existing == null) {
      dao.upsertPlayerState(PlayerStateEntity())
    }
    return getSnapshot()
  }

  suspend fun getSnapshot(): PlayerProgress {
    val state = dao.getPlayerState() ?: PlayerStateEntity().also { dao.upsertPlayerState(it) }
    val stars = dao.getLevelStars()
    val achievements = dao.getAchievements()
    val scores = dao.getTopScores()
    val session = dao.getSavedSession()
    return mapProgress(state, stars, achievements, scores, session)
  }

  fun realDateKey(): String {
    val cal = Calendar.getInstance()
    cal.timeInMillis = clockMillis()
    val y = cal.get(Calendar.YEAR)
    val m = cal.get(Calendar.MONTH) + 1
    val d = cal.get(Calendar.DAY_OF_MONTH)
    return String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
  }

  suspend fun effectiveTodayKey(): String {
    val state = dao.getPlayerState() ?: PlayerStateEntity()
    val real = realDateKey()
    return if (state.maxSeenDate.isBlank() || real > state.maxSeenDate) {
      if (state.maxSeenDate != real) {
        dao.upsertPlayerState(state.copy(maxSeenDate = real))
      }
      real
    } else {
      state.maxSeenDate
    }
  }

  fun dayDifference(fromDate: String, toDate: String): Int {
    val fromMs = parseDateUtcMillis(fromDate) ?: return 999
    val toMs = parseDateUtcMillis(toDate) ?: return 999
    return ((toMs - fromMs) / 86_400_000.0).roundToInt()
  }

  fun weekKeyFor(dateKey: String): String {
    val parts = dateKey.split("-")
    if (parts.size != 3) return dateKey
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.clear()
    cal.set(parts[0].toIntOrNull() ?: 2026, (parts[1].toIntOrNull() ?: 1) - 1, parts[2].toIntOrNull() ?: 1)
    val dow = cal.get(Calendar.DAY_OF_WEEK)
    val offsetFromMonday = (dow + 5) % 7 // Mon=0..Sun=6
    cal.add(Calendar.DAY_OF_MONTH, -offsetFromMonday)
    return String.format(
      Locale.US,
      "%04d-%02d-%02d",
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH) + 1,
      cal.get(Calendar.DAY_OF_MONTH),
    )
  }

  fun secondsToMidnight(): Long {
    val now = Calendar.getInstance()
    now.timeInMillis = clockMillis()
    val midnight = (now.clone() as Calendar).apply {
      add(Calendar.DAY_OF_MONTH, 1)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    return max(0L, (midnight.timeInMillis - now.timeInMillis) / 1000L)
  }

  fun nextDailyRewardDay(progress: PlayerProgress, today: String): Int {
    val last = progress.dailyLastDate
    if (last.isBlank()) return 1
    val diff = dayDifference(last, today)
    return when {
      diff == 1 -> (progress.dailyStreakDay % 7) + 1
      diff <= 0 -> progress.dailyStreakDay.coerceIn(1, 7)
      else -> 1
    }
  }

  suspend fun claimDailyReward(): DailyClaimResult {
    val today = effectiveTodayKey()
    val state = dao.getPlayerState() ?: PlayerStateEntity()
    if (state.dailyLastDate == today) {
      return DailyClaimResult.AlreadyClaimed
    }
    val progress = getSnapshot()
    val day = nextDailyRewardDay(progress, today)
    val reward = GameCatalog.DAILY_REWARDS[day - 1]
    var updated = state.copy(
      dailyLastDate = today,
      dailyStreakDay = day,
      maxSeenDate = maxDate(state.maxSeenDate, today),
    )
    if (reward.coins > 0) {
      updated = updated.copy(
        coins = updated.coins + reward.coins,
        coinsEarned = updated.coinsEarned + reward.coins,
      )
    }
    for ((booster, count) in reward.fixedBoosters) {
      updated = addBoosterToEntity(updated, booster, count)
    }
    val randomGranted = mutableListOf<BoosterType>()
    repeat(reward.randomBoosters) {
      val b = BoosterType.entries.random()
      updated = addBoosterToEntity(updated, b, 1)
      randomGranted.add(b)
    }
    dao.upsertPlayerState(updated)
    return DailyClaimResult.Success(day = day, item = reward, randomBoosters = randomGranted)
  }

  suspend fun buyBooster(booster: BoosterType): Boolean {
    val state = dao.getPlayerState() ?: PlayerStateEntity()
    if (state.coins < booster.price) return false
    val updated = addBoosterToEntity(
      state.copy(coins = state.coins - booster.price),
      booster,
      1,
    )
    dao.upsertPlayerState(updated)
    return true
  }

  suspend fun grantRewardedAdBooster(): BoosterType? {
    val today = effectiveTodayKey()
    val state = dao.getPlayerState() ?: PlayerStateEntity()
    val countToday = if (state.adBoostDate == today) state.adBoostCount else 0
    if (countToday >= 3) return null
    val chosen = BoosterType.entries.random()
    val updated = addBoosterToEntity(
      state.copy(adBoostDate = today, adBoostCount = countToday + 1),
      chosen,
      1,
    )
    dao.upsertPlayerState(updated)
    return chosen
  }

  suspend fun grantRewardedDailyBonusCoins(): Boolean {
    val today = effectiveTodayKey()
    val state = dao.getPlayerState() ?: PlayerStateEntity()
    if (state.dailyBonusAdDate == today) return false
    dao.upsertPlayerState(
      state.copy(
        coins = state.coins + 100,
        coinsEarned = state.coinsEarned + 100,
        dailyBonusAdDate = today,
      ),
    )
    return true
  }

  suspend fun spinWheel(useAd: Boolean): SpinSegment? {
    val today = effectiveTodayKey()
    val state = dao.getPlayerState() ?: PlayerStateEntity()
    val usedFree = if (state.spinDate == today) state.spinFreeUsed else 0
    val usedAd = if (state.spinDate == today) state.spinAdUsed else 0

    if (!useAd && usedFree >= 1) return null
    if (useAd && usedAd >= 2) return null

    val totalWeight = GameCatalog.SPIN_SEGMENTS.sumOf { it.weight }
    var roll = Math.random() * totalWeight
    var chosen = GameCatalog.SPIN_SEGMENTS.first()
    for (seg in GameCatalog.SPIN_SEGMENTS) {
      roll -= seg.weight
      if (roll <= 0) {
        chosen = seg
        break
      }
    }

    var updated = state.copy(
      spinDate = today,
      spinFreeUsed = if (!useAd) usedFree + 1 else usedFree,
      spinAdUsed = if (useAd) usedAd + 1 else usedAd,
    )
    if (chosen.coins > 0) {
      updated = updated.copy(
        coins = updated.coins + chosen.coins,
        coinsEarned = updated.coinsEarned + chosen.coins,
      )
    }
    if (chosen.booster != null) {
      updated = addBoosterToEntity(updated, chosen.booster, 1)
    }
    if (chosen.bonusShots > 0) {
      updated = updated.copy(bonusShotsNextLevel = updated.bonusShotsNextLevel + chosen.bonusShots)
    }
    dao.upsertPlayerState(updated)
    return chosen
  }

  suspend fun consumeBonusShotsForLevel(): Int {
    val state = dao.getPlayerState() ?: PlayerStateEntity()
    val bonus = state.bonusShotsNextLevel
    if (bonus > 0) {
      dao.upsertPlayerState(state.copy(bonusShotsNextLevel = 0))
    }
    return bonus
  }

  suspend fun consumeBooster(booster: BoosterType): Boolean {
    val state = dao.getPlayerState() ?: PlayerStateEntity()
    val count = getBoosterFromEntity(state, booster)
    if (count <= 0) return false
    val updated = addBoosterToEntity(
      state.copy(boostersUsed = state.boostersUsed + 1),
      booster,
      -1,
    )
    dao.upsertPlayerState(updated)
    return true
  }

  suspend fun incrementGamesPlayed() {
    val state = dao.getPlayerState() ?: PlayerStateEntity()
    dao.upsertPlayerState(state.copy(gamesPlayed = state.gamesPlayed + 1))
  }

  suspend fun recordBubblesAndCombo(deltaBubbles: Int, combo: Int) {
    if (deltaBubbles <= 0 && combo <= 0) return
    val state = dao.getPlayerState() ?: PlayerStateEntity()
    dao.upsertPlayerState(
      state.copy(
        bubblesPopped = state.bubblesPopped + deltaBubbles,
        maxCombo = max(state.maxCombo, combo),
      ),
    )
  }

  suspend fun recordGameFinished(
    levelNumber: Int,
    isDaily: Boolean,
    won: Boolean,
    score: Int,
    starsEarned: Int,
  ): VictoryRewardsSummary? {
    val today = effectiveTodayKey()
    val wk = weekKeyFor(today)
    dao.clearSavedSession()

    val state = dao.getPlayerState() ?: PlayerStateEntity()
    val currentWeekScore = if (state.weekKey == wk) state.weekScore else 0L
    var updated = state.copy(
      highScore = max(state.highScore, score),
      totalScore = state.totalScore + score,
      weekKey = wk,
      weekScore = currentWeekScore + score,
    )

    dao.insertScore(ScoreRecordEntity(score = score, levelNumber = if (isDaily) 0 else levelNumber, dateKey = today))
    dao.trimScoresToTop10()

    if (!won) {
      dao.upsertPlayerState(updated)
      return null
    }

    updated = updated.copy(wins = updated.wins + 1)
    var coinsEarned = 0
    val bonusBoosters = mutableListOf<BoosterType>()
    var unlockedNewLevel = false

    if (isDaily) {
      val firstToday = !(state.challengeDate == today && state.challengeCompleted)
      if (firstToday) {
        coinsEarned = 200
        val bonus = BoosterType.entries.random()
        updated = addBoosterToEntity(updated, bonus, 1)
        bonusBoosters.add(bonus)
        updated = updated.copy(
          challengeDate = today,
          challengeCompleted = true,
          dailyWins = updated.dailyWins + 1,
        )
      } else {
        coinsEarned = 20
      }
    } else {
      val prevStars = dao.getLevelStars().firstOrNull { it.levelNumber == levelNumber }?.stars ?: 0
      val firstClear = prevStars == 0
      coinsEarned = 40 + starsEarned * 20 + min(60, levelNumber)
      if (!firstClear) {
        coinsEarned = (coinsEarned * 0.4).roundToInt()
      }
      dao.upsertLevelStar(LevelStarEntity(levelNumber, max(prevStars, starsEarned)))

      if (firstClear) {
        if (levelNumber % 10 == 0) {
          coinsEarned += 250
          repeat(2) {
            val bonus = BoosterType.entries.random()
            updated = addBoosterToEntity(updated, bonus, 1)
            bonusBoosters.add(bonus)
          }
        } else if (levelNumber % 3 == 0) {
          val bonus = BoosterType.entries.random()
          updated = addBoosterToEntity(updated, bonus, 1)
          bonusBoosters.add(bonus)
        }
      }

      if (levelNumber < MAX_LEVELS && state.unlockedLevel < levelNumber + 1) {
        unlockedNewLevel = true
      }
      val newUnlocked = max(state.unlockedLevel, min(MAX_LEVELS, levelNumber + 1))
      updated = updated.copy(
        unlockedLevel = newUnlocked,
        currentLevel = newUnlocked,
      )
    }

    updated = updated.copy(
      coins = updated.coins + coinsEarned,
      coinsEarned = updated.coinsEarned + coinsEarned,
    )
    dao.upsertPlayerState(updated)

    return VictoryRewardsSummary(
      coins = coinsEarned,
      bonusBoosters = bonusBoosters,
      unlockedNewLevel = unlockedNewLevel,
    )
  }

  suspend fun grantDoubleVictoryCoins(coinsToAdd: Int) {
    if (coinsToAdd <= 0) return
    val state = dao.getPlayerState() ?: PlayerStateEntity()
    dao.upsertPlayerState(
      state.copy(
        coins = state.coins + coinsToAdd,
        coinsEarned = state.coinsEarned + coinsToAdd,
      ),
    )
  }

  suspend fun setCurrentLevel(levelNumber: Int) {
    val state = dao.getPlayerState() ?: PlayerStateEntity()
    dao.upsertPlayerState(state.copy(currentLevel = levelNumber.coerceIn(1, state.unlockedLevel)))
  }

  suspend fun renamePlayer(name: String) {
    val clean = name.trim().take(18).ifBlank { "Player" }
    val state = dao.getPlayerState() ?: PlayerStateEntity()
    dao.upsertPlayerState(state.copy(playerName = clean))
  }

  suspend fun updateSettings(transform: (GameSettings) -> GameSettings) {
    val state = dao.getPlayerState() ?: PlayerStateEntity()
    val current = GameSettings(
      sfxEnabled = state.sfxEnabled,
      musicEnabled = state.musicEnabled,
      vibrationEnabled = state.vibrationEnabled,
      sfxVolume = state.sfxVolume,
      musicVolume = state.musicVolume,
    )
    val next = transform(current)
    dao.upsertPlayerState(
      state.copy(
        sfxEnabled = next.sfxEnabled,
        musicEnabled = next.musicEnabled,
        vibrationEnabled = next.vibrationEnabled,
        sfxVolume = next.sfxVolume.coerceIn(0, 100),
        musicVolume = next.musicVolume.coerceIn(0, 100),
      ),
    )
  }

  suspend fun saveSession(session: SavedSession) {
    dao.upsertSavedSession(
      SavedSessionEntity(
        id = 1,
        levelNumber = session.levelNumber,
        isDaily = session.isDaily,
        dateKey = session.dateKey,
        gridJson = encodeGrid(session.grid),
        parity = session.parity,
        shotsLeft = session.shotsLeft,
        shotsMax = session.shotsMax,
        score = session.score,
        removedCount = session.removedCount,
        colorClearedCount = session.colorClearedCount,
        combo = session.combo,
        maxCombo = session.maxCombo,
        missCount = session.missCount,
        freezeShots = session.freezeShots,
        currentColorIndex = session.currentColor.index,
        nextColorIndex = session.nextColor.index,
        usedBooster = session.usedBooster,
      ),
    )
  }

  suspend fun clearSession() {
    dao.clearSavedSession()
  }

  suspend fun checkAndUnlockAchievements(): List<AchievementDef> {
    val progress = getSnapshot()
    val newlyUnlocked = mutableListOf<AchievementDef>()
    var addedCoins = 0
    val now = clockMillis()

    for (def in GameCatalog.ACHIEVEMENTS) {
      if (progress.unlockedAchievements.containsKey(def.id)) continue
      val currentValue = achievementProgressValue(def.id, progress)
      if (currentValue >= def.target) {
        dao.upsertAchievement(AchievementEntity(def.id, now))
        newlyUnlocked.add(def)
        addedCoins += def.rewardCoins
      }
    }

    if (addedCoins > 0) {
      val state = dao.getPlayerState() ?: PlayerStateEntity()
      dao.upsertPlayerState(
        state.copy(
          coins = state.coins + addedCoins,
          coinsEarned = state.coinsEarned + addedCoins,
        ),
      )
    }
    return newlyUnlocked
  }

  fun achievementProgressValue(id: String, progress: PlayerProgress): Int = when (id) {
    "pop1" -> min(1, progress.stats.bubblesPopped)
    "win1" -> min(1, progress.completedLevelsCount)
    "lv10" -> min(10, progress.completedLevelsCount)
    "lv25" -> min(25, progress.completedLevelsCount)
    "lv100" -> min(100, progress.completedLevelsCount)
    "b1000" -> min(1000, progress.stats.bubblesPopped)
    "combo10" -> min(10, progress.stats.maxCombo)
    "star3" -> min(10, progress.stars.values.count { it >= 3 })
    "boost" -> min(25, progress.stats.boostersUsed)
    "daily1" -> min(1, progress.stats.dailyWins)
    else -> 0
  }

  suspend fun resetAllProgress() {
    val current = dao.getPlayerState()
    val preservedSettings = if (current != null) {
      PlayerStateEntity(
        sfxEnabled = current.sfxEnabled,
        musicEnabled = current.musicEnabled,
        vibrationEnabled = current.vibrationEnabled,
        sfxVolume = current.sfxVolume,
        musicVolume = current.musicVolume,
      )
    } else {
      PlayerStateEntity()
    }
    dao.resetAllProgress(preservedSettings)
  }

  private fun getBoosterFromEntity(state: PlayerStateEntity, booster: BoosterType): Int = when (booster) {
    BoosterType.BOMB -> state.bombCount
    BoosterType.RAINBOW -> state.rainbowCount
    BoosterType.LIGHTNING -> state.lightningCount
    BoosterType.FIRE -> state.fireCount
    BoosterType.FREEZE -> state.freezeCount
  }

  private fun addBoosterToEntity(state: PlayerStateEntity, booster: BoosterType, delta: Int): PlayerStateEntity =
    when (booster) {
      BoosterType.BOMB -> state.copy(bombCount = max(0, state.bombCount + delta))
      BoosterType.RAINBOW -> state.copy(rainbowCount = max(0, state.rainbowCount + delta))
      BoosterType.LIGHTNING -> state.copy(lightningCount = max(0, state.lightningCount + delta))
      BoosterType.FIRE -> state.copy(fireCount = max(0, state.fireCount + delta))
      BoosterType.FREEZE -> state.copy(freezeCount = max(0, state.freezeCount + delta))
    }

  private fun mapProgress(
    stateEntity: PlayerStateEntity?,
    stars: List<LevelStarEntity>,
    achievements: List<AchievementEntity>,
    scores: List<ScoreRecordEntity>,
    sessionEntity: SavedSessionEntity?,
  ): PlayerProgress {
    val s = stateEntity ?: PlayerStateEntity()
    return PlayerProgress(
      playerName = s.playerName,
      currentLevel = s.currentLevel,
      unlockedLevel = s.unlockedLevel,
      coins = s.coins,
      inventory = BoosterInventory(
        bomb = s.bombCount,
        rainbow = s.rainbowCount,
        lightning = s.lightningCount,
        fire = s.fireCount,
        freeze = s.freezeCount,
      ),
      highScore = s.highScore,
      stars = stars.associate { it.levelNumber to it.stars },
      unlockedAchievements = achievements.associate { it.achievementId to it.unlockedAtMillis },
      dailyLastDate = s.dailyLastDate,
      dailyStreakDay = s.dailyStreakDay,
      challengeDate = s.challengeDate,
      challengeCompleted = s.challengeCompleted,
      spinDate = s.spinDate,
      spinFreeUsed = s.spinFreeUsed,
      spinAdUsed = s.spinAdUsed,
      bonusShotsNextLevel = s.bonusShotsNextLevel,
      settings = GameSettings(
        sfxEnabled = s.sfxEnabled,
        musicEnabled = s.musicEnabled,
        vibrationEnabled = s.vibrationEnabled,
        sfxVolume = s.sfxVolume,
        musicVolume = s.musicVolume,
      ),
      stats = PlayerStats(
        bubblesPopped = s.bubblesPopped,
        boostersUsed = s.boostersUsed,
        maxCombo = s.maxCombo,
        gamesPlayed = s.gamesPlayed,
        wins = s.wins,
        dailyWins = s.dailyWins,
        totalScore = s.totalScore,
        coinsEarned = s.coinsEarned,
        weekKey = s.weekKey,
        weekScore = s.weekScore,
        adBoostDate = s.adBoostDate,
        adBoostCount = s.adBoostCount,
        dailyBonusAdDate = s.dailyBonusAdDate,
      ),
      topScores = scores.map { ScoreEntry(it.score, it.levelNumber, it.dateKey) },
      maxSeenDate = s.maxSeenDate,
      activeSession = sessionEntity?.let {
        SavedSession(
          levelNumber = it.levelNumber,
          isDaily = it.isDaily,
          dateKey = it.dateKey,
          grid = decodeGrid(it.gridJson),
          parity = it.parity,
          shotsLeft = it.shotsLeft,
          shotsMax = it.shotsMax,
          score = it.score,
          removedCount = it.removedCount,
          colorClearedCount = it.colorClearedCount,
          combo = it.combo,
          maxCombo = it.maxCombo,
          missCount = it.missCount,
          freezeShots = it.freezeShots,
          currentColor = BubbleColor.fromIndex(it.currentColorIndex),
          nextColor = BubbleColor.fromIndex(it.nextColorIndex),
          usedBooster = it.usedBooster,
        )
      },
    )
  }

  private fun parseDateUtcMillis(dateKey: String): Long? {
    val parts = dateKey.split("-")
    if (parts.size != 3) return null
    val y = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    val d = parts[2].toIntOrNull() ?: return null
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.clear()
    cal.set(y, m - 1, d)
    return cal.timeInMillis
  }

  private fun maxDate(a: String, b: String): String = when {
    a.isBlank() -> b
    b.isBlank() -> a
    a >= b -> a
    else -> b
  }

  private fun encodeGrid(grid: List<List<BubbleCell?>>): String {
    val outer = JSONArray()
    for (row in grid) {
      val rowArr = JSONArray()
      for (cell in row) {
        if (cell == null) {
          rowArr.put(0)
        } else {
          val cellArr = JSONArray()
          cellArr.put(cell.color.index)
          cellArr.put(if (cell.isStone) 1 else 0)
          cellArr.put(if (cell.hasBomb) 1 else 0)
          rowArr.put(cellArr)
        }
      }
      outer.put(rowArr)
    }
    return outer.toString()
  }

  private fun decodeGrid(json: String): List<List<BubbleCell?>> {
    return try {
      val outer = JSONArray(json)
      val rows = mutableListOf<List<BubbleCell?>>()
      for (r in 0 until outer.length()) {
        val rowArr = outer.getJSONArray(r)
        val cells = MutableList<BubbleCell?>(GRID_COLS) { null }
        for (c in 0 until min(GRID_COLS, rowArr.length())) {
          val item = rowArr.opt(c)
          if (item is JSONArray && item.length() >= 3) {
            cells[c] = BubbleCell(
              color = BubbleColor.fromIndex(item.optInt(0, 0)),
              isStone = item.optInt(1, 0) == 1,
              hasBomb = item.optInt(2, 0) == 1,
            )
          }
        }
        rows.add(cells)
      }
      rows
    } catch (_: Exception) {
      emptyList()
    }
  }
}

sealed interface DailyClaimResult {
  data object AlreadyClaimed : DailyClaimResult
  data class Success(
    val day: Int,
    val item: DailyRewardItem,
    val randomBoosters: List<BoosterType>,
  ) : DailyClaimResult
}

data class VictoryRewardsSummary(
  val coins: Int,
  val bonusBoosters: List<BoosterType>,
  val unlockedNewLevel: Boolean,
)
