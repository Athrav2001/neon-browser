package com.neo.downloader.android.pages.browser

import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class AdBlocker {
    private val blockedHostsCache = ConcurrentHashMap<String, Boolean>()
    
    fun shouldBlock(
        requestUrl: String,
        pageUrl: String?,
        isMainFrame: Boolean,
        dynamicHosts: Set<String> = emptySet(),
    ): Boolean {
        if (isMainFrame) return false
        
        // Skip non-ad file types for speed
        if (isLikelyNonAdResource(requestUrl)) {
            return false
        }
        
        val requestHost = requestUrl.toHost() ?: return false
        val requestHostNoWww = requestHost.removePrefix("www.")
        val pageHostNoWww = pageUrl?.toHost()?.removePrefix("www.")
        
        // Check cache first
        blockedHostsCache[requestHostNoWww]?.let { return it }
        
        // Never block first-party resources; this prevents page/video/image load deadlocks.
        if (pageHostNoWww != null && pageHostNoWww == requestHostNoWww) {
            blockedHostsCache[requestHostNoWww] = false
            return false
        }
        
        if (hostInSetOrParent(requestHost, alwaysBlockedHosts)) {
            blockedHostsCache[requestHostNoWww] = true
            return true
        }
        
        // Apply downloaded host lists only when we can confirm third-party context.
        if (pageHostNoWww != null && hostInSetOrParent(requestHost, dynamicHosts)) {
            blockedHostsCache[requestHostNoWww] = true
            return true
        }
        
        val isThirdParty = pageHostNoWww != null && pageHostNoWww != requestHostNoWww
        val normalizedUrl = requestUrl.lowercase(Locale.US)
        val result = isThirdParty && adUrlMarkers.any { marker -> marker in normalizedUrl }
        blockedHostsCache[requestHostNoWww] = result
        return result
    }
    
    private fun isLikelyNonAdResource(url: String): Boolean {
        val lower = url.lowercase(Locale.US)
        return lower.endsWith(".png") || lower.endsWith(".jpg") || 
               lower.endsWith(".jpeg") || lower.endsWith(".gif") || 
               lower.endsWith(".webp") || lower.endsWith(".css") || 
               lower.endsWith(".js") || lower.endsWith(".woff") || 
               lower.endsWith(".woff2") || lower.endsWith(".ttf") || 
               lower.endsWith(".ico")
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
