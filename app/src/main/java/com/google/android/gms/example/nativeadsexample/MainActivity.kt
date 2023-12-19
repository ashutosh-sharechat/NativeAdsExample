/*
 * Copyright (C) 2017 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.example.nativeadsexample

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.VideoController
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.example.nativeadsexample.databinding.ActivityMainBinding
import com.google.android.gms.example.nativeadsexample.databinding.AdUnifiedBinding
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "MainActivity"
const val AD_MANAGER_AD_UNIT_ID = "/21872722794/Moj_render_rate_ashutosh"

/** A simple activity class that displays native ad formats. */
class MainActivity : AppCompatActivity() {

    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private lateinit var mainActivityBinding: ActivityMainBinding
    private var currentNativeAdsList: MutableList<NativeAd> = emptyList<NativeAd>().toMutableList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivityBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainActivityBinding.root)

        // Log the Mobile Ads SDK version.
        Log.d(TAG, "Google Mobile Ads SDK Version: " + MobileAds.getVersion())

        initializeMobileAdsSdk()

        mainActivityBinding.refreshButton.setOnClickListener {
            refreshAd()
        }

        mainActivityBinding.destroyAllAds.setOnClickListener {
            destroyAds()
        }

        mainActivityBinding.destroyOneAd.setOnClickListener {
            currentNativeAdsList.firstOrNull { it.mediaContent?.hasVideoContent() == true }?.destroy()
        }

        mainActivityBinding.showAd.setOnClickListener {
            val unifiedAdBinding = AdUnifiedBinding.inflate(layoutInflater)
            val index = mainActivityBinding.enterAdIndex.text.toString().toInt()
            populateNativeAdView(currentNativeAdsList[index], unifiedAdBinding)
            mainActivityBinding.adFrame.removeAllViews()
            mainActivityBinding.adFrame.addView(unifiedAdBinding.root)
        }
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }
        MobileAds.initialize(this) { initializationStatus ->
            refreshAd()
        }
    }

    private fun refreshAd() {

        val videoOptions: VideoOptions = VideoOptions.Builder().apply {
            setStartMuted(false)
            setCustomControlsRequested(true)
            setCustomControlsRequested(true)
        }.build()
        val adOptions: NativeAdOptions = NativeAdOptions.Builder().apply {
            setVideoOptions(videoOptions)
            setMediaAspectRatio(3)
        }.build()

        val adLoader = AdLoader.Builder(this.applicationContext, AD_MANAGER_AD_UNIT_ID)
            .forNativeAd { nativeAd ->
                Log.e(
                    "SampleApp", "${nativeAd.advertiser}, ${nativeAd.mediaContent?.hasVideoContent()}," +
                            " ${nativeAd.mediaContent?.aspectRatio}, ${nativeAd.responseInfo?.responseId}"
                )
                currentNativeAdsList.add(nativeAd)
            }
            .withNativeAdOptions(adOptions)
            .withAdListener(
                object : AdListener() {
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        val error =
                            """"
            domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}
          """
                        Toast.makeText(
                            this@MainActivity,
                            "Failed to load native ad with error $error",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }
            )
            .build()

        adLoader.loadAd(AdManagerAdRequest.Builder().build())
    }

    /**
     * Populates a [NativeAdView] object with data from a given [NativeAd].
     *
     * @param nativeAd the object containing the ad's assets
     * @param unifiedAdBinding the binding object of the layout that has NativeAdView as the root view
     */
    private fun populateNativeAdView(nativeAd: NativeAd, unifiedAdBinding: AdUnifiedBinding) {
        val nativeAdView = unifiedAdBinding.root

        // Set the media view.
        nativeAdView.mediaView = unifiedAdBinding.adMedia

        // Set other ad assets.
        nativeAdView.headlineView = unifiedAdBinding.adHeadline
        nativeAdView.bodyView = unifiedAdBinding.adBody
        nativeAdView.callToActionView = unifiedAdBinding.adCallToAction
        nativeAdView.iconView = unifiedAdBinding.adAppIcon
        nativeAdView.priceView = unifiedAdBinding.adPrice
        nativeAdView.starRatingView = unifiedAdBinding.adStars
        nativeAdView.storeView = unifiedAdBinding.adStore
        nativeAdView.advertiserView = unifiedAdBinding.adAdvertiser

        // The headline and media content are guaranteed to be in every NativeAd.
        unifiedAdBinding.adHeadline.text = nativeAd.headline
        nativeAd.mediaContent?.let { unifiedAdBinding.adMedia.setMediaContent(it) }

        // These assets aren't guaranteed to be in every NativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.body == null) {
            unifiedAdBinding.adBody.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adBody.visibility = View.VISIBLE
            unifiedAdBinding.adBody.text = nativeAd.body
        }

        if (nativeAd.callToAction == null) {
            unifiedAdBinding.adCallToAction.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adCallToAction.visibility = View.VISIBLE
            unifiedAdBinding.adCallToAction.text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            unifiedAdBinding.adAppIcon.visibility = View.GONE
        } else {
            unifiedAdBinding.adAppIcon.setImageDrawable(nativeAd.icon?.drawable)
            unifiedAdBinding.adAppIcon.visibility = View.VISIBLE
        }

        if (nativeAd.price == null) {
            unifiedAdBinding.adPrice.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adPrice.visibility = View.VISIBLE
            unifiedAdBinding.adPrice.text = nativeAd.price
        }

        if (nativeAd.store == null) {
            unifiedAdBinding.adStore.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adStore.visibility = View.VISIBLE
            unifiedAdBinding.adStore.text = nativeAd.store
        }

        if (nativeAd.starRating == null) {
            unifiedAdBinding.adStars.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adStars.rating = nativeAd.starRating!!.toFloat()
            unifiedAdBinding.adStars.visibility = View.VISIBLE
        }

        if (nativeAd.advertiser == null) {
            unifiedAdBinding.adAdvertiser.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adAdvertiser.text = nativeAd.advertiser
            unifiedAdBinding.adAdvertiser.visibility = View.VISIBLE
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        nativeAdView.setNativeAd(nativeAd)

        val mediaContent = nativeAd.mediaContent

        // Updates the UI to say whether or not this ad has a video asset.
        if (mediaContent != null && mediaContent.hasVideoContent()) {
            val videoController = mediaContent.videoController
            mainActivityBinding.videostatusText.text =
                String.format(
                    Locale.getDefault(),
                    "Video status: Ad contains a %.2f:1 video asset.",
                    mediaContent.aspectRatio
                )
            // Create a new VideoLifecycleCallbacks object and pass it to the VideoController. The
            // VideoController will call methods on this object when events occur in the video
            // lifecycle.
            videoController?.videoLifecycleCallbacks =
                object : VideoController.VideoLifecycleCallbacks() {
                    override fun onVideoEnd() {
                        // Publishers should allow native ads to complete video playback before
                        // refreshing or replacing them with another ad in the same UI location.
                        mainActivityBinding.videostatusText.text = "Video status: Video playback has ended."
                        super.onVideoEnd()
                    }
                }
        } else {
            mainActivityBinding.videostatusText.text = "Video status: Ad does not contain a video asset."
        }
    }

    private fun destroyAds() {
        currentNativeAdsList.forEach { it.destroy() }
    }

    override fun onDestroy() {
        destroyAds()
        super.onDestroy()
    }
}
