package com.neo.downloader.android.pages.browser

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.neo.downloader.android.ui.widget.WebView


@Composable
fun NDMWebView(
    modifier: Modifier = Modifier,
    webViewHolder: WebViewHolder,
) {
    val tab = webViewHolder.tab
    key(tab.tabId) {
        val wState = tab.tabState
        val navigator = webViewHolder.navigator
        WebView(
            state = wState,
            modifier = modifier,
            captureBackPresses = false,
            navigator = navigator,
            enablePullToRefresh = true,
            onRefresh = {
                navigator.reload()
            },
            client = webViewHolder.client,
            chromeClient = webViewHolder.chromeClient,
            onDispose = {
                webViewHolder.deactivate()
            },
            factory = {
                webViewHolder.activate(it)
            },
        )
    }

}
