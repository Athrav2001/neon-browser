package com.neo.downloader.android.pages.browser

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Stable
import com.neo.browser.logic.history.NeoBrowserHistoryManager
import com.neo.browser.logic.history.NeoBrowserHistoryEntry
import com.neo.browser.logic.session.NeoBrowserSessionManager
import com.neo.browser.logic.session.NeoBrowserSessionTab
import com.neo.browser.logic.search.NeoSearchEngine
import com.neo.browser.logic.search.NeoSearchEngineProvider
import com.neo.browser.logic.url.NeoBrowserUrlResolver
import com.neo.downloader.android.ui.MainActivity
import com.neo.downloader.android.pages.add.multiple.AddMultiDownloadActivity
import com.neo.downloader.android.pages.add.single.AddSingleDownloadActivity
import com.neo.downloader.android.pages.browser.bookmark.EditBookmarkState
import com.neo.downloader.android.storage.BrowserBookmark
import com.neo.downloader.android.storage.BrowserBookmarksStorage
import com.neo.downloader.android.storage.BrowserHistoryStorage
import com.neo.downloader.android.storage.BrowserSessionStorage
import com.neo.downloader.android.storage.AppSettingsStorage
import com.neo.downloader.android.pages.browser.adblock.AdBlockFiltersManager
import com.neo.downloader.android.ui.widget.WebContent
import com.neo.downloader.android.ui.widget.WebViewState
import com.neo.downloader.resources.Res
import com.neo.downloader.shared.pages.adddownload.AddDownloadConfig
import com.neo.downloader.shared.util.BaseComponent
import com.neo.downloader.shared.util.ClipboardUtil
import com.neo.downloader.shared.util.mvi.ContainsEffects
import com.neo.downloader.shared.util.mvi.supportEffects
import com.neo.downloader.shared.util.ui.icon.MyIcons
import com.arkivanov.decompose.ComponentContext
import ir.neo.util.compose.action.AnAction
import ir.neo.util.compose.action.MenuItem
import ir.neo.util.compose.action.buildMenu
import ir.neo.util.compose.action.simpleAction
import ir.neo.util.compose.asStringSource
import ir.neo.util.ifThen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.text.orEmpty
import com.neo.downloader.android.ytdlp.YtDlpManager

class BrowserComponent(
    componentContext: ComponentContext,
    private val context: Context,
    private val json: Json,
    private val browserBookmarksStorage: BrowserBookmarksStorage,
    private val browserHistoryStorage: BrowserHistoryStorage,
    private val browserSessionStorage: BrowserSessionStorage,
    private val appSettingsStorage: AppSettingsStorage,
    private val adBlockFiltersManager: AdBlockFiltersManager,
    ) : BaseComponent(
    componentContext,
), ContainsEffects<BrowserComponent.Effects> by supportEffects() {
    private val _grabberItemsByTab = MutableStateFlow<Map<NDMBrowserTabId, List<GrabberDetectedItem>>>(emptyMap())
    val grabberItemsByTab = _grabberItemsByTab.asStateFlow()
    private val _showGrabberSheet = MutableStateFlow(false)
    val showGrabberSheet = _showGrabberSheet.asStateFlow()
    private val _showGrabberDownloadAllSheet = MutableStateFlow(false)
    val showGrabberDownloadAllSheet = _showGrabberDownloadAllSheet.asStateFlow()
    private val _showYouTubeDialog = MutableStateFlow(false)
    val showYouTubeDialog = _showYouTubeDialog.asStateFlow()
    fun openYouTubeDownload() {
        YtDlpManager.init(context)
        _showYouTubeDialog.value = true
        closeMainMenu()
    }
    fun closeYouTubeDialog() {
        _showYouTubeDialog.value = false
    }

    val downloadInterceptor = DownloadInterceptor(
        scope, {
            val intent = when (it.size) {
                0 -> null
                1 -> AddSingleDownloadActivity.createIntent(
                    context,
                    AddDownloadConfig.SingleAddConfig(it.first()),
                    json,
                )

                else -> AddMultiDownloadActivity.createIntent(
                    context,
                    AddDownloadConfig.MultipleAddConfig(
                        it
                    ),
                    json
                )
            }
            intent?.let { intent ->
                sendEffect(Effects.StartActivity(intent))
            }
        },
        onDetectedListUpdated = { tabId, items ->
            _grabberItemsByTab.update { old ->
                old + (tabId to items)
            }
        }
    )
    private val searchEngineProvider = NeoSearchEngineProvider(
        selectedEngine = NeoSearchEngine.GOOGLE
    )
    private val urlResolver = NeoBrowserUrlResolver(searchEngineProvider)
    private val historyManager = NeoBrowserHistoryManager(
        browserHistoryStorage.historyFlow
    )
    private val sessionManager = NeoBrowserSessionManager(
        browserSessionStorage.sessionFlow
    )
    val tabs = MutableStateFlow(
        createInitialTabs()
    )
    val bookmarks = browserBookmarksStorage.bookmarksFlow
    val history = historyManager.history
    val userAgent = appSettingsStorage.userAgent.asStateFlow()
    private val _mainMenu: MutableStateFlow<MenuItem.SubMenu?> = MutableStateFlow(null)
    val mainMenu = _mainMenu.asStateFlow()
    private val _showHistoryList: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showHistoryList = _showHistoryList.asStateFlow()
    val adBlockEnabled = appSettingsStorage.browserAdBlockEnabled.asStateFlow()
    private val _pendingPopupWindow = MutableStateFlow<PendingPopupWindowRequest?>(null)
    val pendingPopupWindow = _pendingPopupWindow.asStateFlow()
    fun openMainMenu() {
        val tab = tabs.value.activeTab
        val url = tab?.tabState?.lastLoadedUrl
        val title = tab?.tabState?.pageTitle
        _mainMenu.value = MenuItem.SubMenu(
            title = title?.asStringSource() ?: Res.string.menu.asStringSource(),
            items = buildMenu {
                +createNewTabAction()
                separator()
                +createShowBookmarksAction()
                +createShowHistoryAction()
                subMenu(
                    title = "User Agent".asStringSource(),
                    icon = MyIcons.earth,
                ) {
                    BrowserUserAgentPreset.entries.forEach { preset ->
                        +createSelectUserAgentAction(preset)
                    }
                }
                +createOpenGrabberAction()
                +createYouTubeDownloadAction()
                +createToggleAdBlockAction()
                if (url != null) {
                    if (isBookmarked(url)) {
                        +createRemoveFromBookmarkAction(url)
                    } else {
                        +createAddToBookmarkAction(url, title)
                    }
                }
                tab?.let {
                    separator()
                    +createCloseTabAction(it)
                }
            }
        )
    }

    fun closeMainMenu() {
        _mainMenu.value = null
    }

    fun newTab(
        url: String? = NDMBrowserTab.blankPage,
        switch: Boolean = true,
        id: String = UUID.randomUUID().toString(),
        openedBy: NDMBrowserTabId? = null,
    ): NDMBrowserTab {
        val browserTab = NDMBrowserTab(
            tabId = id,
            tabState = WebViewState(WebContent.fromNullableUrl(url)),
        )
        tabs.update { currentTabState ->
            val newTabPosition = openedBy?.let {
                // index of openedBy + 1 or null if not found
                currentTabState.tabs
                    .indexOfFirst { it.tabId == openedBy }
                    .takeIf { it >= 0 }
                    ?.plus(1)
            } ?: currentTabState.tabs.size
            val newItems = buildList {
                addAll(currentTabState.tabs)
                add(newTabPosition, browserTab)
            }
            val newIndex = if (switch) {
                newTabPosition
            } else {
                currentTabState.activeTabIndex
            }
            currentTabState.copy(
                tabs = newItems,
                activeTabIndex = newIndex
            )
        }
        persistSessionSnapshot()
        return browserTab
    }

    fun closeTab(tabId: NDMBrowserTabId) {
        tabs.update {
            val newItems = it.tabs.filterNot { it.tabId == tabId }
            it.copy(
                tabs = newItems,
                activeTabIndex = runCatching {
                    it.activeTabIndex.coerceIn(newItems.indices)
                }.getOrElse { -1 },
            )
        }
        downloadInterceptor.clearDetectedItems(tabId)
        _grabberItemsByTab.update { it - tabId }
        persistSessionSnapshot()
    }

    fun addToBookmarks(
        bookmark: BrowserBookmark,
        replaceWith: BrowserBookmark?,
    ) {
        browserBookmarksStorage.bookmarksFlow.update { currentBookmarks ->
            if (replaceWith != null) {
                currentBookmarks.map { item ->
                    item.ifThen(item == replaceWith) {
                        bookmark
                    }
                }
            } else {
                currentBookmarks.plus(bookmark)
            }
        }
    }

    private val _showBookmarkList: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showBookmarkList = _showBookmarkList.asStateFlow()
    fun setShowBookmarkList(show: Boolean) {
        _showBookmarkList.value = show
    }

    fun setShowHistoryList(show: Boolean) {
        _showHistoryList.value = show
    }

    fun isAdBlockEnabled(): Boolean = appSettingsStorage.browserAdBlockEnabled.value

    fun setAdBlockEnabled(enabled: Boolean) {
        appSettingsStorage.browserAdBlockEnabled.value = enabled
    }

    fun adBlockHosts(): Set<String> = adBlockFiltersManager.hostsFlow.value

    fun removeFromHistory(entry: NeoBrowserHistoryEntry) {
        browserHistoryStorage.historyFlow.update { current ->
            current.filterNot {
                it.url == entry.url && it.visitedAt == entry.visitedAt
            }
        }
    }

    fun clearHistory() {
        browserHistoryStorage.historyFlow.value = emptyList()
    }

    fun removeHistoryEntries(entries: Set<NeoBrowserHistoryEntry>) {
        if (entries.isEmpty()) return
        browserHistoryStorage.historyFlow.update { current ->
            current.filterNot { it in entries }
        }
    }

    private val _editBookmarkState = MutableStateFlow<EditBookmarkState?>(null)
    val editBookmarkState = _editBookmarkState.asStateFlow()
    fun promptAddBookmark(
        bookmark: BrowserBookmark,
    ) {
        _editBookmarkState.value = EditBookmarkState(
            initialValue = bookmark,
            editMode = false,
        )
    }

    fun promptEditBookmark(
        bookmark: BrowserBookmark
    ) {
        _editBookmarkState.value = EditBookmarkState(
            initialValue = bookmark,
            editMode = true,
        )
    }

    fun dismissEditBookmark() {
        _editBookmarkState.value = null
    }

    fun removeBookmark(url: String) {
        browserBookmarksStorage.bookmarksFlow.update {
            it.filterNot { bookmark -> bookmark.url == url }
        }
    }

    fun clearBookmarks() {
        browserBookmarksStorage.bookmarksFlow.value = emptyList()
    }

    fun isBookmarked(url: String): Boolean {
        return browserBookmarksStorage.bookmarksFlow.value.find {
            it.url == url
        } != null
    }

    fun switchTab(tabId: NDMBrowserTabId) {
        tabs.update {
            val tabIndex = it.tabs.indexOfFirst { it.tabId == tabId }
            val newIndex = if (tabIndex < 0) {
                it.activeTabIndex
            } else {
                tabIndex
            }
            it.copy(
                activeTabIndex = newIndex,
            )
        }
        persistSessionSnapshot()
    }

    fun getTabById(tabId: NDMBrowserTabId): NDMBrowserTab? {
        return tabs.value.tabs.find { it.tabId == tabId }
    }

    fun getActiveTabDetectedItems(): List<GrabberDetectedItem> {
        val tabId = tabs.value.activeTab?.tabId ?: return emptyList()
        return _grabberItemsByTab.value[tabId].orEmpty()
    }

    fun openGrabber() {
        val tabId = tabs.value.activeTab?.tabId ?: return
        _grabberItemsByTab.update {
            it + (tabId to downloadInterceptor.getDetectedItems(tabId))
        }
        _showGrabberSheet.value = true
        closeMainMenu()
    }

    fun closeGrabber() {
        _showGrabberSheet.value = false
        _showGrabberDownloadAllSheet.value = false
    }

    fun openGrabberDownloadAll() {
        _showGrabberDownloadAllSheet.value = true
    }

    fun closeGrabberDownloadAll() {
        _showGrabberDownloadAllSheet.value = false
    }

    fun requestPopupWindow(
        sourceTabId: NDMBrowserTabId?,
        sourceUrl: String?,
        targetUrlHint: String?,
        onDecision: (PopupWindowDecision) -> Unit,
    ) {
        _pendingPopupWindow.value?.onDecision?.invoke(
            PopupWindowDecision(
                action = PopupWindowAction.Deny,
                openInBackgroundTab = false,
                closeCurrentTabAfterOpen = false,
            )
        )
        _pendingPopupWindow.value = PendingPopupWindowRequest(
            sourceTabId = sourceTabId,
            sourceUrl = sourceUrl,
            targetUrlHint = targetUrlHint,
            onDecision = onDecision,
        )
    }

    fun respondPopupWindow(
        action: PopupWindowAction,
        openInBackgroundTab: Boolean,
        closeCurrentTabAfterOpen: Boolean,
    ) {
        val request = _pendingPopupWindow.value ?: return
        _pendingPopupWindow.value = null
        request.onDecision(
            PopupWindowDecision(
                action = action,
                openInBackgroundTab = openInBackgroundTab,
                closeCurrentTabAfterOpen = closeCurrentTabAfterOpen,
            )
        )
    }

    fun dismissPopupWindow() {
        respondPopupWindow(
            action = PopupWindowAction.Deny,
            openInBackgroundTab = false,
            closeCurrentTabAfterOpen = false,
        )
    }

    fun onGrabberRefresh(tabId: NDMBrowserTabId) {
        _grabberItemsByTab.update {
            it + (tabId to downloadInterceptor.getDetectedItems(tabId))
        }
    }

    fun clearGrabberItems(tabId: NDMBrowserTabId) {
        downloadInterceptor.clearDetectedItems(tabId)
        _grabberItemsByTab.update { it - tabId }
    }

    fun downloadGrabberUrls(urls: List<String>) {
        val tab = tabs.value.activeTab ?: return
        downloadInterceptor.triggerDownloadsByUrls(
            urls = urls,
            userAgent = getEffectiveUserAgent(),
            page = tab.tabState.lastLoadedUrl,
            tab = tab,
        )
    }

    fun downloadYouTube(url: String, formatId: String) {
        scope.launch {
            YtDlpManager.getDownloadUrl(url, formatId).onSuccess { directUrl ->
                downloadGrabberUrls(listOf(directUrl))
            }.onFailure { e ->
                Log.e("BrowserComponent", "Failed to get download URL", e)
                // TODO: show toast or notification
            }
        }
    }

    fun getEffectiveUserAgent(): String? {
        return appSettingsStorage.userAgent.value.takeIf { it.isNotBlank() }
    }

    fun applyUserAgentPreset(preset: BrowserUserAgentPreset) {
        appSettingsStorage.userAgent.value = preset.value
        closeMainMenu()
    }

    fun onTabPageFinished(
        tabId: NDMBrowserTabId?,
        url: String?,
        title: String?,
    ) {
        if (tabId == null) return
        historyManager.recordVisit(
            rawUrl = url,
            title = title,
        )
        persistSessionSnapshot()
    }

    fun onTabTitleReceived(tabId: NDMBrowserTabId?) {
        if (tabId == null) return
        persistSessionSnapshot()
    }

    fun goHome() {
        sendEffect(
            Effects.StartActivity(
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            )
        )
        sendEffect(Effects.CloseCurrentPage)
    }

    fun createNewUrlFor(urlOrSearch: String): String {
        return urlResolver.resolve(urlOrSearch)
    }

    private fun createInitialTabs(): NDMTabs {
        val session = sessionManager.current()
        val restoredTabs = session.tabs.mapNotNull { tab ->
            val tabId = tab.tabId.ifBlank { UUID.randomUUID().toString() }
            val restoredUrl = tab.url
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: NDMBrowserTab.blankPage
            val content = WebContent.fromNullableUrl(restoredUrl)
            NDMBrowserTab(
                tabId = tabId,
                tabState = WebViewState(content),
            )
        }
        if (restoredTabs.isEmpty()) {
            return NDMTabs(
                tabs = listOf(NDMBrowserTab.createDefaultTab()),
                activeTabIndex = 0,
            )
        }
        val restoredIndex = restoredTabs.indexOfFirst { it.tabId == session.activeTabId }
            .takeIf { it >= 0 } ?: 0
        return NDMTabs(
            tabs = restoredTabs,
            activeTabIndex = restoredIndex,
        )
    }

    private fun persistSessionSnapshot() {
        val tabsState = tabs.value
        val tabsToPersist = tabsState.tabs.map { tab ->
            val fallbackUrl = (tab.tabState.content as? WebContent.Url)?.url
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            NeoBrowserSessionTab(
                tabId = tab.tabId,
                url = tab.tabState.lastLoadedUrl
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: fallbackUrl
                    ?: NDMBrowserTab.blankPage,
                title = tab.tabState.pageTitle,
            )
        }
        sessionManager.persist(
            tabs = tabsToPersist,
            activeTabId = tabsState.activeTab?.tabId,
        )
    }

    val contextMenu: MutableStateFlow<MenuItem.SubMenu?> = MutableStateFlow(null)

    fun closeContextMenu() {
        contextMenu.value = null
    }

    fun onLinkSelected(
        link: String,
        tab: NDMBrowserTab,
    ) {
        contextMenu.value = MenuItem.SubMenu(
            title = link.asStringSource(),
            items = buildMenu {
                +simpleAction(
                    Res.string.browser_open_in_new_tab.asStringSource(),
                    MyIcons.file,
                ) {
                    newTab(
                        url = link,
                        switch = true,
                        openedBy = tab.tabId,
                    )
                }
                +simpleAction(
                    Res.string.browser_open_in_new_background_tab.asStringSource(),
                    MyIcons.file,
                ) {
                    newTab(
                        url = link,
                        switch = false,
                        openedBy = tab.tabId,
                    )
                }
                +simpleAction(
                    Res.string.share.asStringSource(),
                    MyIcons.share,
                ) {
                    sendEffect(Effects.ShareText(link))
                }
                +simpleAction(
                    Res.string.copy.asStringSource(),
                    MyIcons.copy,
                ) {
                    ClipboardUtil.copy(link)
                }
                +simpleAction(
                    Res.string.download.asStringSource(),
                    MyIcons.download,
                ) {
                    downloadInterceptor.onDownloadStart(
                        url = link,
                        userAgent = getEffectiveUserAgent(),
                        page = null,
                        tab = tab,
                    )
                }
                if (isBookmarked(link)) {
                    +createRemoveFromBookmarkAction(link)
                } else {
                    +createAddToBookmarkAction(link, null)
                }
            }
        )
    }

    fun createNewTabAction(): AnAction {
        return simpleAction(
            title = Res.string.browser_new_tab.asStringSource(),
            icon = MyIcons.file,
        ) {
            newTab(
                url = NDMBrowserTab.blankPage,
                switch = true,
            )
        }
    }

    fun createAddToBookmarkAction(
        url: String,
        title: String?,
    ): AnAction {
        return simpleAction(
            Res.string.browser_add_to_bookmarks.asStringSource(),
            MyIcons.add,
        ) {
            promptAddBookmark(
                BrowserBookmark(
                    url = url,
                    title = title.orEmpty(),
                )
            )
        }
    }

    fun createRemoveFromBookmarkAction(
        url: String,
    ): AnAction {
        return simpleAction(
            Res.string.browser_remove_from_bookmarks.asStringSource(),
            MyIcons.remove,
        ) {
            removeBookmark(url)
        }
    }

    fun createCloseTabAction(tab: NDMBrowserTab): AnAction {
        return simpleAction(
            title = Res.string.browser_close_tab.asStringSource(),
            icon = MyIcons.close,
        ) {
            closeTab(tab.tabId)
        }
    }

    fun createShowBookmarksAction(): AnAction {
        return simpleAction(
            title = Res.string.browser_bookmarks.asStringSource(),
            icon = MyIcons.hearth,
        ) {
            setShowBookmarkList(true)
        }
    }

    fun createShowHistoryAction(): AnAction {
        return simpleAction(
            title = "History".asStringSource(),
            icon = MyIcons.clock,
        ) {
            setShowHistoryList(true)
        }
    }

    fun createOpenGrabberAction(): AnAction {
        return simpleAction(
            title = "Grabber".asStringSource(),
            icon = MyIcons.download,
        ) {
            openGrabber()
        }
    }

    fun createYouTubeDownloadAction(): AnAction {
        return simpleAction(
            title = "YouTube Download".asStringSource(),
            icon = MyIcons.download, // TODO: replace with YouTube icon if available
        ) {
            openYouTubeDownload()
        }
    }

    fun createToggleAdBlockAction(): AnAction {
        val enabled = adBlockEnabled.value
        return simpleAction(
            title = if (enabled) "Ad Block: ON".asStringSource() else "Ad Block: OFF".asStringSource(),
            icon = if (enabled) MyIcons.check else null,
        ) {
            setAdBlockEnabled(!enabled)
            closeMainMenu()
        }
    }

    private fun createSelectUserAgentAction(
        preset: BrowserUserAgentPreset
    ): AnAction {
        val isActive = appSettingsStorage.userAgent.value == preset.value
        val title = if (isActive) {
            "${preset.title} (Active)".asStringSource()
        } else {
            preset.title.asStringSource()
        }
        return simpleAction(
            title = title,
            icon = if (isActive) MyIcons.check else null,
        ) {
            applyUserAgentPreset(preset)
        }
    }

    sealed interface Effects {
        data class StartActivity(
            val intent: Intent
        ) : Effects

        data class ShareText(
            val text: String,
        ) : Effects

        data object CloseCurrentPage : Effects
    }
}

enum class BrowserUserAgentPreset(
    val title: String,
    val value: String,
) {
    Android(
        title = "Android",
        value = "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
    ),
    IPhone(
        title = "iPhone",
        value = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1",
    ),
    Desktop(
        title = "Desktop",
        value = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    ),
}

typealias NDMBrowserTabId = String

@Stable
data class NDMBrowserTab(
    val tabId: NDMBrowserTabId,
    val tabState: WebViewState,
) {
    companion object {
        fun createDefaultTab(
            page: String = blankPage
        ) = NDMBrowserTab(
            tabId = UUID.randomUUID().toString(),
            tabState = WebViewState(WebContent.Url(page)),
        )

        val blankPage = "https://www.google.com"
    }
}

@Stable
data class NDMTabs(
    val tabs: List<NDMBrowserTab>,
    val activeTabIndex: Int,
) {
    val tabsSize = tabs.size
    val activeTab get() = if (activeTabIndex == -1) null else tabs[activeTabIndex]

    companion object {
        fun createDefault(): NDMTabs = NDMTabs(
            listOf(),
            -1,
        )
    }
}

enum class PopupWindowAction {
    Preview,
    Open,
    Deny,
}

data class PopupWindowDecision(
    val action: PopupWindowAction,
    val openInBackgroundTab: Boolean,
    val closeCurrentTabAfterOpen: Boolean,
)

data class PendingPopupWindowRequest(
    val sourceTabId: NDMBrowserTabId?,
    val sourceUrl: String?,
    val targetUrlHint: String?,
    val onDecision: (PopupWindowDecision) -> Unit,
)
