package com.neo.downloader.android.pages.browser

import android.webkit.CookieManager
import com.neo.downloader.android.ui.widget.WebViewState
import com.neo.downloader.shared.pages.adddownload.AddDownloadCredentialsInUiProps
import ir.amirab.downloader.downloaditem.http.HttpDownloadCredentials
import ir.amirab.util.HttpUrlUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

typealias NDMWebRequestId = String

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
) : RequestInterceptor {
    private val requests = mutableMapOf<String, NDMWebRequest>()
    private val recentlyHandledUrls = mutableMapOf<String, Long>()

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
                AddDownloadCredentialsInUiProps(
                    HttpDownloadCredentials(
                        link = webRequest.url,
                        headers = webRequest.headers,
                        downloadPage = webRequest.page,
                    ),
                    AddDownloadCredentialsInUiProps.Configs()
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
        val downloads = urls
            .filter(::isUsefulDownloadUrl)
            .map { url ->
                getWebRequestOrDefault(
                    url = url,
                    userAgent = userAgent,
                    page = page,
                    webViewState = tab.tabState,
                )
            }
            .filter { markHandled(it.url) }
            .map { webRequest ->
                AddDownloadCredentialsInUiProps(
                    HttpDownloadCredentials(
                        link = webRequest.url,
                        headers = webRequest.headers,
                        downloadPage = webRequest.page,
                    ),
                    AddDownloadCredentialsInUiProps.Configs()
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

        // keep HLS/DASH playlists even if URL is non-standard.
        if (path.contains(".m3u8") || path.contains(".mpd")) {
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

    companion object {
        private const val REMOVE_REQUESTS_DELAY = 20_000L
        private const val HANDLED_COOLDOWN_MILLIS = 12_000L
        private val USEFUL_EXTENSIONS = setOf(
            "mp4", "mkv", "avi", "mov", "wmv", "webm",
            "mp3", "m4a", "wav", "flac", "aac", "ogg",
            "pdf", "epub", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "zip", "rar", "7z", "tar", "gz", "bz2",
            "apk", "xapk", "apkm",
            "m3u8", "mpd"
        )
        private val BLOCKED_EXTENSIONS = setOf(
            "tmp", "temp", "bin", "log",
            "jpg", "jpeg", "png", "gif", "webp", "svg", "ico",
            "css", "js", "map", "txt", "json", "xml",
            "html", "htm", "php", "asp", "aspx"
        )
    }
}
