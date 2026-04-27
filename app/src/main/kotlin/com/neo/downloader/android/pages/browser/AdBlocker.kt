package com.neo.downloader.android.pages.browser

import java.util.Locale

class AdBlocker {
    fun shouldBlock(
        requestUrl: String,
        pageUrl: String?,
        isMainFrame: Boolean,
        dynamicHosts: Set<String> = emptySet(),
    ): Boolean {
        if (isMainFrame) return false
        val requestHost = requestUrl.toHost() ?: return false
        val requestHostNoWww = requestHost.removePrefix("www.")
        val pageHostNoWww = pageUrl?.toHost()?.removePrefix("www.")

        // Never block first-party resources; this prevents page/video/image load deadlocks.
        if (pageHostNoWww != null && pageHostNoWww == requestHostNoWww) {
            return false
        }

        if (hostInSetOrParent(requestHost, alwaysBlockedHosts)) {
            return true
        }

        // Apply downloaded host lists only when we can confirm third-party context.
        if (pageHostNoWww != null && hostInSetOrParent(requestHost, dynamicHosts)) {
            return true
        }

        val isThirdParty = pageHostNoWww != null && pageHostNoWww != requestHostNoWww
        val normalizedUrl = requestUrl.lowercase(Locale.US)
        return isThirdParty && adUrlMarkers.any { marker -> marker in normalizedUrl }
    }

    private fun String.toHost(): String? {
        return runCatching { java.net.URI(this).host?.lowercase(Locale.US) }.getOrNull()
    }

    private fun hostInSetOrParent(host: String, hostSet: Set<String>): Boolean {
        if (hostSet.isEmpty()) return false
        var candidate = host
        while (true) {
            if (hostSet.contains(candidate)) return true
            val nextDot = candidate.indexOf('.')
            if (nextDot < 0 || nextDot == candidate.lastIndex) return false
            candidate = candidate.substring(nextDot + 1)
        }
    }

    companion object {
        private val alwaysBlockedHosts = setOf(
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "adservice.google.com",
            "adservice.google.co.in",
            "adsystem.amazon.com",
            "adnxs.com",
            "criteo.com",
            "taboola.com",
            "outbrain.com",
            "scorecardresearch.com",
            "googletagservices.com",
            "facebook.net",
            "appsflyer.com",
            "adjust.com",
        )

        private val adUrlMarkers = listOf(
            "/ads/",
            "adservice",
            "doubleclick",
            "googlesyndication",
            "googletagmanager",
            "google-analytics",
            "analytics",
            "tracking",
            "tracker",
            "pixel",
            "beacon",
        )
    }
}
