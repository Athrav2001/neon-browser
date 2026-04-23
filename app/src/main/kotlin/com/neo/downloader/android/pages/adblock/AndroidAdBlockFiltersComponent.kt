package com.neo.downloader.android.pages.adblock

import com.neo.downloader.android.pages.browser.adblock.AdBlockFiltersManager
import com.neo.downloader.android.pages.browser.adblock.AdBlockFilterSource
import com.neo.downloader.shared.util.BaseComponent
import com.arkivanov.decompose.ComponentContext
import ir.amirab.util.compose.action.MenuItem
import ir.amirab.util.compose.action.buildMenu
import ir.amirab.util.compose.action.simpleAction
import ir.amirab.util.compose.asStringSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidAdBlockFiltersComponent(
    ctx: ComponentContext,
    private val adBlockFiltersManager: AdBlockFiltersManager,
    private val closeRequested: () -> Unit,
) : BaseComponent(ctx) {
    val sources = adBlockFiltersManager.sourcesFlow
    val hosts = adBlockFiltersManager.hostsFlow
    val isUpdating = adBlockFiltersManager.isUpdating
    val autoUpdateEnabled = adBlockFiltersManager.autoUpdateEnabledFlow
    val lastUpdatedAt = adBlockFiltersManager.lastUpdatedAtFlow

    private val _showManageSources = MutableStateFlow(true)
    val showManageSources = _showManageSources.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _mainMenu: MutableStateFlow<MenuItem.SubMenu?> = MutableStateFlow(null)
    val mainMenu = _mainMenu.asStateFlow()
    private val _sourceMenu: MutableStateFlow<MenuItem.SubMenu?> = MutableStateFlow(null)
    val sourceMenu = _sourceMenu.asStateFlow()
    private val _sourceEditor = MutableStateFlow<AdBlockFilterSource?>(null)
    val sourceEditor = _sourceEditor.asStateFlow()

    fun onBack() = closeRequested()

    fun openMenu() {
        val autoUpdate = autoUpdateEnabled.value
        _mainMenu.value = MenuItem.SubMenu(
            title = "Ad Block".asStringSource(),
            items = buildMenu {
                +simpleAction(
                    if (autoUpdate) "Auto Update: ON".asStringSource() else "Auto Update: OFF".asStringSource()
                ) {
                    adBlockFiltersManager.setAutoUpdateEnabled(!autoUpdate)
                    closeMenu()
                }
                +simpleAction("Force Update".asStringSource()) {
                    forceUpdate()
                    closeMenu()
                }
                +simpleAction("Manage Sources".asStringSource()) {
                    _showManageSources.value = !_showManageSources.value
                    closeMenu()
                }
            }
        )
    }

    fun closeMenu() {
        _mainMenu.value = null
    }

    fun openSourceMenu(sourceId: String) {
        val source = sources.value.firstOrNull { it.id == sourceId } ?: return
        _sourceMenu.value = MenuItem.SubMenu(
            title = source.name.asStringSource(),
            items = buildMenu {
                +simpleAction("Edit".asStringSource()) {
                    openSourceEditor(source)
                    closeSourceMenu()
                }
                +simpleAction("Force Update".asStringSource()) {
                    forceUpdateSource(source.id)
                    closeSourceMenu()
                }
                +simpleAction("Delete".asStringSource()) {
                    deleteSource(source.id)
                    closeSourceMenu()
                }
            }
        )
    }

    fun closeSourceMenu() {
        _sourceMenu.value = null
    }

    fun setSourceEnabled(sourceId: String, enabled: Boolean) {
        adBlockFiltersManager.setSourceEnabled(sourceId, enabled)
    }

    fun openNewSourceEditor() {
        _sourceEditor.value = AdBlockFilterSource(
            id = "",
            name = "",
            url = "",
            enabled = true,
        )
    }

    fun openSourceEditor(source: AdBlockFilterSource) {
        _sourceEditor.value = source
    }

    fun closeSourceEditor() {
        _sourceEditor.value = null
    }

    fun saveSource(
        sourceId: String?,
        name: String,
        url: String,
    ) {
        if (name.isBlank() || url.isBlank()) {
            _statusMessage.value = "Name and URL are required"
            return
        }
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            _statusMessage.value = "URL must start with http:// or https://"
            return
        }
        if (sourceId.isNullOrBlank()) {
            adBlockFiltersManager.addSource(name, url, enabled = true)
            _statusMessage.value = "Source added"
        } else {
            adBlockFiltersManager.updateSource(sourceId, name, url)
            _statusMessage.value = "Source updated"
        }
        closeSourceEditor()
    }

    fun deleteSource(sourceId: String) {
        adBlockFiltersManager.deleteSource(sourceId)
        _statusMessage.value = "Source deleted"
        forceUpdate()
    }

    fun forceUpdate() {
        adBlockFiltersManager.forceUpdateInBackground { result ->
            _statusMessage.value = result.fold(
                onSuccess = { "Updated ${it} hosts" },
                onFailure = { "Update failed: ${it.message ?: "unknown error"}" }
            )
        }
    }

    fun forceUpdateSource(sourceId: String) {
        adBlockFiltersManager.forceUpdateSourceInBackground(sourceId) { result ->
            _statusMessage.value = result.fold(
                onSuccess = { "Source updated" },
                onFailure = { "Update failed: ${it.message ?: "unknown error"}" }
            )
        }
    }

    fun dismissStatusMessage() {
        _statusMessage.value = null
    }
}
