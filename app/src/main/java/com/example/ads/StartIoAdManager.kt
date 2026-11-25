package com.example.ads

import android.app.Activity
import android.content.Context
import com.example.data.AdPlacement
import com.example.data.STARTIO_APP_ID
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
import com.startapp.sdk.adsbase.adlisteners.VideoListener
import java.lang.ref.WeakReference

enum class AdLoadState {
  IDLE,
  LOADING,
  READY,
  SHOWING,
  FAILED,
}

class StartIoAdManager(private val appContext: Context) {

  private var activityRef: WeakReference<Activity>? = null
  private var rewardedAd: StartAppAd? = null

  @Volatile
  var sdkInitialized: Boolean = false
    private set

  @Volatile
  var loadState: AdLoadState = AdLoadState.IDLE
    private set

  @Volatile
  var lastStatusMessage: String = "Start.io Native SDK ($STARTIO_APP_ID) ready"
    private set

  fun attachActivity(activity: Activity) {
    activityRef = WeakReference(activity)
    initializeIfNeeded()
    preloadRewarded()
  }

  fun initializeIfNeeded() {
    if (sdkInitialized) return
    try {
      // Passing returnAdsEnabled = false ensures no unrequested return/open ads interrupt players.
      StartAppSDK.init(appContext, STARTIO_APP_ID, false)
      StartAppAd.disableSplash()
      sdkInitialized = true
      lastStatusMessage = "Start.io Native SDK active (App ID: $STARTIO_APP_ID)"
    } catch (t: Throwable) {
      sdkInitialized = false
      lastStatusMessage = "Start.io SDK init error: ${t.message ?: "unavailable"}"
    }
  }

  fun preloadRewarded() {
    val activity = activityRef?.get() ?: return
    if (!sdkInitialized || loadState == AdLoadState.LOADING || loadState == AdLoadState.SHOWING) return

    loadState = AdLoadState.LOADING
    try {
      val ad = rewardedAd ?: StartAppAd(activity).also { rewardedAd = it }
      ad.loadAd(
        StartAppAd.AdMode.REWARDED_VIDEO,
        object : AdEventListener {
          override fun onReceiveAd(ad: Ad) {
            loadState = AdLoadState.READY
            lastStatusMessage = "Start.io Rewarded Video ready (App ID: $STARTIO_APP_ID)"
          }

          override fun onFailedToReceiveAd(ad: Ad?) {
            loadState = AdLoadState.FAILED
            lastStatusMessage = "Rewarded video unavailable right now (${ad?.errorMessage ?: "no fill"})"
          }
        },
      )
    } catch (t: Throwable) {
      loadState = AdLoadState.FAILED
      lastStatusMessage = "Rewarded video load failed: ${t.message ?: "error"}"
    }
  }

  fun showRewardedAd(
    placement: AdPlacement,
    onRewardEarned: () -> Unit,
    onFailedOrCancelled: (String) -> Unit,
  ) {
    val activity = activityRef?.get()
    if (activity == null || activity.isFinishing || activity.isDestroyed) {
      onFailedOrCancelled("Unable to open ad outside active screen")
      return
    }
    initializeIfNeeded()
    if (!sdkInitialized) {
      onFailedOrCancelled("Start.io SDK unavailable – no reward granted")
      return
    }
    if (loadState == AdLoadState.SHOWING) {
      onFailedOrCancelled("An ad is already playing")
      return
    }

    val ad = rewardedAd ?: StartAppAd(activity).also { rewardedAd = it }
    if (loadState == AdLoadState.READY) {
      displayPreparedAd(ad, placement, onRewardEarned, onFailedOrCancelled)
      return
    }

    // Attempt an immediate load-then-show for this user-initiated rewarded placement.
    loadState = AdLoadState.LOADING
    try {
      ad.loadAd(
        StartAppAd.AdMode.REWARDED_VIDEO,
        object : AdEventListener {
          override fun onReceiveAd(receivedAd: Ad) {
            loadState = AdLoadState.READY
            displayPreparedAd(ad, placement, onRewardEarned, onFailedOrCancelled)
          }

          override fun onFailedToReceiveAd(failedAd: Ad?) {
            loadState = AdLoadState.FAILED
            val reason = failedAd?.errorMessage?.takeIf { it.isNotBlank() } ?: "No rewarded ad available right now"
            lastStatusMessage = "Start.io (${placement.key}): $reason"
            onFailedOrCancelled("$reason – no reward granted")
          }
        },
      )
    } catch (t: Throwable) {
      loadState = AdLoadState.FAILED
      onFailedOrCancelled("Ad failed to load – no reward granted")
    }
  }

  private fun displayPreparedAd(
    ad: StartAppAd,
    placement: AdPlacement,
    onRewardEarned: () -> Unit,
    onFailedOrCancelled: (String) -> Unit,
  ) {
    var videoCompleted = false
    var rewardDispatched = false
    var settled = false

    fun dispatchOnceCompleted() {
      if (videoCompleted && !rewardDispatched) {
        rewardDispatched = true
        settled = true
        onRewardEarned()
      }
    }

    loadState = AdLoadState.SHOWING
    ad.setVideoListener(
      VideoListener {
        videoCompleted = true
        dispatchOnceCompleted()
      },
    )

    val displayed = try {
      ad.showAd(
        object : AdDisplayListener {
          override fun adHidden(hiddenAd: Ad) {
            loadState = AdLoadState.IDLE
            if (videoCompleted) {
              dispatchOnceCompleted()
            } else if (!settled) {
              settled = true
              onFailedOrCancelled("Ad closed before completion – no reward granted")
            }
            rewardedAd = null
            preloadRewarded()
          }

          override fun adDisplayed(displayedAd: Ad) {
            lastStatusMessage = "Showing Start.io Rewarded (${placement.label})"
          }

          override fun adClicked(clickedAd: Ad) = Unit

          override fun adNotDisplayed(notDisplayedAd: Ad) {
            loadState = AdLoadState.FAILED
            if (!settled) {
              settled = true
              onFailedOrCancelled("Ad could not be displayed – no reward granted")
            }
            rewardedAd = null
            preloadRewarded()
          }
        },
      )
    } catch (_: Throwable) {
      false
    }

    if (!displayed && !settled) {
      loadState = AdLoadState.FAILED
      settled = true
      onFailedOrCancelled("Ad failed to start – no reward granted")
      rewardedAd = null
      preloadRewarded()
    }
  }
}
