package com.neo.downloader.android.pages.browser

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neo.downloader.android.pages.browser.bookmark.BookmarkList
import com.neo.downloader.android.pages.browser.bookmark.EditBookmarkSheet
import com.neo.downloader.android.storage.BrowserBookmark
import com.neo.downloader.android.R
import com.neo.downloader.android.ui.SheetHeader
import com.neo.downloader.android.ui.SheetTitle
import com.neo.downloader.android.ui.SheetUI
import com.neo.downloader.android.ui.menu.RenderMenuInSheet
import com.neo.downloader.android.ui.page.PageFooter
import com.neo.downloader.android.ui.page.PageHeader
import com.neo.downloader.android.ui.page.PageTitle
import com.neo.downloader.android.ui.page.PageUi
import com.neo.downloader.android.ui.widget.LoadingState
import com.neo.downloader.resources.Res
import com.neo.downloader.shared.ui.widget.ActionButton
import com.neo.downloader.shared.ui.widget.CheckBox
import com.neo.downloader.shared.ui.widget.MyTextField
import com.neo.downloader.shared.ui.widget.Text
import com.neo.downloader.shared.ui.widget.TransparentIconActionButton
import com.neo.downloader.shared.util.ClipboardUtil
import com.neo.downloader.shared.util.ResponsiveDialog
import com.neo.downloader.shared.util.div
import com.neo.downloader.shared.util.rememberResponsiveDialogState
import com.neo.downloader.shared.util.ui.WithContentAlpha
import com.neo.downloader.shared.util.ui.WithContentColor
import com.neo.downloader.shared.util.ui.icon.MyIcons
import com.neo.downloader.shared.util.ui.myColors
import com.neo.downloader.shared.util.ui.theme.myShapes
import com.neo.downloader.shared.util.ui.theme.mySpacings
import com.neo.downloader.shared.util.ui.widget.MyIcon
import ir.amirab.util.compose.asStringSource
import ir.amirab.util.compose.resources.myStringResource
import ir.amirab.util.ifThen
import java.util.Locale

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
    val tab = tabs.activeTab
    val tabWebViewHolder = remember(tab?.tabId) {
        tab?.let {
            viewRegistry.getWebViewHolder(it)
        }
    }
    BackHandler(tabs.tabsSize > 1) {
        tab?.let {
            browserComponent.closeTab(tab.tabId)
        }
    }
    BackHandler(tabWebViewHolder?.navigator?.canGoBack ?: false) {
        tabWebViewHolder?.webView?.goBack()
    }
    LaunchedEffect(tabs) {
        viewRegistry.onTabsUpdated(tabs)
    }
    PageUi(
        header = {
            PageHeader(
                leadingIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_browser_home_custom),
                        contentDescription = "Home",
                        colorFilter = ColorFilter.tint(Color.White),
                        modifier = Modifier
                            .size(mySpacings.iconSize)
                            .clickable(
                                onClick = browserComponent::goHome,
                            )
                    )
                },
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
                    Spacer(
                        Modifier
                            .height(1.dp)
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        myColors.onSurface / 0.04f,
                                        Color.Transparent,
                                    )
                                )
                            )
                    )
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
                                        .background(myColors.info),
                                )
                            }
                        }
                    }
                    WithContentColor(myColors.onSurface) {
                        AddressBar(
                            browserComponent = browserComponent,
                            currentWebViewHolder = tabWebViewHolder,
                            tabs = tabs,
                            modifier = Modifier,
                        )
                    }
                }
            }
        }
    ) {
        if (tabWebViewHolder != null) {
            NDMWebView(
                modifier = Modifier
                    .fillMaxSize()
                    .background(myColors.background)
                    .padding(it.paddingValues),
                webViewHolder = tabWebViewHolder,
            )
        } else {
            EmptyPage(
                Modifier
                    .fillMaxSize()
                    .background(myColors.background)
                    .padding(it.paddingValues),
                onRequestOpenUrlFromClipboard = {
                    ClipboardUtil.read()?.let {
                        browserComponent.newTab(
                            browserComponent.createNewUrlFor(it)
                        )
                    }
                },
                onRequestOpenBookmarks = {
                    browserComponent.setShowBookmarkList(true)
                }
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
        bookmarks = browserComponent.bookmarks.collectAsState().value,
        onRequestEditBookmark = browserComponent::promptEditBookmark,
        onRequestNewBookmark = {
            browserComponent.promptAddBookmark((BrowserBookmark("", "")))
        },
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
    RenderMenuInSheet(
        browserComponent.mainMenu.collectAsState().value,
        browserComponent::closeMainMenu,
    )
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
                                        Text(item.size, color = myColors.onSurface / 0.7f)
                                    }
                                }
                            } else {
                                Text("Quality: ${first.quality}", color = myColors.onSurface / 0.7f)
                                Text("Size: ${first.size}", color = myColors.onSurface / 0.7f)
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
                                    "Quality: ${item.quality}  •  Size: ${item.size}",
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
fun EmptyPage(
    modifier: Modifier,
    onRequestOpenUrlFromClipboard: () -> Unit,
    onRequestOpenBookmarks: () -> Unit,
) {
    Box(modifier) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                myStringResource(Res.string.browser_no_tab_open),
                maxLines = 1,
            )
            Spacer(Modifier.height(mySpacings.largeSpace))
            ActionButton(
                text = myStringResource(Res.string.browser_paste_and_go),
                onClick = onRequestOpenUrlFromClipboard,
                start = {
                    MyIcon(
                        MyIcons.paste,
                        null,
                        Modifier.size(mySpacings.iconSize)
                    )
                    Spacer(Modifier.width(mySpacings.mediumSpace))
                }
            )
            Spacer(Modifier.height(mySpacings.largeSpace))
            ActionButton(
                text = myStringResource(Res.string.browser_bookmarks),
                onClick = onRequestOpenBookmarks,
                start = {
                    MyIcon(
                        MyIcons.hearth,
                        null,
                        Modifier.size(mySpacings.iconSize)
                    )
                    Spacer(Modifier.width(mySpacings.mediumSpace))
                }
            )
        }
    }
}

@Composable
fun AddressBar(
    browserComponent: BrowserComponent,
    currentWebViewHolder: WebViewHolder?,
    tabs: NDMTabs,
    modifier: Modifier,
) {
    val webViewState = currentWebViewHolder?.tab?.tabState
    val navigator = currentWebViewHolder?.navigator
    val canGoBack = navigator?.canGoBack ?: false
    val canGoForward = navigator?.canGoForward ?: false
    val currentURL = webViewState?.lastLoadedUrl
    val currentTitle = webViewState?.pageTitle
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
        Spacer(Modifier.height(mySpacings.mediumSpace))
        Row {
            TransparentIconActionButton(
                enabled = canGoBack,
                icon = MyIcons.back,
                contentDescription = Res.string.back.asStringSource()
            ) {
                navigator?.navigateBack()
            }
            TransparentIconActionButton(
                enabled = canGoForward,
                icon = MyIcons.next,
                contentDescription = Res.string.next.asStringSource()
            ) {
                navigator?.navigateForward()
            }
            Spacer(Modifier.width(16.dp))
            webViewState?.let {
                TransparentIconActionButton(
                    icon = if (webViewState.isLoading) {
                        MyIcons.close
                    } else {
                        MyIcons.refresh
                    },
                    contentDescription = Res.string.next.asStringSource()
                ) {
                    if (webViewState.isLoading) {
                        navigator?.stopLoading()
                    } else {
                        navigator?.reload()
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            val shape = myShapes.defaultRounded
            Box(
                Modifier
                    .sizeIn(mySpacings.thumbSize, mySpacings.thumbSize)
                    .clip(shape)
                    .border(
                        1.dp, myColors.onBackground / 0.1f, shape
                    )
                    .clickable(
                        role = Role.Button,
                        onClick = {
                            isTabListVisible = !isTabListVisible
                        },
                    )
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${tabs.tabsSize}",
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                )
            }
            TransparentIconActionButton(
                MyIcons.menu,
                contentDescription = Res.string.menu.asStringSource()
            ) {
                browserComponent.openMainMenu()
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
            browserComponent.newTab(
                requestedUrl?.let {
                    browserComponent.createNewUrlFor(it)
                }
            )
        },
        tabs = tabs,
        currentTabId = currentWebViewHolder?.tab?.tabId,
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
