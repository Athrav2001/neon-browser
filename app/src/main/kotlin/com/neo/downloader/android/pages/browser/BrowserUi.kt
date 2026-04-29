package com.neo.downloader.android.pages.browser

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neo.downloader.android.pages.browser.bookmark.BookmarkList
import com.neo.downloader.android.pages.browser.bookmark.EditBookmarkSheet
import com.neo.downloader.android.pages.browser.bookmark.BookmarkFavicon
import com.neo.browser.logic.history.NeoBrowserHistoryEntry
import com.neo.downloader.android.R
import com.neo.downloader.android.storage.BrowserBookmark
import com.neo.downloader.android.ui.SheetHeader
import com.neo.downloader.android.ui.SheetTitle
import com.neo.downloader.android.ui.SheetUI
import com.neo.downloader.android.ui.menu.RenderMenuInSheet
import com.neo.downloader.android.ui.page.PageFooter
import com.neo.downloader.android.ui.page.PageHeader
import com.neo.downloader.android.ui.page.PageUi
import com.neo.downloader.android.ui.widget.LoadingState
import com.neo.downloader.android.ui.widget.WebContent
import com.neo.downloader.resources.Res
import com.neo.downloader.shared.ui.widget.ActionButton
import com.neo.downloader.shared.ui.widget.CheckBox
import com.neo.downloader.shared.ui.widget.MyTextField
import com.neo.downloader.shared.ui.widget.Text
import com.neo.downloader.shared.ui.widget.TransparentIconActionButton
import com.neo.downloader.shared.util.ClipboardUtil
import com.neo.downloader.shared.util.ResponsiveDialog
import kotlinx.coroutines.launch
import com.neo.downloader.shared.util.div
import com.neo.downloader.shared.util.rememberResponsiveDialogState
import com.neo.downloader.shared.util.ui.LocalContentColor
import com.neo.downloader.shared.util.ui.WithContentAlpha
import com.neo.downloader.shared.util.ui.WithContentColor
import com.neo.downloader.shared.util.ui.icon.MyIcons
import com.neo.downloader.shared.util.ui.myColors
import com.neo.downloader.shared.util.ui.theme.myShapes
import com.neo.downloader.shared.util.ui.theme.mySpacings
import com.neo.downloader.shared.util.ui.theme.myTextSizes
import com.neo.downloader.shared.util.ui.widget.MyIcon
import ir.neo.util.compose.asStringSource
import ir.neo.util.compose.resources.myStringResource
import ir.neo.util.ifThen
import java.net.URI
import java.util.Locale
import com.neo.downloader.android.ytdlp.FormatOption
import com.neo.downloader.android.ytdlp.YtDlpManager

@Composable
fun BrowserPage(
    browserComponent: BrowserComponent,
) {
    val scope = rememberCoroutineScope()
    val viewRegistry = remember {
        WebViewRegistry(scope, browserComponent)
    }
    DisposableEffect(viewRegistry) {
        onDispose {
            viewRegistry.disposeAll()
        }
    }
    val tabs by browserComponent.tabs.collectAsState()
    val userAgent by browserComponent.userAgent.collectAsState()
    val pendingPopupWindow by browserComponent.pendingPopupWindow.collectAsState()
    var previousUserAgent by remember { mutableStateOf<String?>(null) }
    val tab = tabs.activeTab
    val bookmarks by browserComponent.bookmarks.collectAsState()
    val tabWebViewHolder = remember(tab?.tabId) {
        tab?.let {
            viewRegistry.getWebViewHolder(it)
        }
    }
    val canGoBack = tabWebViewHolder?.navigator?.canGoBack ?: false
    val currentUrl = tab?.tabState?.lastLoadedUrl ?: (tab?.tabState?.content as? WebContent.Url)?.url
    val isBrowserHomeLike = isHomeLikeUrl(currentUrl)
    BackHandler(canGoBack) {
        tabWebViewHolder?.webView?.goBack()
    }
    BackHandler(!canGoBack && tab != null && (tabs.tabsSize > 1 || !isBrowserHomeLike)) {
        tab?.let { activeTab ->
            if (tabs.tabsSize <= 1) {
                tabWebViewHolder?.navigator?.loadUrl(NDMBrowserTab.blankPage)
                    ?: browserComponent.newTab(NDMBrowserTab.blankPage)
            } else {
                browserComponent.closeTab(activeTab.tabId)
            }
        }
    }
    LaunchedEffect(tabs) {
        viewRegistry.onTabsUpdated(tabs)
    }
    LaunchedEffect(userAgent) {
        viewRegistry.applyUserAgentToAll(
            userAgent.takeIf { it.isNotBlank() }
        )
        if (previousUserAgent != null && previousUserAgent != userAgent) {
            tabWebViewHolder?.navigator?.reload()
        }
        previousUserAgent = userAgent
    }
    PageUi(
        header = {
            PageHeader(
                headerTitle = {},
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = mySpacings.largeSpace),
            )
        },
        footer = {
            PageFooter {
                Column(
                    Modifier
                        .background(myColors.surface)
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    tab?.tabState?.loadingState?.let {
                        if (it is LoadingState.Loading) {
                            Box(
                                Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Box(
                                    Modifier
                                        .height(1.dp)
                                        .fillMaxWidth(it.progress)
                                        .background(myColors.primary),
                                )
                            }
                        }
                    }
                    WithContentColor(myColors.onSurface) {
                        AddressBar(
                            browserComponent = browserComponent,
                            currentWebViewHolder = tabWebViewHolder,
                            tabs = tabs,
                            bookmarks = bookmarks,
                            onRequestOpenBookmark = { bookmark ->
                                val url = browserComponent.createNewUrlFor(bookmark.url)
                                tabWebViewHolder
                                    ?.navigator
                                    ?.loadUrl(url)
                                    ?: browserComponent.newTab(url)
                            },
                            onRequestAddBookmark = {
                                browserComponent.promptAddBookmark(BrowserBookmark("", ""))
                            },
                            modifier = Modifier,
                        )
                    }
                }
            }
        }
    ) {
        tabWebViewHolder?.let { holder ->
            NDMWebView(
                modifier = Modifier
                    .fillMaxSize()
                    .background(myColors.background)
                    .padding(it.paddingValues),
                webViewHolder = holder,
            )
        }
    }
    val showGrabberSheet by browserComponent.showGrabberSheet.collectAsState()
    val showGrabberDownloadAllSheet by browserComponent.showGrabberDownloadAllSheet.collectAsState()
    val detectedItemsByTab by browserComponent.grabberItemsByTab.collectAsState()
    val activeTabId = tab?.tabId
    val detectedItems = activeTabId?.let { detectedItemsByTab[it] }.orEmpty()
    var selectedUrls by remember(activeTabId) { mutableStateOf(setOf<String>()) }

    GrabberSheet(
        visible = showGrabberSheet,
        items = detectedItems,
        onDismissRequest = browserComponent::closeGrabber,
        onRefresh = {
            activeTabId?.let { browserComponent.clearGrabberItems(it) }
            tabWebViewHolder?.client?.requestGrabberScan(tabWebViewHolder.webView)
        },
        onDownloadOne = { url ->
            browserComponent.closeGrabber()
            browserComponent.downloadGrabberUrls(listOf(url))
        },
        onDownloadAll = {
            selectedUrls = detectedItems.map { it.url }.toSet()
            browserComponent.openGrabberDownloadAll()
        }
    )

    GrabberBulkDownloadSheet(
        visible = showGrabberDownloadAllSheet,
        items = detectedItems,
        selectedUrls = selectedUrls,
        onToggle = { url ->
            selectedUrls = if (url in selectedUrls) {
                selectedUrls - url
            } else {
                selectedUrls + url
            }
        },
        onSelectAll = {
            selectedUrls = detectedItems.map { it.url }.toSet()
        },
        onSelectNone = {
            selectedUrls = emptySet()
        },
        onDismissRequest = browserComponent::closeGrabberDownloadAll,
        onDownloadSelected = {
            browserComponent.downloadGrabberUrls(selectedUrls.toList())
            browserComponent.closeGrabberDownloadAll()
            browserComponent.closeGrabber()
        },
    )

    val showYouTubeDialog by browserComponent.showYouTubeDialog.collectAsState()
    if (showYouTubeDialog) {
        YouTubeDownloadDialog(
            onDismiss = browserComponent::closeYouTubeDialog,
            onDownload = { url, formatId ->
                browserComponent.downloadYouTube(url, formatId)
            }
        )
    }

    RenderMenuInSheet(
        browserComponent.contextMenu.collectAsState().value,
        browserComponent::closeContextMenu
    )
    BookmarkList(
        visible = browserComponent.showBookmarkList.collectAsState().value,
        onDismissRequest = {
            browserComponent.setShowBookmarkList(false)
        },
        onRemoveBookmarkRequest = {
            browserComponent.removeBookmark(it.url)
        },
        onBookmarkClick = {
            browserComponent.setShowBookmarkList(false)
            val newLink = browserComponent.createNewUrlFor(it.url)
            tabWebViewHolder
                ?.navigator
                ?.loadUrl(newLink)
                ?: browserComponent.newTab(newLink)
        },
        bookmarks = bookmarks,
        onRequestEditBookmark = browserComponent::promptEditBookmark,
        onRequestNewBookmark = {
            browserComponent.promptAddBookmark((BrowserBookmark("", "")))
        },
    )
    BrowserHistoryList(
        visible = browserComponent.showHistoryList.collectAsState().value,
        onDismissRequest = { browserComponent.setShowHistoryList(false) },
        entries = browserComponent.history.collectAsState().value,
        onHistoryClick = { entry ->
            browserComponent.setShowHistoryList(false)
            val newLink = browserComponent.createNewUrlFor(entry.url)
            tabWebViewHolder
                ?.navigator
                ?.loadUrl(newLink)
                ?: browserComponent.newTab(newLink)
        },
        onRemoveHistoryRequest = browserComponent::removeFromHistory,
        onRemoveManyRequest = browserComponent::removeHistoryEntries,
    )
    val editBookmarkState by browserComponent.editBookmarkState.collectAsState()
    editBookmarkState?.let { s ->
        EditBookmarkSheet(
            state = s,
            onSave = {
                browserComponent.addToBookmarks(
                    it,
                    if (s.editMode) {
                        s.initialValue
                    } else {
                        null
                    },
                )
                browserComponent.dismissEditBookmark()
            },
            onCancel = {
                browserComponent.dismissEditBookmark()
            }
        )
    }
    PopupWindowRequestSheet(
        request = pendingPopupWindow,
        onDismissRequest = browserComponent::dismissPopupWindow,
        onPreview = { openInBackgroundTab, closeCurrentTabAfterOpen ->
            browserComponent.respondPopupWindow(
                action = PopupWindowAction.Preview,
                openInBackgroundTab = openInBackgroundTab,
                closeCurrentTabAfterOpen = closeCurrentTabAfterOpen,
            )
        },
        onNo = {
            browserComponent.respondPopupWindow(
                action = PopupWindowAction.Deny,
                openInBackgroundTab = false,
                closeCurrentTabAfterOpen = false,
            )
        },
        onYes = { openInBackgroundTab, closeCurrentTabAfterOpen ->
            browserComponent.respondPopupWindow(
                action = PopupWindowAction.Open,
                openInBackgroundTab = openInBackgroundTab,
                closeCurrentTabAfterOpen = closeCurrentTabAfterOpen,
            )
        },
    )
    RenderMenuInSheet(
        browserComponent.mainMenu.collectAsState().value,
        browserComponent::closeMainMenu,
    )
}

@Composable
private fun PopupWindowRequestSheet(
    request: PendingPopupWindowRequest?,
    onDismissRequest: () -> Unit,
    onPreview: (openInBackgroundTab: Boolean, closeCurrentTabAfterOpen: Boolean) -> Unit,
    onNo: () -> Unit,
    onYes: (openInBackgroundTab: Boolean, closeCurrentTabAfterOpen: Boolean) -> Unit,
) {
    val visible = request != null
    val responsiveState = rememberResponsiveDialogState(visible)
    var openInBackgroundTab by remember(request) { mutableStateOf(false) }
    var closeCurrentTabAfterOpen by remember(request) { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            responsiveState.show()
        } else {
            responsiveState.hide()
        }
    }
    if (!visible) return

    ResponsiveDialog(
        state = responsiveState,
        onDismiss = onDismissRequest,
    ) {
        SheetUI(
            header = {
                SheetHeader(
                    headerTitle = {
                        SheetTitle("Popup Alert")
                    },
                    headerActions = {
                        TransparentIconActionButton(
                            MyIcons.close,
                            Res.string.close.asStringSource(),
                        ) {
                            onDismissRequest()
                        }
                    }
                )
            }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    (request.targetUrlHint?.takeIf { it.isNotBlank() }?.let { target ->
                        "This page wants to open a new window:\n$target"
                    } ?: "This page wants to open a new window.")
                )
                request.sourceUrl?.takeIf { it.isNotBlank() }?.let { source ->
                    WithContentAlpha(0.7f) {
                        Text("From: $source", maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { openInBackgroundTab = !openInBackgroundTab }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CheckBox(
                        value = openInBackgroundTab,
                        onValueChange = {
                            openInBackgroundTab = it
                        },
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Open in background tab")
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { closeCurrentTabAfterOpen = !closeCurrentTabAfterOpen }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CheckBox(
                        value = closeCurrentTabAfterOpen,
                        onValueChange = {
                            closeCurrentTabAfterOpen = it
                        },
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Close current tab after opening new tab")
                }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ActionButton(
                        text = "Preview",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onPreview(openInBackgroundTab, closeCurrentTabAfterOpen)
                        },
                    )
                    Spacer(Modifier.width(8.dp))
                    ActionButton(
                        text = "No",
                        modifier = Modifier.weight(1f),
                        onClick = onNo,
                    )
                    Spacer(Modifier.width(8.dp))
                    ActionButton(
                        text = "Yes",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onYes(openInBackgroundTab, closeCurrentTabAfterOpen)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun GrabberSheet(
    visible: Boolean,
    items: List<GrabberDetectedItem>,
    onDismissRequest: () -> Unit,
    onRefresh: () -> Unit,
    onDownloadOne: (String) -> Unit,
    onDownloadAll: () -> Unit,
) {
    fun prettyValue(value: String): String {
        return if (value.equals("Unknown", ignoreCase = true)) "—" else value
    }
    fun prettyParts(parts: Int?): String {
        return parts?.let { "$it parts" } ?: "— parts"
    }
    fun prettyDuration(durationSeconds: Double?): String {
        if (durationSeconds == null || durationSeconds <= 0.0) return ""
        val total = durationSeconds.toInt()
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.US, "%dh%02dm%02ds", h, m, s)
        else String.format(Locale.US, "%dm%02ds", m, s)
    }
    val responsiveState = rememberResponsiveDialogState(visible)
    LaunchedEffect(visible) {
        if (visible) responsiveState.show() else responsiveState.hide()
    }
    val grouped = remember(items) {
        items
            .groupBy {
                if (it.isStream) {
                    "stream:" + it.name
                        .replace(Regex("""(?i)\b(144p|240p|360p|480p|720p|1080p|1440p|2160p|4k)\b"""), "")
                        .trim()
                        .lowercase(Locale.US)
                } else {
                    "file:${it.url}"
                }
            }
            .values
            .map { groupItems ->
                val first = groupItems.first()
                val title = if (first.isStream) {
                    first.name.replace(Regex("""(?i)\b(144p|240p|360p|480p|720p|1080p|1440p|2160p|4k)\b"""), "").trim()
                        .ifBlank { first.name }
                } else {
                    first.name
                }
                title to groupItems.sortedBy { it.quality }
            }
    }
    ResponsiveDialog(
        state = responsiveState,
        onDismiss = onDismissRequest,
    ) {
        SheetUI(
            header = {
                SheetHeader(
                    headerTitle = { SheetTitle("Grabber") },
                    headerActions = {
                        TransparentIconActionButton(
                            MyIcons.refresh,
                            Res.string.refresh.asStringSource(),
                        ) { onRefresh() }
                        TransparentIconActionButton(
                            MyIcons.download,
                            Res.string.download.asStringSource(),
                        ) { onDownloadAll() }
                        TransparentIconActionButton(
                            MyIcons.close,
                            Res.string.close.asStringSource(),
                        ) { onDismissRequest() }
                    }
                )
            }
        ) {
            if (grouped.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(mySpacings.largeSpace),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No detected files yet. Tap refresh.")
                }
            } else {
                LazyColumn {
                    items(grouped) { (title, groupItems) ->
                        val first = groupItems.first()
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = mySpacings.mediumSpace, vertical = mySpacings.smallSpace)
                                .background(myColors.surface / 0.6f, myShapes.defaultRounded)
                                .padding(mySpacings.mediumSpace)
                        ) {
                            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(6.dp))
                            if (first.isStream) {
                                Text("Stream qualities", color = myColors.onSurface / 0.7f)
                                Spacer(Modifier.height(6.dp))
                                groupItems.forEach { item ->
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable { onDownloadOne(item.url) }
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        MyIcon(MyIcons.download, null, Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(item.quality)
                                        Spacer(Modifier.weight(1f))
                                        val durationLabel = prettyDuration(item.durationSeconds)
                                        val suffix = if (durationLabel.isNotEmpty()) {
                                            "${prettyValue(item.size)} • ${prettyParts(item.partsCount)} • $durationLabel"
                                        } else {
                                            "${prettyValue(item.size)} • ${prettyParts(item.partsCount)}"
                                        }
                                        Text(suffix, color = myColors.onSurface / 0.7f)
                                    }
                                }
                            } else {
                                Text("Quality: ${prettyValue(first.quality)}", color = myColors.onSurface / 0.7f)
                                Text("Size: ${prettyValue(first.size)}", color = myColors.onSurface / 0.7f)
                                Spacer(Modifier.height(8.dp))
                                ActionButton(
                                    text = myStringResource(Res.string.download),
                                    onClick = { onDownloadOne(first.url) },
                                    start = {
                                        MyIcon(MyIcons.download, null, Modifier.size(mySpacings.iconSize))
                                        Spacer(Modifier.width(8.dp))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GrabberBulkDownloadSheet(
    visible: Boolean,
    items: List<GrabberDetectedItem>,
    selectedUrls: Set<String>,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onDismissRequest: () -> Unit,
    onDownloadSelected: () -> Unit,
) {
    val responsiveState = rememberResponsiveDialogState(visible)
    LaunchedEffect(visible) {
        if (visible) responsiveState.show() else responsiveState.hide()
    }
    ResponsiveDialog(
        state = responsiveState,
        onDismiss = onDismissRequest,
    ) {
        SheetUI(
            header = {
                SheetHeader(
                    headerTitle = { SheetTitle("Download All") },
                    headerActions = {
                        TransparentIconActionButton(
                            MyIcons.check,
                            Res.string.select_all.asStringSource(),
                        ) { onSelectAll() }
                        TransparentIconActionButton(
                            MyIcons.remove,
                            "Clear".asStringSource(),
                        ) { onSelectNone() }
                        TransparentIconActionButton(
                            MyIcons.close,
                            Res.string.close.asStringSource(),
                        ) { onDismissRequest() }
                    }
                )
            }
        ) {
            Column(Modifier.fillMaxWidth()) {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(items) { item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(item.url) }
                                .padding(horizontal = mySpacings.mediumSpace, vertical = mySpacings.smallSpace),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CheckBox(
                                value = item.url in selectedUrls,
                                onValueChange = { onToggle(item.url) },
                            )
                            Spacer(Modifier.width(mySpacings.mediumSpace))
                            Column(Modifier.weight(1f)) {
                                Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "Quality: ${item.quality}  •  Size: ${item.size}  •  Parts: ${item.partsCount ?: "—"}",
                                    color = myColors.onSurface / 0.7f,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(mySpacings.mediumSpace))
                ActionButton(
                    text = "Download Selected (${selectedUrls.size})",
                    onClick = onDownloadSelected,
                    enabled = selectedUrls.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = mySpacings.mediumSpace, vertical = mySpacings.smallSpace),
                    start = {
                        MyIcon(MyIcons.download, null, Modifier.size(mySpacings.iconSize))
                        Spacer(Modifier.width(mySpacings.smallSpace))
                    }
                )
            }
        }
    }
}

@Composable
private fun YouTubeDownloadDialog(
    onDismiss: () -> Unit,
    onDownload: (url: String, formatId: String) -> Unit,
) {
    var youTubeUrl by remember { mutableStateOf("") }
    var resolvedYouTubeUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var formats by remember { mutableStateOf<List<FormatOption>>(emptyList()) }
    val scope = rememberCoroutineScope()

    ResponsiveDialog(
        state = rememberResponsiveDialogState(true),
        onDismiss = onDismiss,
    ) {
        SheetUI(
            header = {
                SheetHeader(
                    headerTitle = { SheetTitle("YouTube Download") },
                    headerActions = {
                        TransparentIconActionButton(
                            MyIcons.close,
                            Res.string.close.asStringSource(),
                        ) { onDismiss() }
                    }
                )
            }
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                MyTextField(
                    text = youTubeUrl,
                    onTextChange = { youTubeUrl = it },
                    placeholder = "Paste YouTube link here",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                ActionButton(
                    text = if (isLoading) "Loading..." else "Fetch Qualities",
                    onClick = {
                        val sanitizedUrl = youTubeUrl.trim()
                        if (sanitizedUrl.isBlank()) return@ActionButton
                        resolvedYouTubeUrl = sanitizedUrl
                        formats = emptyList()
                        isLoading = true
                        scope.launch {
                            try {
                                YtDlpManager.getFormats(sanitizedUrl).onSuccess { fetchedFormats ->
                                    formats = fetchedFormats
                                }.onFailure { e ->
                                    // TODO: show error message
                                    e.printStackTrace()
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = youTubeUrl.trim().isNotBlank() && !isLoading,
                )
                Spacer(Modifier.height(8.dp))
                if (formats.isNotEmpty()) {
                    LazyColumn {
                        items(formats) { format ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    onDownload(resolvedYouTubeUrl, format.id)
                                }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(format.quality)
                                Spacer(Modifier.weight(1f))
                                Text(format.size ?: "")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyPage(
    modifier: Modifier,
    onRequestSearch: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    fun submitSearch() {
        val text = query.trim()
        if (text.isEmpty()) return
        onRequestSearch(text)
    }
    Box(modifier) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = mySpacings.largeSpace),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Google",
                fontSize = myTextSizes.x5l,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(mySpacings.largeSpace))
            MyTextField(
                text = query,
                onTextChange = { query = it },
                placeholder = "Search Google or type URL",
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { submitSearch() },
                    onDone = { submitSearch() },
                ),
                start = {
                    MyIcon(
                        MyIcons.search,
                        null,
                        Modifier
                            .padding(start = mySpacings.mediumSpace)
                            .size(mySpacings.iconSize)
                    )
                },
                end = {
                    TransparentIconActionButton(
                        icon = MyIcons.search,
                        contentDescription = Res.string.search.asStringSource(),
                        onClick = ::submitSearch,
                    )
                }
            )
        }
    }
}

@Composable
private fun BookmarkSiteIcon(
    bookmark: BrowserBookmark,
    size: androidx.compose.ui.unit.Dp,
) {
    BookmarkFavicon(
        bookmark = bookmark,
        size = size,
    )
}

@Composable
private fun BookmarkQuickRow(
    bookmarks: List<BrowserBookmark>,
    onRequestOpenBookmark: (BrowserBookmark) -> Unit,
    onRequestAddBookmark: () -> Unit,
) {
    val itemShape = RoundedCornerShape(14.dp)
    val itemSize = 44.dp
    if (bookmarks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemSize),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(itemSize)
                    .clip(itemShape)
                    .border(1.dp, myColors.onBackground / 0.25f, itemShape)
                    .clickable(onClick = onRequestAddBookmark),
                contentAlignment = Alignment.Center,
            ) {
                MyIcon(MyIcons.add, null, Modifier.size(mySpacings.iconSize))
            }
        }
        return
    }
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(bookmarks) { bookmark ->
            Box(
                modifier = Modifier
                    .padding(horizontal = mySpacings.smallSpace)
                    .size(itemSize)
                    .clip(itemShape)
                    .border(1.dp, myColors.onBackground / 0.25f, itemShape)
                    .clickable { onRequestOpenBookmark(bookmark) },
                contentAlignment = Alignment.Center,
            ) {
                BookmarkSiteIcon(
                    bookmark = bookmark,
                    size = 22.dp,
                )
            }
        }
        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = mySpacings.smallSpace)
                    .size(itemSize)
                    .clip(itemShape)
                    .border(1.dp, myColors.onBackground / 0.25f, itemShape)
                    .clickable(onClick = onRequestAddBookmark),
                contentAlignment = Alignment.Center,
            ) {
                MyIcon(MyIcons.add, null, Modifier.size(mySpacings.iconSize))
            }
        }
    }
}

@Composable
private fun BrowserHistoryList(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    entries: List<NeoBrowserHistoryEntry>,
    onHistoryClick: (NeoBrowserHistoryEntry) -> Unit,
    onRemoveHistoryRequest: (NeoBrowserHistoryEntry) -> Unit,
    onRemoveManyRequest: (Set<NeoBrowserHistoryEntry>) -> Unit,
) {
    val responsiveState = rememberResponsiveDialogState(visible)
    var selectionMode by remember(visible) { mutableStateOf(false) }
    var selectedEntries by remember(visible, entries) { mutableStateOf(setOf<NeoBrowserHistoryEntry>()) }
    LaunchedEffect(visible) {
        if (visible) responsiveState.show() else responsiveState.hide()
    }
    ResponsiveDialog(
        state = responsiveState,
        onDismiss = onDismissRequest,
    ) {
        SheetUI(
            header = {
                SheetHeader(
                    headerTitle = { SheetTitle("History") },
                    headerActions = {
                        TransparentIconActionButton(
                            MyIcons.remove,
                            "Delete".asStringSource(),
                        ) {
                            if (!selectionMode) {
                                selectionMode = true
                                selectedEntries = emptySet()
                            } else if (selectedEntries.isNotEmpty()) {
                                onRemoveManyRequest(selectedEntries)
                                selectedEntries = emptySet()
                                selectionMode = false
                            }
                        }
                        TransparentIconActionButton(
                            MyIcons.close,
                            Res.string.close.asStringSource(),
                        ) { onDismissRequest() }
                    }
                )
            }
        ) {
            if (entries.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(mySpacings.largeSpace),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No history yet")
                }
            } else {
                LazyColumn {
                    if (selectionMode) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedEntries = if (selectedEntries.size == entries.size) {
                                            emptySet()
                                        } else {
                                            entries.toSet()
                                        }
                                    }
                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CheckBox(
                                    value = selectedEntries.size == entries.size && entries.isNotEmpty(),
                                    onValueChange = {
                                        selectedEntries = if (it) entries.toSet() else emptySet()
                                    },
                                )
                                Spacer(Modifier.width(12.dp))
                                Text("Select All")
                            }
                        }
                    }
                    items(entries) { entry ->
                        Row(
                            modifier = Modifier
                                .heightIn(mySpacings.thumbSize)
                                .clickable {
                                    if (selectionMode) {
                                        selectedEntries = if (entry in selectedEntries) {
                                            selectedEntries - entry
                                        } else {
                                            selectedEntries + entry
                                        }
                                    } else {
                                        onHistoryClick(entry)
                                    }
                                }
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (selectionMode) {
                                CheckBox(
                                    value = entry in selectedEntries,
                                    onValueChange = { checked ->
                                        selectedEntries = if (checked) {
                                            selectedEntries + entry
                                        } else {
                                            selectedEntries - entry
                                        }
                                    },
                                )
                                Spacer(Modifier.width(12.dp))
                            }
                            MyIcon(
                                MyIcons.clock,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = entry.title.ifBlank { entry.url },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = entry.url,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = myTextSizes.sm,
                                    color = LocalContentColor.current / 0.75f,
                                )
                            }
                            if (!selectionMode) {
                                WithContentAlpha(0.5f) {
                                    TransparentIconActionButton(
                                        MyIcons.remove,
                                        Res.string.remove.asStringSource(),
                                    ) {
                                        onRemoveHistoryRequest(entry)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddressBar(
    browserComponent: BrowserComponent,
    currentWebViewHolder: WebViewHolder?,
    tabs: NDMTabs,
    bookmarks: List<BrowserBookmark>,
    onRequestOpenBookmark: (BrowserBookmark) -> Unit,
    onRequestAddBookmark: () -> Unit,
    modifier: Modifier,
) {
    val webViewState = currentWebViewHolder?.tab?.tabState
    val navigator = currentWebViewHolder?.navigator
    val currentURL = webViewState?.lastLoadedUrl ?: (webViewState?.content as? WebContent.Url)?.url
    val currentTitle = webViewState?.pageTitle
    val isHomeLike = isHomeLikeUrl(currentURL)
    var isTabListVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .padding(horizontal = mySpacings.mediumSpace)
            .padding(vertical = mySpacings.mediumSpace)
    ) {
        AddressField(
            currentPageURL = currentURL,
            currentPageTitle = currentTitle,
            currentPageIcon = remember(webViewState?.pageIcon) {
                webViewState?.pageIcon?.asImageBitmap()
            },
            onNewPageRequested = {
                it?.let { text ->
                    val newLink = browserComponent.createNewUrlFor(text)
                    navigator
                        ?.loadUrl(newLink)
                        ?: browserComponent.newTab(newLink)
                }
            }
        )
        if (isHomeLike) {
            Spacer(Modifier.height(mySpacings.mediumSpace))
            BookmarkQuickRow(
                bookmarks = bookmarks,
                onRequestOpenBookmark = onRequestOpenBookmark,
                onRequestAddBookmark = onRequestAddBookmark,
            )
        }
        Spacer(Modifier.height(mySpacings.mediumSpace))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrowserBottomBarSlot {
                Box(
                    modifier = Modifier
                        .clickable {
                            browserComponent.goHome()
                        }
                        .padding(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_browser_home_nav),
                        contentDescription = "Home",
                        colorFilter = ColorFilter.tint(LocalContentColor.current),
                        modifier = Modifier.size(mySpacings.iconSize),
                    )
                }
            }
            BrowserBottomBarSlot {
                Box(
                    modifier = Modifier
                        .clickable { isTabListVisible = !isTabListVisible }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_browser_tabs_nav),
                        contentDescription = Res.string.browser_tabs.asStringSource().rememberString(),
                        colorFilter = ColorFilter.tint(LocalContentColor.current),
                        modifier = Modifier.size(mySpacings.iconSize),
                    )
                    Text(
                        text = "${tabs.tabsSize}",
                        maxLines = 1,
                        fontSize = myTextSizes.xs,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 1.dp, end = 1.dp),
                    )
                }
            }
            BrowserBottomBarSlot {
                TransparentIconActionButton(
                    icon = if (webViewState?.isLoading == true) {
                        MyIcons.close
                    } else {
                        MyIcons.refresh
                    },
                    contentDescription = Res.string.refresh.asStringSource()
                ) {
                    if (webViewState?.isLoading == true) {
                        navigator?.stopLoading()
                    } else {
                        navigator?.reload()
                    }
                }
            }
            BrowserBottomBarSlot {
                TransparentIconActionButton(
                    MyIcons.menu,
                    contentDescription = Res.string.menu.asStringSource()
                ) {
                    browserComponent.openMainMenu()
                }
            }
        }
    }
    TabList(
        visible = isTabListVisible,
        onDismissRequest = {
            isTabListVisible = false
        },
        onCloseTabRequest = {
            browserComponent.closeTab(it.tabId)
        },
        onTabClick = {
            isTabListVisible = false
            browserComponent.switchTab(it.tabId)
        },
        onRequestNewTab = { requestedUrl ->
            isTabListVisible = false
            val targetUrl = requestedUrl
                ?.takeIf { it.isNotBlank() }
                ?.let { browserComponent.createNewUrlFor(it) }
                ?: NDMBrowserTab.blankPage
            browserComponent.newTab(
                targetUrl
            )
        },
        tabs = tabs,
        currentTabId = currentWebViewHolder?.tab?.tabId,
    )
}

private fun isHomeLikeUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return true
    val normalized = url.trim()
    if (normalized.startsWith("about:blank", ignoreCase = true)) return true
    val host = runCatching { URI(normalized).host?.lowercase(Locale.US).orEmpty() }.getOrDefault("")
    return host == "google.com" || host == "www.google.com"
}

@Composable
private fun RowScope.BrowserBottomBarSlot(
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
private fun TabList(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    tabs: NDMTabs,
    onRequestNewTab: (String?) -> Unit,
    onTabClick: (NDMBrowserTab) -> Unit,
    onCloseTabRequest: (NDMBrowserTab) -> Unit,
    currentTabId: String?,
) {
    val responsiveState = rememberResponsiveDialogState(visible)
    LaunchedEffect(visible) {
        if (visible) {
            responsiveState.show()
        } else {
            responsiveState.hide()
        }
    }
    ResponsiveDialog(
        state = responsiveState,
        onDismiss = onDismissRequest
    ) {
        SheetUI(
            header = {
                SheetHeader(
                    headerTitle = {
                        SheetTitle(
                            myStringResource(Res.string.browser_tabs),
                        )
                    },
                    headerActions = {
                        TransparentIconActionButton(
                            MyIcons.paste,
                            Res.string.paste.asStringSource(),
                        ) {
                            onRequestNewTab(
                                ClipboardUtil.read()
                            )
                        }
                        TransparentIconActionButton(
                            MyIcons.add,
                            Res.string.add.asStringSource(),
                        ) {
                            onRequestNewTab(null)
                        }
                        TransparentIconActionButton(
                            MyIcons.close,
                            Res.string.close.asStringSource(),
                        ) {
                            onDismissRequest()
                        }
                    }
                )
            }
        ) {
            LazyColumn {
                items(tabs.tabs) { tabItem ->
                    val isSelected = tabItem.tabId == currentTabId
                    Row(
                        modifier = Modifier
                            .heightIn(mySpacings.thumbSize)
                            .ifThen(isSelected) {
                                background(myColors.onBackground / 0.1f)
                            }
                            .clickable {
                                onTabClick(tabItem)
                            }
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val websiteIconBitmap = remember(tabItem.tabState.pageIcon) {
                            tabItem.tabState.pageIcon?.asImageBitmap()
                        }
                        val modifier = Modifier.size(24.dp)
                        if (websiteIconBitmap != null) {
                            Image(
                                bitmap = websiteIconBitmap,
                                contentDescription = null,
                                modifier = modifier,
                            )
                        } else {
                            MyIcon(
                                MyIcons.earth,
                                contentDescription = null,
                                modifier = modifier,
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = tabItem.tabState.let {
                                it.pageTitle ?: it.lastLoadedUrl
                            }.orEmpty(),
                            modifier = Modifier
                                .weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        WithContentAlpha(0.5f) {
                            TransparentIconActionButton(
                                MyIcons.close,
                                Res.string.close.asStringSource(),
                            ) {
                                onCloseTabRequest(tabItem)
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun AddressField(
    currentPageIcon: ImageBitmap?,
    currentPageURL: String?,
    currentPageTitle: String?,
    onNewPageRequested: (String?) -> Unit,
) {
    val title = currentPageTitle ?: currentPageURL ?: "Blank"
    val url = currentPageURL ?: ""
    val isSecure = remember(url) {
        url.startsWith("https://")
    }
    var isEditing by remember {
        mutableStateOf(false)
    }
    BackHandler(enabled = isEditing) {
        isEditing = false
    }
    val textFieldInteractionSource = remember { MutableInteractionSource() }
    val isFocused by textFieldInteractionSource.collectIsFocusedAsState()
    LaunchedEffect(isFocused) {
        isEditing = isFocused
    }
    if (isEditing) {
        val fr = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            fr.requestFocus()
        }
        var editingText by remember {
            mutableStateOf(url)
        }
        MyTextField(
            text = editingText,
            onTextChange = {
                editingText = it
            },
            interactionSource = textFieldInteractionSource,
            placeholder = "URL",
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(fr),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go,
            ),
            keyboardActions = KeyboardActions(
                onGo = {
                    isEditing = false
                    onNewPageRequested(editingText)
                },
            ),
            end = {
                MyIcon(
                    MyIcons.paste,
                    contentDescription = myStringResource(Res.string.paste),
                    modifier = Modifier
                        .clickable {
                            ClipboardUtil.read()?.let {
                                editingText = it
                            }
                        }
                        .fillMaxHeight()
                        .padding(horizontal = 10.dp),
                )
                MyIcon(
                    MyIcons.clear,
                    contentDescription = null,
                    modifier = Modifier
                        .clickable {
                            if (editingText.isNotEmpty()) {
                                editingText = ""
                            } else {
                                isEditing = false
                            }
                        }
                        .fillMaxHeight()
                        .padding(horizontal = 10.dp),
                )
            },
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(mySpacings.thumbSize)
                .clip(myShapes.defaultRounded)
                .background(myColors.onSurface / 0.05f)
                .clickable {
                    isEditing = true
                }
                .padding(horizontal = 16.dp)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (currentPageIcon != null) {
                Image(
                    bitmap = currentPageIcon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(16.dp))
            }
            Text(
                title,
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isSecure) {
                Spacer(Modifier.width(8.dp))
                MyIcon(
                    MyIcons.lock,
                    "HTTPS",
                    modifier = Modifier.size(24.dp),
                    tint = myColors.success,
                )
            }
        }
    }
}
