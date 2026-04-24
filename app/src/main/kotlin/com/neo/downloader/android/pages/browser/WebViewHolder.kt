package com.neo.downloader.android.pages.browser

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Message
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import com.neo.downloader.android.ui.widget.AccompanistWebChromeClient
import com.neo.downloader.android.ui.widget.AccompanistWebViewClient
import com.neo.downloader.android.ui.widget.WebContent
import com.neo.downloader.android.ui.widget.WebViewNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONTokener
import java.io.ByteArrayInputStream
import java.util.UUID

class WebViewRegistry(
    private val scope: CoroutineScope,
    private val browserComponent: BrowserComponent,
) : WebViewFactory {
    val viewHolders = mutableMapOf<NDMBrowserTabId, WebViewHolder>()
    fun onTabsUpdated(
        webViewStates: NDMTabs,
    ) {
        val webViewStateIds = webViewStates.tabs.map { it.tabId }.toSet()
        for (viewHolderKey in viewHolders.keys.toList()) {
            if (viewHolderKey !in webViewStateIds) {
                removeViewHolder(viewHolderKey)
            }
        }
    }

    fun getWebViewHolder(
        tab: NDMBrowserTab
    ): WebViewHolder {
        return viewHolders.getOrPut(tab.tabId, {
            WebViewHolder(
                tab = tab,
                navigator = WebViewNavigator(scope),
                webView = null,
                client = NDMWebViewClient(
                    requestInterceptor = browserComponent.downloadInterceptor,
                    scope = scope,
                    browserComponent = browserComponent,
                    adBlocker = AdBlocker(),
                    isAdBlockEnabled = { browserComponent.isAdBlockEnabled() },
                    adBlockHosts = { browserComponent.adBlockHosts() },
                ),
                chromeClient = NDMChromeClient(browserComponent, ::getWebViewHolder),
                webViewFactory = this,
            )
        })
    }

    fun removeViewHolder(id: String) {
        viewHolders.remove(id)?.release()
    }

    fun disposeAll() {
        viewHolders.forEach { (_, holder) ->
            holder.release()
        }
        viewHolders.clear()
    }

    fun applyUserAgentToAll(userAgent: String?) {
        viewHolders.values.forEach { holder ->
            holder.webView?.let { webView ->
                applyUserAgent(webView, userAgent)
            }
        }
    }

    override fun createWebView(
        context: Context,
        tab: NDMBrowserTab,
    ): NDMWebView {
        return NDMWebView(context).apply {
            val webView = this
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.setSupportZoom(true)
            webView.settings.builtInZoomControls = false
            webView.settings.setSupportMultipleWindows(true)
            webView.settings.javaScriptCanOpenWindowsAutomatically = true
            webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            webView.settings.loadsImagesAutomatically = true
            webView.settings.allowFileAccess = true
            webView.settings.allowContentAccess = true
            webView.settings.mediaPlaybackRequiresUserGesture = false
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
            applyUserAgent(webView, browserComponent.getEffectiveUserAgent())
            webView.isLongClickable = true
            webView.setOnLongClickListener {
                val hit = webView.hitTestResult

                if (hit.type == WebView.HitTestResult.SRC_ANCHOR_TYPE ||
                    hit.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
                ) {
                    val url = hit.extra ?: return@setOnLongClickListener false

                    browserComponent.onLinkSelected(
                        url,
                        tab,
                    )
                    true
                } else {
                    false
                }
            }
            webView.setDownloadListener { url, userAgent, _, _, _ ->
                scope.launch(Dispatchers.Main) {
                    if (!webView.canGoBack() && webView.originalUrl == null) {
                        browserComponent.closeTab(tab.tabId)
                    }
                    browserComponent.downloadInterceptor.onDownloadStart(
                        url,
                        userAgent,
                        webView.originalUrl ?: webView.openedBy,
                        tab,
                    )
                }
            }
            webView.tabId = tab.tabId
        }
    }

    private fun applyUserAgent(
        webView: NDMWebView,
        userAgent: String?,
    ) {
        val resolved = userAgent ?: WebSettings.getDefaultUserAgent(webView.context)
        webView.settings.userAgentString = resolved
    }

}

data class WebViewHolder(
    val tab: NDMBrowserTab,
    var webView: NDMWebView? = null,
    val navigator: WebViewNavigator,
    val client: NDMWebViewClient,
    val chromeClient: NDMChromeClient,
    private val webViewFactory: WebViewFactory,
) {

    fun activate(context: Context): NDMWebView {
        return if (webView != null) {
            (webView!!).also {
                it.onResume()
            }
        } else {
            webViewFactory.createWebView(context, tab).also { webView = it }
        }
    }

    fun deactivate() {
        webView?.onPause()
    }

    fun release() {
        webView?.onPause()
        webView?.destroy()
        webView = null
    }
}

interface WebViewFactory {
    fun createWebView(
        context: Context,
        tab: NDMBrowserTab,
    ): NDMWebView
}

class NDMWebViewClient(
    private val requestInterceptor: DownloadInterceptor,
    private val scope: CoroutineScope,
    private val browserComponent: BrowserComponent,
    private val adBlocker: AdBlocker,
    private val isAdBlockEnabled: () -> Boolean,
    private val adBlockHosts: () -> Set<String>,
) : AccompanistWebViewClient() {
    fun requestGrabberScan(view: WebView?) {
        if (view != null) {
            injectUniversalGrabber(view)
        }
    }

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        if (request != null && isAdBlockEnabled()) {
            if (request.isForMainFrame) {
                return super.shouldInterceptRequest(view, request)
            }
            val pageUrlFromHeaders = request.requestHeaders["Referer"]
            if (isCriticalBrowsingRequest(request.url.toString(), pageUrlFromHeaders)) {
                return super.shouldInterceptRequest(view, request)
            }
            val shouldBlock = adBlocker.shouldBlock(
                requestUrl = request.url.toString(),
                pageUrl = pageUrlFromHeaders,
                isMainFrame = request.isForMainFrame,
                dynamicHosts = adBlockHosts(),
            )
            if (shouldBlock) {
                return EMPTY_RESPONSE
            }
        }
        if (request != null) {
            scope.launch(Dispatchers.Main) {
                requestInterceptor.interceptRequest(
                    NDMWebRequest(
                        url = request.url.toString(),
                        headers = request.requestHeaders,
                        page = view?.originalUrl ?: view?.url
                    )
                )
                val ndmWebView = view as? NDMWebView
                val tab = ndmWebView?.tabId?.let { browserComponent.getTabById(it) }
                val requestUrl = request.url?.toString()
                if (tab != null && requestUrl != null) {
                    requestInterceptor.onDetectedLinks(
                        urls = listOf(requestUrl),
                        userAgent = request.requestHeaders["User-Agent"],
                        page = ndmWebView.originalUrl ?: ndmWebView.url,
                        tab = tab,
                    )
                }
            }
        }
        return super.shouldInterceptRequest(view, request)
    }

    private fun isCriticalBrowsingRequest(
        requestUrl: String,
        pageUrl: String?,
    ): Boolean {
        val req = runCatching { Uri.parse(requestUrl) }.getOrNull() ?: return false
        val reqHost = req.host?.lowercase().orEmpty()
        val pageHost = pageUrl
            ?.let { runCatching { Uri.parse(it).host?.lowercase().orEmpty() }.getOrDefault("") }
            .orEmpty()
        val fromGooglePage = pageHost.contains("google.")
        val fromYoutubePage = pageHost.contains("youtube.com") || pageHost.contains("youtu.be")
        if (!fromGooglePage && !fromYoutubePage) return false
        return reqHost.contains("google.") ||
            reqHost.contains("gstatic.com") ||
            reqHost.contains("googleapis.com") ||
            reqHost.contains("youtube.com") ||
            reqHost.contains("googlevideo.com") ||
            reqHost.contains("ytimg.com") ||
            reqHost.contains("youtubei.googleapis.com")
    }

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest
    ): Boolean {

        val url = request.url.toString()

        // Let WebView load normal web pages
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return false
        }

        // Handle intent:// URIs
        if (url.startsWith("intent://")) {
            try {
                val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                val pm = view.context.packageManager

                if (intent.resolveActivity(pm) != null) {
                    view.context.startActivity(intent)
                } else {
                    intent.getStringExtra("browser_fallback_url")?.let {
                        view.loadUrl(it)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return true
        }

        // Handle ALL other schemes (deep links)
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            val pm = view.context.packageManager

            if (intent.resolveActivity(pm) != null) {
                view.context.startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return true
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        injectUniversalGrabber(view)
        browserComponent.onTabPageFinished(
            tabId = (view as? NDMWebView)?.tabId,
            url = view.url ?: url,
            title = view.title,
        )
    }

    private fun injectUniversalGrabber(view: WebView) {
        val tab = (view as? NDMWebView)?.tabId?.let { browserComponent.getTabById(it) } ?: return
        val userAgent = runCatching { view.settings.userAgentString }.getOrNull()
        val page = view.url ?: view.originalUrl
        view.evaluateJavascript(UNIVERSAL_GRABBER_SCRIPT) { jsResult ->
            val urls = parseUrlsFromJsResult(jsResult)
            if (urls.isEmpty()) {
                return@evaluateJavascript
            }
            scope.launch(Dispatchers.Main) {
                requestInterceptor.onDetectedLinks(
                    urls = urls,
                    userAgent = userAgent,
                    page = page,
                    tab = tab,
                )
            }
        }
    }

    private fun parseUrlsFromJsResult(jsResult: String?): List<String> {
        if (jsResult.isNullOrBlank() || jsResult == "null") {
            return emptyList()
        }
        return runCatching {
            val parsed = JSONTokener(jsResult).nextValue()
            val json = when (parsed) {
                is JSONArray -> parsed
                is String -> JSONArray(parsed)
                else -> JSONArray()
            }
            buildList {
                for (i in 0 until json.length()) {
                    val item = json.optJSONObject(i) ?: continue
                    val url = item.optString("url").takeIf { it.isNotBlank() } ?: continue
                    add(url)
                }
            }.distinct()
        }.onFailure {
            Log.d("BrowserGrabber", "Failed to parse page scan payload", it)
        }.getOrDefault(emptyList())
    }

    companion object {
        private val EMPTY_RESPONSE = WebResourceResponse(
            "text/plain",
            "utf-8",
            ByteArrayInputStream(ByteArray(0)),
        )

        private const val UNIVERSAL_GRABBER_SCRIPT = """
            (function() {
              const foundFiles = [];
              const usefulExts = new Set([
                'mp4','mkv','avi','mov','wmv','webm',
                'mp3','m4a','wav','flac','aac','ogg',
                'pdf','epub','doc','docx','xls','xlsx','ppt','pptx',
                'zip','rar','7z','tar','gz','bz2',
                'apk','xapk','apkm',
                'm3u8'
              ]);
              const blockedExts = new Set([
                'tmp','temp','bin','log',
                'jpg','jpeg','png','gif','webp','svg','ico',
                'css','js','map','txt','json','xml',
                'html','htm','php','asp','aspx'
              ]);

              function normalizeUrl(value) {
                if (!value || value.startsWith('javascript:') || value.startsWith('blob:') || value.startsWith('data:')) {
                  return null;
                }
                try {
                  return new URL(value, location.href).href;
                } catch (_) {
                  return null;
                }
              }

              function extFrom(url) {
                const clean = url.split('#')[0].split('?')[0].toLowerCase();
                const file = clean.substring(clean.lastIndexOf('/') + 1);
                if (!file || file.indexOf('.') === -1) return '';
                return file.substring(file.lastIndexOf('.') + 1);
              }

              function isUseful(url) {
                const lower = url.toLowerCase();
                if (lower.includes('.m3u8')) return true;
                const ext = extFrom(url);
                if (!ext) return false;
                if (blockedExts.has(ext)) return false;
                return usefulExts.has(ext);
              }

              function addFile(rawUrl, nameHint) {
                const url = normalizeUrl(rawUrl);
                if (!url || !isUseful(url)) return;
                if (foundFiles.some(function(f){ return f.url === url; })) return;
                foundFiles.push({
                  url: url,
                  name: nameHint || url.split('/').pop().split('?')[0] || 'Unknown'
                });
              }

              document.querySelectorAll('a[href]').forEach(function(a) {
                addFile(a.getAttribute('href'), (a.textContent || '').trim());
                addFile(a.getAttribute('data-href'), (a.textContent || '').trim());
              });

              document.querySelectorAll('video, audio, source').forEach(function(media) {
                addFile(media.getAttribute('src'), 'Media Player File');
                addFile(media.getAttribute('data-src'), 'Media Player File');
              });

              document.querySelectorAll('[srcset]').forEach(function(el) {
                const srcset = el.getAttribute('srcset') || '';
                srcset.split(',').forEach(function(part) {
                  const candidate = part.trim().split(/\s+/)[0];
                  if (candidate) addFile(candidate, 'Srcset File');
                });
              });

              document.querySelectorAll('script').forEach(function(s) {
                const text = s.textContent || '';
                const matches = text.match(/https?:\/\/[^\s"'<>]+/gi) || [];
                matches.forEach(function(u) { addFile(u, 'Script Link'); });
              });

              // Include already fetched resources from this page session so refresh can rebuild list
              // even if the DOM does not directly expose download links.
              if (window.performance && window.performance.getEntriesByType) {
                window.performance.getEntriesByType('resource').forEach(function(entry) {
                  if (entry && entry.name) addFile(entry.name, 'Resource Link');
                });
              }

              return foundFiles;
            })();
        """
    }
}

class NDMChromeClient(
    private val browserComponent: BrowserComponent,
    private val createWebViewHolder: (tab: NDMBrowserTab) -> WebViewHolder,
) : AccompanistWebChromeClient() {
    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        if (view == null) return false
        val transport = (resultMsg?.obj as? WebView.WebViewTransport) ?: return false
        val sourceTabId = (view as? NDMWebView)?.tabId
        browserComponent.requestPopupWindow(
            sourceTabId = sourceTabId,
            sourceUrl = view.originalUrl ?: view.url,
            targetUrlHint = view.hitTestResult?.extra,
            onDecision = { decision ->
                when (decision.action) {
                    PopupWindowAction.Deny -> {
                        val cancelWebView = WebView(view.context)
                        transport.webView = cancelWebView
                        resultMsg.sendToTarget()
                        cancelWebView.stopLoading()
                        cancelWebView.destroy()
                    }

                    PopupWindowAction.Preview,
                    PopupWindowAction.Open -> {
                        val switchToNewTab = when (decision.action) {
                            PopupWindowAction.Preview -> true
                            PopupWindowAction.Open -> !decision.openInBackgroundTab
                            PopupWindowAction.Deny -> false
                        }
                        val newTab = browserComponent.newTab(
                            id = UUID.randomUUID().toString(),
                            switch = switchToNewTab,
                            url = null,
                            openedBy = sourceTabId,
                        )
                        val newWebView = createWebViewHolder(newTab).activate(view.context)
                        newWebView.openedBy = view.originalUrl ?: view.url
                        transport.webView = newWebView
                        resultMsg.sendToTarget()
                        if (decision.closeCurrentTabAfterOpen && sourceTabId != null) {
                            browserComponent.closeTab(sourceTabId)
                        }
                    }
                }
            },
        )
        return true
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        super.onReceivedTitle(view, title)
        browserComponent.onTabTitleReceived((view as? NDMWebView)?.tabId)
    }
}

class NDMWebView(
    context: Context,
) : WebView(context) {
    var openedBy: String? = null
    var tabId: String? = null
}
