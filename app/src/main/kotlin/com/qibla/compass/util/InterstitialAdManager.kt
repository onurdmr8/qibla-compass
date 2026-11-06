package com.qibla.compass.util

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object InterstitialAdConfig {
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3207706111232143/3074595670"
    const val MIN_SECONDS_BETWEEN_ADS = 90L
}

class InterstitialAdManager(private val context: Context) {

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var lastAdShowTime = 0L

    init {
        loadAd()
    }

    fun loadAd() {
        if (isLoading || interstitialAd != null) return
        isLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            InterstitialAdConfig.INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                    setupAdCallbacks()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                }
            }
        )
    }

    private fun setupAdCallbacks() {
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                lastAdShowTime = System.currentTimeMillis()
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
                // Ad is shown
            }
        }
    }

    fun showIfReady(activity: Activity?) {
        val now = System.currentTimeMillis()
        val elapsedSeconds = (now - lastAdShowTime) / 1000

        if (elapsedSeconds >= InterstitialAdConfig.MIN_SECONDS_BETWEEN_ADS) {
            if (activity != null && interstitialAd != null) {
                interstitialAd?.show(activity)
            } else {
                loadAd()
            }
        }
    }
}
