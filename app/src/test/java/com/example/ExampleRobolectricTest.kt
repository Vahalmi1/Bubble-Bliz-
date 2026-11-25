package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.BoosterType
import com.example.data.DailyClaimResult
import com.example.data.GameRepository
import com.example.data.local.AppDatabase
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.GameUiState
import com.example.ui.theme.BubbleBlitzTheme
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeRule = createComposeRule()

  private lateinit var db: AppDatabase
  private lateinit var repository: GameRepository

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = GameRepository(db.gameDao())
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun app_name_is_bubble_blitz() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("Bubble Blitz", context.getString(R.string.app_name))
  }

  @Test
  fun room_repository_persists_daily_rewards_and_blocks_duplicate_claims() = runTest {
    val initial = repository.ensureInitialized()
    assertEquals(300, initial.coins)
    assertEquals(1, initial.unlockedLevel)

    val firstClaim = repository.claimDailyReward()
    assertTrue(firstClaim is DailyClaimResult.Success)
    val secondClaim = repository.claimDailyReward()
    assertTrue(secondClaim is DailyClaimResult.AlreadyClaimed)

    val afterDaily = repository.getSnapshot()
    assertEquals(400, afterDaily.coins) // Day 1 grants +100 coins
    assertEquals(1, afterDaily.dailyStreakDay)
  }

  @Test
  fun room_repository_persists_shop_purchases_and_level_progression() = runTest {
    repository.ensureInitialized()
    assertTrue(repository.buyBooster(BoosterType.BOMB))
    assertFalse("Should block purchase when coins insufficient", repository.buyBooster(BoosterType.FIRE))

    val victory = repository.recordGameFinished(
      levelNumber = 1,
      isDaily = false,
      won = true,
      score = 450,
      starsEarned = 3,
    )
    assertTrue(victory != null && victory.unlockedNewLevel)

    val reloaded = repository.getSnapshot()
    assertEquals(2, reloaded.unlockedLevel)
    assertEquals(3, reloaded.stars[1])
    assertEquals(4, reloaded.inventory.bomb)
    assertEquals(450, reloaded.highScore)
  }

  @Test
  fun splash_and_home_screens_render_native_compose_controls() {
    var clickedPlay = false
    composeRule.setContent {
      BubbleBlitzTheme {
        SplashScreen(onPlayClick = { clickedPlay = true })
      }
    }
    composeRule.onNodeWithTag("splash_play_button").assertIsDisplayed().performClick()
    assertTrue(clickedPlay)
  }

  @Test
  fun home_screen_displays_continue_levels_and_shop_actions() {
    var continued = false
    composeRule.setContent {
      BubbleBlitzTheme {
        HomeScreen(
          uiState = GameUiState(todayKey = "2026-11-25"),
          onContinueClick = { continued = true },
          onNavigate = {},
          onOpenModal = {},
        )
      }
    }
    composeRule.onNodeWithTag("home_continue_button").assertIsDisplayed().performClick()
    composeRule.onNodeWithTag("home_levels_row").assertExists()
    composeRule.onNodeWithTag("home_shop_button").assertExists()
    assertTrue(continued)
  }
}
