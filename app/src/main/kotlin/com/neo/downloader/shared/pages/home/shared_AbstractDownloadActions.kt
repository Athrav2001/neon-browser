package com.neo.downloader.shared.pages.home

import com.neo.downloader.resources.Res
import com.neo.downloader.shared.action.createMoveToCategoryAction
import com.neo.downloader.shared.action.createMoveToQueueAction
import com.neo.downloader.shared.pagemanager.DownloadDialogManager
import com.neo.downloader.shared.pagemanager.EditDownloadDialogManager
import com.neo.downloader.shared.pagemanager.FileChecksumDialogManager
import com.neo.downloader.shared.util.ClipboardUtil
import com.neo.downloader.shared.util.DownloadSystem
import com.neo.downloader.shared.util.category.Category
import com.neo.downloader.shared.util.category.CategoryManager
import com.neo.downloader.shared.util.extractors.linkextractor.DownloadCredentialsFromCurl
import com.neo.downloader.shared.util.ui.icon.MyIcons
import com.neo.downloader.downloaditem.DownloadJobStatus
import com.neo.downloader.downloaditem.http.HttpDownloadCredentials
import com.neo.downloader.downloaditem.http.HttpDownloadItem
import com.neo.downloader.monitor.IDownloadItemState
import com.neo.downloader.monitor.ProcessingDownloadItemState
import com.neo.downloader.monitor.isFinished
import com.neo.downloader.monitor.statusOrFinished
import com.neo.downloader.queue.QueueManager
import com.neo.downloader.util.compose.action.MenuItem
import com.neo.downloader.util.compose.action.simpleAction
import com.neo.downloader.util.compose.asStringSource
import com.neo.downloader.util.flow.combineStateFlows
import com.neo.downloader.util.flow.mapStateFlow
import com.neo.downloader.util.isNotNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

abstract class AbstractDownloadActions(
    private val scope: CoroutineScope,
    downloadSystem: DownloadSystem,
    downloadDialogManager: DownloadDialogManager,
    editDownloadDialogManager: EditDownloadDialogManager,
    fileChecksumDialogManager: FileChecksumDialogManager,
    val selections: StateFlow<List<IDownloadItemState>>,
    private val mainItem: StateFlow<Long?>,
    private val queueManager: QueueManager,
    private val categoryManager: CategoryManager,
    private val openFile: (Long) -> Unit,
    private val requestDelete: (List<Long>) -> Unit,
) {
    val defaultItem = combineStateFlows(
        selections,
        mainItem,
    ) { selections, mainItem ->
        selections.let {
            it.find {
                it.id == mainItem
            } ?: it.firstOrNull()
        }
    }
    val resumableSelections = selections.mapStateFlow {
        it.filter { state ->
            if (state is ProcessingDownloadItemState) {
                state.canBeResumed()
            } else {
                false
            }
        }
    }
    val pausableSelections = selections.mapStateFlow {
        it.filter { state ->
            if (state is ProcessingDownloadItemState) {
                state.canBePaused()
            } else {
                false
            }
        }
    }
    val openFileAction = simpleAction(
        title = Res.string.open.asStringSource(),
        icon = MyIcons.fileOpen,
        checkEnable = defaultItem.mapStateFlow {
            it?.statusOrFinished() is DownloadJobStatus.Finished
        },
        onActionPerformed = {
            scope.launch {
                val d = defaultItem.value ?: return@launch
                openFile(d.id)
            }
        }
    )

    val deleteAction = simpleAction(
        title = Res.string.delete.asStringSource(),
        icon = MyIcons.remove,
        checkEnable = selections.mapStateFlow { it.isNotEmpty() },
        onActionPerformed = {
            scope.launch {
                requestDelete(selections.value.map { it.id })
            }
        },
    )

    val resumeAction = simpleAction(
        title = Res.string.resume.asStringSource(),
        icon = MyIcons.resume,
        checkEnable = resumableSelections.mapStateFlow {
            it.isNotEmpty()
        },
        onActionPerformed = {
            scope.launch {
                resumableSelections.value.forEach {
                    runCatching {
                        downloadSystem.userManualResume(it.id)
                    }
                }
            }
        }
    )

    val reDownloadAction = simpleAction(
        Res.string.restart_download.asStringSource(),
        MyIcons.refresh
    ) {
        scope.launch {
            selections.value.forEach {
                scope.launch {
                    runCatching {
                        downloadSystem.reset(it.id)
                        downloadSystem.userManualResume(it.id)
                    }
                }
            }
        }
    }

    val pauseAction = simpleAction(
        title = Res.string.pause.asStringSource(),
        icon = MyIcons.pause,
        checkEnable = pausableSelections.mapStateFlow {
            it.isNotEmpty()
        },
        onActionPerformed = {
            scope.launch {
                pausableSelections.value.forEach {
                    runCatching {
                        downloadSystem.manualPause(it.id)
                    }
                }
            }
        }
    )
    val editDownloadAction = simpleAction(
        title = Res.string.edit.asStringSource(),
        icon = MyIcons.edit,
        checkEnable = defaultItem.mapStateFlow { state ->
            state ?: return@mapStateFlow false
            // don't allow edit if download is active
            if (state is ProcessingDownloadItemState) {
                !state.canBePaused()
            } else {
                true
            }
        },
        onActionPerformed = {
            scope.launch {
                val item = defaultItem.value ?: return@launch
                editDownloadDialogManager.openEditDownloadDialog(item.id)
            }
        }
    )

    val copyDownloadLinkAction = simpleAction(
        title = Res.string.copy_link.asStringSource(),
        icon = MyIcons.copy,
        checkEnable = selections.mapStateFlow { it.isNotEmpty() },
        onActionPerformed = {
            scope.launch {
                ClipboardUtil.copy(
                    selections.value.joinToString(System.lineSeparator()) { it.downloadLink }
                )
            }
        }
    )

    val copyDownloadCredentialsAsCurlAction = simpleAction(
        title = Res.string.copy_as_curl.asStringSource(),
        icon = MyIcons.copy,
        checkEnable = selections.mapStateFlow { it.isNotEmpty() },
        onActionPerformed = {
            scope.launch {
                val credentialsList = selections.value
                    .mapNotNull { downloadSystem.getDownloadItemById(it.id) }
                    .filterIsInstance<HttpDownloadItem>()
                    .map { HttpDownloadCredentials.from(it) }
                ClipboardUtil.copy(DownloadCredentialsFromCurl.generateCurlCommands(credentialsList).joinToString("\n"))
            }
        }
    )

    val openDownloadDialogAction = simpleAction(
        Res.string.show_properties.asStringSource(),
        MyIcons.info,
        checkEnable = defaultItem.mapStateFlow(::isNotNull)
    ) {
        defaultItem.value?.let { itemState ->
            downloadDialogManager.openDownloadDialog(itemState.id)
        }
    }
    protected val fileChecksumAction = simpleAction(
        title = Res.string.file_checksum.asStringSource(), MyIcons.info,
        checkEnable = selections.mapStateFlow { list ->
            list.any { iiDownloadItemState ->
                iiDownloadItemState.isFinished()
            }
        }
    ) {
        fileChecksumDialogManager.openFileChecksumPage(
            selections.value.map { it.id }
        )
    }

    protected val moveToQueueItems = MenuItem.SubMenu(
        title = Res.string.move_to_queue.asStringSource(),
        items = emptyList()
    ).apply {
        merge(
            queueManager.queues,
            selections
        ).onEach {
            val qs = queueManager.queues.value
            val list = qs.map { queue ->
                createMoveToQueueAction(scope, downloadSystem, queue, selections.value.map { it.id })
            }
            setItems(list)
        }.launchIn(scope)
    }
    protected val moveToCategoryAction = MenuItem.SubMenu(
        title = Res.string.move_to_category.asStringSource(),
        items = emptyList()
    ).apply {
        merge(
            categoryManager.categoriesFlow.mapStateFlow {
                it.map(Category::id)
            },
            selections
        ).onEach {
            val categories = categoryManager.categoriesFlow.value
            val list = categories.map { category ->
                createMoveToCategoryAction(
                    scope = scope,
                    category = category,
                    downloadSystem = downloadSystem,
                    itemIds = selections.value.map { iDownloadItemState ->
                        iDownloadItemState.id
                    }
                )
            }
            setItems(list)
        }.launchIn(scope)
    }
}
