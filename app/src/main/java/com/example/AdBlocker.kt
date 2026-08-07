package com.example

import android.net.Uri
import android.webkit.WebResourceResponse
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import java.io.ByteArrayInputStream

enum class BlockCategory(val displayName: String) {
    AD_NETWORK("Ad Network / Exchange"),
    ANALYTICS("Analytics & Tracker"),
    SOCIAL_PIXEL("Social Media Pixel"),
    SCRIPT_FINGERPRINT("Script / Fingerprinting")
}

data class BlockedRequestLog(
    val url: String,
    val domain: String,
    val category: BlockCategory,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Ad and Tracker Request Interceptor.
 * Blocks common ad-tracking domains, analytics scripts, social pixels,
 * fingerprinting scripts, and ad network URLs to enhance user privacy
 * and accelerate web page loading speed.
 */
object AdBlocker {

    // Total blocked request counters for Privacy Dashboard
    var blockedCount by mutableIntStateOf(0)
        private set

    var blockedAdsCount by mutableIntStateOf(0)
        private set

    var blockedTrackersCount by mutableIntStateOf(0)
        private set

    var blockedSocialCount by mutableIntStateOf(0)
        private set

    var blockedScriptsCount by mutableIntStateOf(0)
        private set

    // Real-time log of blocked requests
    val blockedLogs = mutableStateListOf<BlockedRequestLog>()

    fun recordBlock(url: String, domain: String, category: BlockCategory) {
        blockedCount++
        when (category) {
            BlockCategory.AD_NETWORK -> blockedAdsCount++
            BlockCategory.ANALYTICS -> blockedTrackersCount++
            BlockCategory.SOCIAL_PIXEL -> blockedSocialCount++
            BlockCategory.SCRIPT_FINGERPRINT -> blockedScriptsCount++
        }
        val log = BlockedRequestLog(
            url = url,
            domain = domain.ifBlank { "unknown domain" },
            category = category
        )
        if (blockedLogs.size >= 100) {
            blockedLogs.removeAt(0)
        }
        blockedLogs.add(log)
    }

    fun resetStats() {
        blockedCount = 0
        blockedAdsCount = 0
        blockedTrackersCount = 0
        blockedSocialCount = 0
        blockedScriptsCount = 0
        blockedLogs.clear()
    }

    // Common Ad Networks, Ad Exchanges, SSPs, and DSPs
    private val adHosts = setOf(
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "amazon-adsystem.com",
        "adnxs.com",
        "adsrvr.org",
        "openx.net",
        "rubiconproject.com",
        "pubmatic.com",
        "criteo.com",
        "taboola.com",
        "outbrain.com",
        "adform.net",
        "casalemedia.com",
        "smartadserver.com",
        "moatads.com",
        "scorecardresearch.com",
        "quantserve.com",
        "popads.net",
        "popcash.net",
        "propellerads.com",
        "adroll.com",
        "media.net",
        "adblade.com",
        "mgid.com",
        "revcontent.com",
        "bidswitch.net",
        "indexww.com",
        "triplelift.com",
        "gumgum.com",
        "teads.tv",
        "yieldmo.com",
        "zemanta.com",
        "sovrn.com",
        "contextweb.com",
        "advertising.com",
        "exoclick.com",
        "juicyads.com",
        "adsterra.com",
        "trafficfactory.biz",
        "a-ads.com"
    )

    // Analytics, User Tracking, and Session Replay Services
    private val trackerHosts = setOf(
        "google-analytics.com",
        "analytics.google.com",
        "googletagmanager.com",
        "googletagservices.com",
        "hotjar.com",
        "mixpanel.com",
        "segment.io",
        "segment.com",
        "amplitude.com",
        "heap.io",
        "crazyegg.com",
        "fullstory.com",
        "mouseflow.com",
        "inspectlet.com",
        "logrocket.com",
        "posthog.com",
        "clarity.ms",
        "omtrdc.net",
        "demdex.net",
        "chartbeat.com",
        "newrelic.com",
        "nr-data.net",
        "bugsnag.com",
        "histats.com",
        "statcounter.com",
        "yandex.ru/metrika",
        "mc.yandex.ru",
        "matomo.cloud",
        "piwik.pro"
    )

    // Social Media Tracking Pixels and Data Collectors
    private val socialTrackers = setOf(
        "connect.facebook.net",
        "pixel.facebook.com",
        "analytics.tiktok.com",
        "tr.snapchat.com",
        "analytics.twitter.com",
        "static.ads-twitter.com",
        "ads-twitter.com",
        "px.ads.linkedin.com",
        "trk.pinterest.com"
    )

    // Specific script filenames or URL path patterns commonly used for ads & trackers
    private val pathPatternKeywords = listOf(
        "gtag/js",
        "analytics.js",
        "fbevents.js",
        "google-analytics.js",
        "pagead2.js",
        "show_ads.js",
        "adframe.js",
        "adloader.js",
        "pop.js",
        "popunder",
        "fingerprint.js",
        "matomo.js",
        "piwik.js",
        "clarity.js",
        "hotjar.js",
        "telemetry.js",
        "beacon.js",
        "pixel.js",
        "/ads/",
        "/adserver/",
        "/banners/",
        "/popunders/",
        "/advertisement/"
    )

    /**
     * Checks whether a request URL should be blocked as an ad or tracker.
     */
    fun isAd(url: String): Boolean {
        if (url.isBlank()) return false
        try {
            val lowerUrl = url.lowercase()
            val uri = Uri.parse(lowerUrl)
            val host = uri.host ?: ""

            // 1. Check Ad Networks & Exchanges
            if (host.isNotEmpty() && adHosts.any { host.endsWith(it) || host.contains(it) }) {
                recordBlock(url, host, BlockCategory.AD_NETWORK)
                return true
            }

            // 2. Check Analytics & Trackers
            if (host.isNotEmpty() && trackerHosts.any { host.endsWith(it) || host.contains(it) }) {
                recordBlock(url, host, BlockCategory.ANALYTICS)
                return true
            }

            // 3. Check Social Tracking Pixels
            if (host.isNotEmpty() && socialTrackers.any { host.endsWith(it) || host.contains(it) }) {
                recordBlock(url, host, BlockCategory.SOCIAL_PIXEL)
                return true
            }

            // 4. Check specific script & URL path patterns
            if (pathPatternKeywords.any { lowerUrl.contains(it) }) {
                recordBlock(url, host, BlockCategory.SCRIPT_FINGERPRINT)
                return true
            }

            return false
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Helper to return an empty WebResourceResponse for intercepted requests.
     */
    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
    }
}
