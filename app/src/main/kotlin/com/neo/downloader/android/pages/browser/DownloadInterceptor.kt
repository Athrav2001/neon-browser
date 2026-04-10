package com.neo.downloader.android.pages.browser

import android.webkit.CookieManager
import com.neo.downloader.android.ui.widget.WebViewState
import com.neo.downloader.shared.pages.adddownload.AddDownloadCredentialsInUiProps
import ir.amirab.downloader.downloaditem.IDownloadCredentials
import ir.amirab.downloader.downloaditem.hls.HLSDownloadCredentials
import ir.amirab.downloader.downloaditem.http.HttpDownloadCredentials
import ir.amirab.util.HttpUrlUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt

typealias NDMWebRequestId = String

data class GrabberDetectedItem(
    val url: String,
    val name: String,
    val size: String,
    val quality: String,
    val isStream: Boolean,
)

data class NDMWebRequest(
    val url: String,
    val headers: Map<String, String>,
    val page: String?,
) {
    val id: NDMWebRequestId = url
}

interface RequestInterceptor {
    fun interceptRequest(request: NDMWebRequest)
}

class DownloadInterceptor(
    private val scope: CoroutineScope,
    private val onNewDownload: (newDownloads: List<AddDownloadCredentialsInUiProps>) -> Unit,
    private val onDetectedListUpdated: (tabId: NDMBrowserTabId, items: List<GrabberDetectedItem>) -> Unit = { _, _ -> },
) : RequestInterceptor {
    private val requests = mutableMapOf<String, NDMWebRequest>()
    private val recentlyHandledUrls = mutableMapOf<String, Long>()
    private val detectedByTab = mutableMapOf<NDMBrowserTabId, MutableMap<String, GrabberDetectedItem>>()
    private val detectedRequestsByTab = mutableMapOf<NDMBrowserTabId, MutableMap<String, NDMWebRequest>>()
    private val runningM3u8Expansion = mutableSetOf<String>()

    fun onDownloadStart(
        url: String?,
        userAgent: String?,
        page: String?,
        tab: NDMBrowserTab,
    ) {
        if (url == null) {
            return
        }
        if (!HttpUrlUtils.isValidUrl(url)) {
            return
        }
        if (!isUsefulDownloadUrl(url)) {
            return
        }
        val webRequest = getWebRequestOrDefault(
            url = url,
            userAgent = userAgent,
            page = page,
            webViewState = tab.tabState,
        )
        if (!markHandled(webRequest.url)) {
            return
        }
        onNewDownload(
            listOf(
                createAddDownloadProps(
                    webRequest = webRequest,
                    pageTitle = tab.tabState.pageTitle,
                )
            )
        )
    }

    fun onDetectedLinks(
        urls: List<String>,
        userAgent: String?,
        page: String?,
        tab: NDMBrowserTab,
    ) {
        val requests = urls
            .filter(::isUsefulDownloadUrl)
            .map { url ->
                getWebRequestOrDefault(
                    url = url,
                    userAgent = userAgent,
                    page = page,
                    webViewState = tab.tabState,
                )
            }
        val items = requests.map { webRequest ->
                GrabberDetectedItem(
                    url = webRequest.url,
                    name = extractName(webRequest.url),
                    size = extractPossibleSizeFromUrl(webRequest.url),
                    quality = extractQuality(webRequest.url),
                    isStream = isStreamUrl(webRequest.url),
                )
            }

        if (items.isEmpty()) return
        mergeDetectedItems(tab.tabId, items)
        mergeDetectedRequests(tab.tabId, requests)

        val streamRequests = requests.filter { isStreamUrl(it.url) }
        streamRequests.forEach { request ->
            requestExpandM3u8Variants(tab.tabId, request)
        }
    }

    fun getDetectedItems(tabId: NDMBrowserTabId): List<GrabberDetectedItem> {
        return detectedByTab[tabId]
            ?.values
            ?.sortedBy { it.name.lowercase(Locale.US) }
            .orEmpty()
    }

    fun clearDetectedItems(tabId: NDMBrowserTabId) {
        if (detectedByTab.remove(tabId) != null) {
            onDetectedListUpdated(tabId, emptyList())
        }
        detectedRequestsByTab.remove(tabId)
    }

    fun triggerDownloadsByUrls(
        urls: List<String>,
        userAgent: String?,
        page: String?,
        tab: NDMBrowserTab,
    ) {
        val downloads = urls
            .distinct()
            .filter(::isUsefulDownloadUrl)
            .map { url ->
                getDetectedRequest(tab.tabId, url) ?: getWebRequestOrDefault(
                    url = url,
                    userAgent = userAgent,
                    page = page,
                    webViewState = tab.tabState,
                )
            }
            .map { webRequest ->
                createAddDownloadProps(
                    webRequest = webRequest,
                    pageTitle = tab.tabState.pageTitle,
                )
            }

        if (downloads.isNotEmpty()) {
            onNewDownload(downloads)
        }
    }

    override fun interceptRequest(
        request: NDMWebRequest,
    ) {
        addToHeaders(request)
    }

    private fun addToHeaders(request: NDMWebRequest) {
        requests[request.id] = request
        scope.launch {
            delay(REMOVE_REQUESTS_DELAY)
            requests.remove(request.id)
        }
    }

    private fun getWebRequestOrDefault(
        url: String,
        userAgent: String?,
        page: String?,
        webViewState: WebViewState,
    ): NDMWebRequest {
        var request = requests[url]
        if (request == null) {
            request = NDMWebRequest(
                url = url,
                headers = emptyMap(),
                page = getPageUrl(webViewState) ?: page,
            )
        }
        return request
            .withUserAgent(userAgent)
            .withCookieManagerCookies()
    }

    private fun NDMWebRequest.withUserAgent(userAgent: String?): NDMWebRequest {
        val request = this
        if (userAgent == null) {
            return request
        }
        val userAgentKey = "User-Agent"
        if (request.headers.containsKey(userAgentKey)) {
            return request
        }
        return request.copy(
            headers = request.headers.plus(
                userAgentKey to userAgent
            )
        )
    }

    private fun NDMWebRequest.withCookieManagerCookies(): NDMWebRequest {
        val request = this
        val cookieFromCookieManager =
            CookieManager.getInstance().getCookie(url)?.takeIf { it.isNotBlank() } ?: return request
        val cookieKey = "Cookie"
        val currentCookie = request.headers[cookieKey]?.takeIf { it.isNotBlank() }
        return request.copy(
            headers = request.headers.plus(
                cookieKey to if (currentCookie != null) {
                    "$currentCookie; $cookieFromCookieManager"
                } else {
                    cookieFromCookieManager
                }
            )
        )
    }

    private fun getPageUrl(state: WebViewState): String? {
        return state.lastLoadedUrl
    }

    private fun markHandled(url: String): Boolean {
        val now = System.currentTimeMillis()
        val previous = recentlyHandledUrls[url]
        if (previous != null && now - previous < HANDLED_COOLDOWN_MILLIS) {
            return false
        }
        recentlyHandledUrls[url] = now
        scope.launch {
            delay(HANDLED_COOLDOWN_MILLIS)
            recentlyHandledUrls.remove(url)
        }
        return true
    }

    private fun isUsefulDownloadUrl(url: String): Boolean {
        if (!HttpUrlUtils.isValidUrl(url)) {
            return false
        }
        val path = url
            .substringBefore('#')
            .substringBefore('?')
            .lowercase(Locale.US)
        val fileName = path.substringAfterLast('/', "")
        val ext = fileName.substringAfterLast('.', "")

        // keep HLS playlists even if URL is non-standard.
        if (path.contains(".m3u8")) {
            return true
        }
        if (ext.isBlank()) {
            return false
        }
        if (ext in BLOCKED_EXTENSIONS) {
            return false
        }
        return ext in USEFUL_EXTENSIONS
    }

    private fun isStreamUrl(url: String): Boolean {
        val lower = url.lowercase(Locale.US)
        return lower.contains(".m3u8")
    }

    private fun requestExpandM3u8Variants(tabId: NDMBrowserTabId, request: NDMWebRequest) {
        if (!request.url.lowercase(Locale.US).contains(".m3u8")) return
        val key = "$tabId|${request.url}"
        if (!runningM3u8Expansion.add(key)) return
        scope.launch(Dispatchers.IO) {
            try {
                val variants = fetchM3u8Variants(request)
                if (variants.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        val inheritedHeaders = buildVariantHeaders(request)
                        val variantRequests = mutableListOf<NDMWebRequest>()
                        variants.forEach { variant ->
                            val variantRequest = NDMWebRequest(
                                url = variant.url,
                                headers = inheritedHeaders,
                                page = request.page,
                            )
                            variantRequests += variantRequest
                            addToHeaders(variantRequest)
                        }
                        mergeDetectedRequests(tabId, variantRequests)
                        mergeDetectedItems(tabId, variants)
                    }
                }
            } catch (_: Exception) {
                // ignored on purpose; invalid/blocked playlists are common on websites.
            } finally {
                withContext(Dispatchers.Main) {
                    runningM3u8Expansion.remove(key)
                }
            }
        }
    }

    private fun fetchM3u8Variants(request: NDMWebRequest): List<GrabberDetectedItem> {
        val connection = URL(request.url).openConnection()
        request.headers.forEach { (k, v) ->
            connection.setRequestProperty(k, v)
        }
        val body = connection.getInputStream().bufferedReader().use { it.readText() }
        return parseM3u8Variants(
            manifest = body,
            baseUrl = request.url,
        )
    }

    private fun buildVariantHeaders(request: NDMWebRequest): Map<String, String> {
        val headers = request.headers.toMutableMap()
        val referer = request.page ?: request.url
        headers.putIfAbsent("Referer", referer)
        val origin = runCatching {
            val u = URL(referer)
            "${u.protocol}://${u.host}" + if (u.port > 0 && u.port != u.defaultPort) ":${u.port}" else ""
        }.getOrNull()
        if (origin != null) {
            headers.putIfAbsent("Origin", origin)
        }
        return headers.toMap()
    }

    private fun parseM3u8Variants(
        manifest: String,
        baseUrl: String,
    ): List<GrabberDetectedItem> {
        val lines = manifest
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        if (lines.none { it.startsWith("#EXT-X-STREAM-INF", ignoreCase = true) }) {
            return emptyList()
        }
        val output = mutableListOf<GrabberDetectedItem>()
        var pendingQuality: String? = null
        for (line in lines) {
            if (line.startsWith("#EXT-X-STREAM-INF", ignoreCase = true)) {
                pendingQuality = extractQualityFromStreamInf(line)
                continue
            }
            if (line.startsWith("#")) continue
            if (pendingQuality != null) {
                val resolvedUrl = runCatching { URL(URL(baseUrl), line).toString() }.getOrNull() ?: continue
                output += GrabberDetectedItem(
                    url = resolvedUrl,
                    name = extractName(baseUrl),
                    size = "Unknown",
                    quality = pendingQuality,
                    isStream = true,
                )
                pendingQuality = null
            }
        }
        return output
    }

    private fun extractQualityFromStreamInf(streamInfLine: String): String {
        val resolutionMatch = Regex("""RESOLUTION=(\d+)x(\d+)""", RegexOption.IGNORE_CASE)
            .find(streamInfLine)
        if (resolutionMatch != null) {
            val y = resolutionMatch.groupValues[2]
            return "${y}p"
        }
        val bandwidthMatch = Regex("""BANDWIDTH=(\d+)""", RegexOption.IGNORE_CASE)
            .find(streamInfLine)
        if (bandwidthMatch != null) {
            val bps = bandwidthMatch.groupValues[1].toLongOrNull() ?: return "Unknown"
            val kbps = (bps / 1000).coerceAtLeast(1)
            return "${kbps}kbps"
        }
        return "Unknown"
    }

    private fun extractName(url: String): String {
        val noQuery = url.substringBefore('#').substringBefore('?')
        val fileName = noQuery.substringAfterLast('/', "")
        return if (fileName.isBlank()) "Unknown" else fileName
    }

    private fun extractQuality(url: String): String {
        return QUALITY_REGEX.find(url)?.value ?: "Unknown"
    }

    private fun extractPossibleSizeFromUrl(url: String): String {
        val match = SIZE_REGEX.find(url.lowercase(Locale.US)) ?: return "Unknown"
        val number = match.groupValues[1].toDoubleOrNull() ?: return "Unknown"
        val unit = match.groupValues[2].uppercase(Locale.US)
        return if (number % 1.0 == 0.0) {
            "${number.toInt()} $unit"
        } else {
            "${(number * 10).roundToInt() / 10.0} $unit"
        }
    }

    private fun mergeDetectedItems(tabId: NDMBrowserTabId, items: List<GrabberDetectedItem>) {
        val tabMap = detectedByTab.getOrPut(tabId) { linkedMapOf() }
        var changed = false
        items.forEach { item ->
            if (tabMap[item.url] != item) {
                tabMap[item.url] = item
                changed = true
            }
        }
        if (changed) {
            onDetectedListUpdated(tabId, tabMap.values.toList())
        }
    }

    private fun mergeDetectedRequests(tabId: NDMBrowserTabId, requests: List<NDMWebRequest>) {
        val tabMap = detectedRequestsByTab.getOrPut(tabId) { linkedMapOf() }
        requests.forEach { request ->
            tabMap[request.url] = request
        }
    }

    private fun getDetectedRequest(tabId: NDMBrowserTabId, url: String): NDMWebRequest? {
        return detectedRequestsByTab[tabId]?.get(url)
    }

    private fun createCredentialsFor(webRequest: NDMWebRequest): IDownloadCredentials {
        val effectiveHeaders = buildDownloadHeaders(webRequest)
        return if (webRequest.url.lowercase(Locale.US).contains(".m3u8")) {
            HLSDownloadCredentials(
                link = webRequest.url,
                headers = effectiveHeaders,
                downloadPage = webRequest.page,
            )
        } else {
            HttpDownloadCredentials(
                link = webRequest.url,
                headers = effectiveHeaders,
                downloadPage = webRequest.page,
            )
        }
    }

    private fun createAddDownloadProps(
        webRequest: NDMWebRequest,
        pageTitle: String?,
    ): AddDownloadCredentialsInUiProps {
        return AddDownloadCredentialsInUiProps(
            credentials = createCredentialsFor(webRequest),
            extraConfig = AddDownloadCredentialsInUiProps.Configs(
                suggestedName = buildSuggestedName(
                    url = webRequest.url,
                    pageTitle = pageTitle,
                )
            )
        )
    }

    private fun buildSuggestedName(
        url: String,
        pageTitle: String?,
    ): String? {
        val title = pageTitle?.trim().orEmpty().takeIf { it.isNotBlank() } ?: return null
        val quality = extractQuality(url).takeIf { it != "Unknown" }
        val base = if (quality != null && !title.contains(quality, ignoreCase = true)) {
            "$title $quality"
        } else {
            title
        }

        val extFromUrl = url
            .substringBefore('#')
            .substringBefore('?')
            .substringAfterLast('/', "")
            .substringAfterLast('.', "")
            .lowercase(Locale.US)
            .takeIf { it.isNotBlank() }
        val outputExt = when {
            url.lowercase(Locale.US).contains(".m3u8") -> "mp4"
            extFromUrl == "m3u8" -> "mp4"
            extFromUrl != null && extFromUrl in USEFUL_EXTENSIONS -> extFromUrl
            else -> null
        }

        return if (outputExt == null) {
            base
        } else if (base.lowercase(Locale.US).endsWith(".$outputExt")) {
            base
        } else {
            "$base.$outputExt"
        }
    }

    private fun buildDownloadHeaders(request: NDMWebRequest): Map<String, String> {
        val headers = request.headers.toMutableMap()
        val referer = request.page ?: request.url
        headers.putIfAbsent("Referer", referer)
        val origin = runCatching {
            val u = URL(referer)
            "${u.protocol}://${u.host}" + if (u.port > 0 && u.port != u.defaultPort) ":${u.port}" else ""
        }.getOrNull()
        if (origin != null) {
            headers.putIfAbsent("Origin", origin)
        }
        return headers.toMap()
    }

    companion object {
        private const val REMOVE_REQUESTS_DELAY = 20_000L
        private const val HANDLED_COOLDOWN_MILLIS = 12_000L
        private val QUALITY_REGEX = Regex("""(?i)\b(144p|240p|360p|480p|720p|1080p|1440p|2160p|4k)\b""")
        private val SIZE_REGEX = Regex("""(\d+(?:\.\d+)?)\s*(kb|mb|gb|tb)""")
        private val USEFUL_EXTENSIONS = setOf(
            "mp4", "mkv", "avi", "mov", "wmv", "webm",
            "mp3", "m4a", "wav", "flac", "aac", "ogg",
            "pdf", "epub", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "zip", "rar", "7z", "tar", "gz", "bz2",
            "apk", "xapk", "apkm",
            "m3u8"
        )
        private val BLOCKED_EXTENSIONS = setOf(
            "tmp", "temp", "bin", "log",
            "jpg", "jpeg", "png", "gif", "webp", "svg", "ico",
            "css", "js", "map", "txt", "json", "xml",
            "html", "htm", "php", "asp", "aspx"
        )
    }
}
