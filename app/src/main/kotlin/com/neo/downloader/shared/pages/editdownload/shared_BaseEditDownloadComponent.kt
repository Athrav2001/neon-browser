package com.neo.downloader.shared.pages.editdownload

import com.neo.downloader.shared.downloaderinui.DownloaderInUiRegistry
import com.neo.downloader.shared.downloaderinui.edit.DownloadConflictDetector
import com.neo.downloader.shared.downloaderinui.edit.EditDownloadInputs
import com.neo.downloader.shared.util.BaseComponent
import com.neo.downloader.shared.util.DownloadSystem
import com.neo.downloader.shared.util.FileIconProvider
import com.arkivanov.decompose.ComponentContext
import com.neo.downloader.downloaditem.DownloadJobExtraConfig
import com.neo.downloader.downloaditem.IDownloadCredentials
import com.neo.downloader.downloaditem.IDownloadItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

open class BaseEditDownloadComponent(
    ctx: ComponentContext,
    private val downloaderInUiRegistry: DownloaderInUiRegistry,
    val iconProvider: FileIconProvider,
    val downloadSystem: DownloadSystem,
    val onRequestClose: () -> Unit,
    val downloadId: Long,
    val acceptEdit: StateFlow<Boolean>,
    private val onEdited: ((IDownloadItem) -> Unit, DownloadJobExtraConfig?) -> Unit,
) : BaseComponent(ctx) {

    val editDownloadUiChecker =
        MutableStateFlow(null as EditDownloadInputs<IDownloadItem, IDownloadCredentials, *, *, *, *>?)

    init {
        scope.launch {
            load(downloadId)
        }
    }

    private var pendingCredential: IDownloadCredentials? = null
    private val _credentialsImportedFromExternal = MutableStateFlow(false)
    val credentialsImportedFromExternal = _credentialsImportedFromExternal.asStateFlow()
    fun importCredential(credentials: IDownloadCredentials) {
        editDownloadUiChecker.value?.let {
            it.importCredentials(credentials)
        } ?: run {
            pendingCredential = credentials
        }
        _credentialsImportedFromExternal.value = true
    }

    private suspend fun load(id: Long) {
        val downloadItem = downloadSystem.getDownloadItemById(id = id)
        if (downloadItem == null) {
            onRequestClose()
            return
        }
        val downloader = downloaderInUiRegistry.getDownloaderOf(downloadItem)
        if (downloader == null) {
            onRequestClose()
            return
        }
        val httpEditDownloadInputs = downloader.createEditDownloadInputs(
            currentDownloadItem = MutableStateFlow(downloadItem),
            editedDownloadItem = MutableStateFlow(downloadItem),
            conflictDetector = DownloadConflictDetector(downloadSystem),
            scope = scope,
        )
        editDownloadUiChecker.value = httpEditDownloadInputs
        pendingCredential?.let { credentials ->
            httpEditDownloadInputs.importCredentials(credentials)
            pendingCredential = null
        }
    }


    fun onRequestEdit() {
        if (!acceptEdit.value) {
            return
        }
        editDownloadUiChecker.value?.let { editDownloadUiChecker ->
            onEdited(editDownloadUiChecker::applyEditedItemTo, editDownloadUiChecker.downloadJobConfig.value)
        }
    }
}
