package com.example

import com.example.data.BoosterType
import com.example.data.BubbleCell
import com.example.data.BubbleColor
import com.example.data.EndReason
import com.example.data.GRID_COLS
import com.example.data.LevelConfig
import com.example.data.LevelGenerator
import com.example.data.LevelObjective
import com.example.data.MAX_LEVELS
import com.example.data.SavedSession
import com.example.game.BubbleEngine
import com.example.game.EngineAudioCallbacks
import com.example.game.EnginePhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  private fun noopCallbacks(
    onEnd: (Boolean, EndReason, Int, Int) -> Unit = { _, _, _, _ -> },
  ) = object : EngineAudioCallbacks {
    override fun onShoot(isFire: Boolean) = Unit
    override fun onBounce() = Unit
    override fun onStick() = Unit
    override fun onPop(index: Int) = Unit
    override fun onFall() = Unit
    override fun onCombo(combo: Int) = Unit
    override fun onBoom() = Unit
    override fun onZap() = Unit
    override fun onRainbow() = Unit
    override fun onFreeze() = Unit
    override fun onRowDrop() = Unit
    override fun onSwap() = Unit
    override fun onVibrate(ms: Long) = Unit
    override fun onBubblesScored(bubblesCleared: Int, combo: Int) = Unit
    override fun onSessionChanged(session: SavedSession) = Unit
    override fun onGameEnded(won: Boolean, reason: EndReason, stars: Int, score: Int) {
      onEnd(won, reason, stars, score)
    }
  }

  @Test
  fun all_100_levels_generate_valid_connected_boards() {
    for (lvl in 1..MAX_LEVELS) {
      val cfg = LevelGenerator.generateLevel(lvl)
      assertEquals(lvl, cfg.levelNumber)
      assertTrue("Level $lvl must have bubbles", cfg.bubbleCount > 0)
      assertTrue("Level $lvl must have positive shots", cfg.shots > 0)
      assertTrue("Level $lvl palette non-empty", cfg.palette.isNotEmpty())
      assertTrue(
        "Level $lvl should have no floating bubbles initially",
        LevelGenerator.findFloatingBubbles(cfg.grid, 0).isEmpty(),
      )
    }
  }

  @Test
  fun daily_challenge_generates_deterministic_levels() {
    val a = LevelGenerator.dailyLevelForDate("2026-11-25")
    val b = LevelGenerator.dailyLevelForDate("2026-11-25")
    assertEquals(a.shots, b.shots)
    assertEquals(a.bubbleCount, b.bubbleCount)
    assertTrue(a.isDaily)
  }

  @Test
  fun floating_bubbles_are_detected_when_disconnected_from_top_row() {
    val grid = listOf(
      listOf(BubbleCell(BubbleColor.RED)) + List(GRID_COLS - 1) { null },
      List(GRID_COLS) { null },
      listOf(null, null, BubbleCell(BubbleColor.BLUE)) + List(GRID_COLS - 3) { null },
    )
    val floating = LevelGenerator.findFloatingBubbles(grid, parity = 0)
    assertEquals(1, floating.size)
    assertEquals(2, floating[0].r)
    assertEquals(2, floating[0].c)
  }

  @Test
  fun shooting_matches_cluster_drops_floating_and_wins_level() {
    val row0 = MutableList<BubbleCell?>(GRID_COLS) { null }.apply {
      this[4] = BubbleCell(BubbleColor.RED)
      this[5] = BubbleCell(BubbleColor.RED)
    }
    val row1 = MutableList<BubbleCell?>(GRID_COLS) { null }.apply {
      this[3] = BubbleCell(BubbleColor.BLUE)
    }
    val cfg = LevelConfig(
      levelNumber = 1,
      grid = listOf(row0, row1),
      objective = LevelObjective.ClearAll,
      shots = 10,
      palette = listOf(BubbleColor.RED),
      dropEvery = 0,
      bubbleCount = 3,
      pattern = "clusters",
      topColor = BubbleColor.RED,
    )

    val deterministicSession = SavedSession(
      levelNumber = 1,
      isDaily = false,
      dateKey = "",
      grid = listOf(row0, row1),
      parity = 0,
      shotsLeft = 10,
      shotsMax = 10,
      score = 0,
      removedCount = 0,
      colorClearedCount = 0,
      combo = 0,
      maxCombo = 0,
      missCount = 0,
      freezeShots = 0,
      currentColor = BubbleColor.RED,
      nextColor = BubbleColor.RED,
      usedBooster = false,
    )

    var wonFlag = false
    var starsEarned = 0
    val engine = BubbleEngine(
      levelConfig = cfg,
      bonusShots = 0,
      restoredSession = deterministicSession,
      callbacks = noopCallbacks { won, _, stars, _ ->
        wonFlag = won
        starsEarned = stars
      },
    )
    engine.updateBounds(360f, 600f)
    engine.aimTowards(engine.cellX(0, 5), engine.cellY(0))
    assertTrue(engine.shoot { false })

    // Simulate frames until projectile resolves
    repeat(120) {
      engine.step(0.016f)
    }

    assertTrue("Expected victory after clearing final cluster", wonFlag)
    assertEquals(EnginePhase.ENDED, engine.phase)
    assertEquals(3, starsEarned)
    assertTrue(engine.score > 0)
  }

  @Test
  fun freeze_booster_prevents_row_drops() {
    val cfg = LevelGenerator.generateLevel(10)
    val engine = BubbleEngine(cfg, 0, null, noopCallbacks())
    engine.updateBounds(360f, 600f)
    assertTrue(engine.activateFreeze())
    assertEquals(5, engine.freezeShots)
    assertFalse("Cannot double freeze while active", engine.activateFreeze())
  }

  @Test
  fun bomb_booster_clears_nearby_bubbles() {
    val row0 = MutableList<BubbleCell?>(GRID_COLS) { BubbleCell(BubbleColor.GREEN) }
    val row1 = MutableList<BubbleCell?>(GRID_COLS) { BubbleCell(BubbleColor.BLUE) }
    val cfg = LevelConfig(
      levelNumber = 5,
      grid = listOf(row0, row1),
      objective = LevelObjective.ReachScore(5000),
      shots = 10,
      palette = listOf(BubbleColor.GREEN, BubbleColor.BLUE),
      dropEvery = 0,
      bubbleCount = 20,
      pattern = "clusters",
      topColor = BubbleColor.GREEN,
    )
    val engine = BubbleEngine(cfg, 0, null, noopCallbacks())
    engine.updateBounds(360f, 600f)
    engine.armedBooster = BoosterType.BOMB
    engine.aimTowards(engine.cellX(1, 5), engine.cellY(1))
    assertTrue(engine.shoot { true })
    repeat(120) { engine.step(0.016f) }
    assertTrue("Bomb should reduce bubble count", engine.remainingBubbleCount() < 20)
    assertNotNull(engine.exportSession())
  }
}
