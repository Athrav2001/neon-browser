package com.neo.downloader.shared.singledownloadpage

import com.neo.downloader.resources.Res
import com.neo.downloader.shared.repository.BaseAppRepository
import com.neo.downloader.shared.storage.BaseAppSettingsStorage
import com.neo.downloader.shared.storage.ExtraDownloadSettingsStorage
import com.neo.downloader.shared.storage.IExtraDownloadItemSettings
import com.neo.downloader.shared.ui.configurable.item.IntConfigurable
import com.neo.downloader.shared.ui.configurable.item.SpeedLimitConfigurable
import com.neo.downloader.shared.util.BaseComponent
import com.neo.downloader.shared.util.DownloadItemOpener
import com.neo.downloader.shared.util.DownloadSystem
import com.neo.downloader.shared.util.FileIconProvider
import com.neo.downloader.shared.util.ThreadCountLimitation
import com.neo.downloader.shared.util.TimeNames
import com.neo.downloader.shared.util.convertDurationToHumanReadable
import com.neo.downloader.shared.util.convertPositiveSizeToHumanReadable
import com.neo.downloader.shared.util.convertPositiveSpeedToHumanReadable
import com.neo.downloader.shared.util.convertTimeRemainingToHumanReadable
import com.neo.downloader.shared.util.mvi.ContainsEffects
import com.neo.downloader.shared.util.mvi.supportEffects
import com.arkivanov.decompose.ComponentContext
import com.neo.downloader.DownloadManager
import com.neo.downloader.DownloadManagerEvents
import com.neo.downloader.downloaditem.DownloadJobStatus
import com.neo.downloader.monitor.CompletedDownloadItemState
import com.neo.downloader.monitor.DurationBasedProcessingDownloadItemState
import com.neo.downloader.monitor.IDownloadItemState
import com.neo.downloader.monitor.IDownloadMonitor
import com.neo.downloader.monitor.ProcessingDownloadItemState
import com.neo.downloader.util.compose.StringSource
import com.neo.downloader.util.compose.asStringSource
import com.neo.downloader.util.compose.asStringSourceWithARgs
import com.neo.downloader.util.flow.combineStateFlows
import com.neo.downloader.util.flow.createMutableStateFlowFromFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import kotlin.getValue

abstract class BaseSingleDownloadComponent<
        TExtraDownloadItemSettings : IExtraDownloadItemSettings
        >(
    ctx: ComponentContext,
    val downloadItemOpener: DownloadItemOpener,
    private val extraDownloadSettingsStorage: ExtraDownloadSettingsStorage<TExtraDownloadItemSettings>,
    private val onDismiss: () -> Unit,
    val downloadId: Long,
    private val downloadSystem: DownloadSystem,
    private val appSettings: BaseAppSettingsStorage,
    private val appRepository: BaseAppRepository,
    private val applicationScope: CoroutineScope,
    val fileIconProvider: FileIconProvider,
) : BaseComponent(ctx),
    ContainsEffects<BaseSingleDownloadComponent.Effects> by supportEffects(),
    KoinComponent {
    open val defaultShowPartInfo: Boolean = true


    private val downloadMonitor: IDownloadMonitor = downloadSystem.downloadMonitor
    private val downloadManager: DownloadManager = downloadSystem.downloadManager

    val itemStateFlow = MutableStateFlow<IDownloadItemState?>(null)
    protected val globalShowCompletionDialog: StateFlow<Boolean> = appSettings.showDownloadCompletionDialog
    protected val itemShouldShowCompletionDialog: MutableStateFlow<Boolean?> = MutableStateFlow(null as Boolean?)
    private val shouldShowCompletionDialog = combineStateFlows(
        globalShowCompletionDialog,
        itemShouldShowCompletionDialog,
    ) { global, item ->
        item ?: global
    }
    val deletePartialFileOnDownloadCancellation = appSettings.deletePartialFileOnDownloadCancellation.asStateFlow()

    val extraDownloadItemSettingsFlow = createMutableStateFlowFromFlow(
        extraDownloadSettingsStorage
            .getExternalDownloadItemSettingsAsFlow(downloadId, initialEmit = false),
        extraDownloadSettingsStorage
            .getExtraDownloadItemSettings(downloadId),
        {
            scope.launch {
                extraDownloadSettingsStorage.setExtraDownloadItemSettings(
                    it
                )
            }
        },
        scope,
    )


    private fun shouldShowCompletionDialog(): Boolean {
        return shouldShowCompletionDialog.value
    }

    init {
        downloadMonitor
            .downloadListFlow
            // downloadListFlow (combinedStateFlow { active + completed } downloads) emits null sometimes when download item removed from active downloads and also not exists in completed downloads yet (exactly at the moment that download finishes)
            // however if the download removed by user (item == null)  this component will be closed outside of this component we don't need to handle this case here
            // I explicitly filter nulls here to make onEach function predictable
            // if I fix downloadListFlow to not emit nulls I can remove this filter later
            .mapNotNull { it.firstOrNull { it.id == downloadId } }
            .distinctUntilChanged()
            .onEach {
                val item = it
                val previous = itemStateFlow.value
                if (previous is ProcessingDownloadItemState && item is CompletedDownloadItemState) {
                    // if It was opened to show progress
                    if (shouldShowCompletionDialog()) {
                        itemStateFlow.value = item
                    } else {
                        itemStateFlow.value = null
                        // app component tries to create this component if user want to auto open completion dialog and this component is not created yet
                        // so we keep this component active a while to prevent create new component
                        // this prevents opening this window if global [appSettings.showDownloadCompletionDialog] is true but user explicitly tells that he don't want to open completion dialog for this item
                        delay(100)
                        close()
                    }
                } else {
                    itemStateFlow.value = item
                }
            }.launchIn(scope)
    }

    private val _showPartInfo = MutableStateFlow(defaultShowPartInfo)
    val showPartInfo = _showPartInfo.asStateFlow()
    open fun setShowPartInfo(value: Boolean) {
        _showPartInfo.value = value
    }

    // TODO this can be moved to a nested component to reduce system resource usage
    val extraDownloadProgressInfo: StateFlow<List<SingleDownloadPagePropertyItem>> = itemStateFlow
        .filterIsInstance<ProcessingDownloadItemState>()
        .map {
            buildList {
                add(SingleDownloadPagePropertyItem(Res.string.name.asStringSource(), it.name.asStringSource()))
                add(SingleDownloadPagePropertyItem(Res.string.status.asStringSource(), createStatusString(it)))
                if (it is DurationBasedProcessingDownloadItemState) {
                    add(
                        SingleDownloadPagePropertyItem(
                            Res.string.size.asStringSource(),
                            it.duration
                                ?.let(::convertDurationToHumanReadable)
                                ?: Res.string.unknown.asStringSource()
                        )
                    )
                } else {
                    add(
                        SingleDownloadPagePropertyItem(
                            Res.string.size.asStringSource(),
                            convertPositiveSizeToHumanReadable(it.contentLength, appRepository.sizeUnit.value)
                        )
                    )
                }
                add(
                    SingleDownloadPagePropertyItem(
                        Res.string.download_page_downloaded_size.asStringSource(),
                        StringSource.CombinedStringSource(
                            buildList {
                                add(convertPositiveSizeToHumanReadable(it.progress, appRepository.sizeUnit.value))
                                if (it.percent != null) {
                                    add("(${it.percent}%)".asStringSource())
                                }
                            },
                            " "
                        )
                    )
                )
                add(
                    SingleDownloadPagePropertyItem(
                        Res.string.speed.asStringSource(),
                        convertPositiveSpeedToHumanReadable(it.speed, appRepository.speedUnit.value).asStringSource()
                    )
                )
                add(
                    SingleDownloadPagePropertyItem(
                        Res.string.time_left.asStringSource(),
                        (it.remainingTime?.let { remainingTime ->
                            convertTimeRemainingToHumanReadable(remainingTime, TimeNames.ShortNames)
                        }.orEmpty()).asStringSource()
                    )
                )
                add(
                    SingleDownloadPagePropertyItem(
                        Res.string.resume_support.asStringSource(),
                        when (it.supportResume) {
                            true -> Res.string.yes.asStringSource()
                            false -> Res.string.no.asStringSource()
                            null -> Res.string.unknown.asStringSource()
                        },
                        when (it.supportResume) {
                            true -> SingleDownloadPagePropertyItem.ValueType.Success
                            false -> SingleDownloadPagePropertyItem.ValueType.Error
                            null -> SingleDownloadPagePropertyItem.ValueType.Normal
                        }
                    )
                )
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())



    fun openFolder() {
        val itemState = itemStateFlow.value
        applicationScope.launch {
            if (itemState is CompletedDownloadItemState) {
                downloadItemOpener.openDownloadItemFolder(downloadId)
            }
        }
        onDismiss()
    }

    fun openFile(alsoClose: Boolean = true) {
        val itemState = itemStateFlow.value
        applicationScope.launch {
            if (itemState is CompletedDownloadItemState) {
                runCatching {
                    downloadItemOpener.openDownloadItem(downloadId)
                }
            }
        }
        if (alsoClose) {
            onDismiss()
        }
    }

    fun toggle() {
        val state = itemStateFlow.value as? ProcessingDownloadItemState ?: return
        scope.launch {
            when {
                state.canBePaused() -> downloadSystem.manualPause(downloadId)
                state.canBeResumed() -> downloadSystem.userManualResume(downloadId)
            }
        }
    }

    fun resume() {
        val state = itemStateFlow.value as? ProcessingDownloadItemState ?: return
        scope.launch {
            if (state.canBeResumed()) {
                downloadSystem.userManualResume(downloadId)
            }
        }
    }

    fun pause() {
        val state = itemStateFlow.value as? ProcessingDownloadItemState ?: return
        scope.launch {
            if (state.canBePaused()) {
                downloadSystem.manualPause(downloadId)
            }
        }
    }

    fun close() {
        scope.launch {
            onDismiss()
        }
    }

    fun cancel() {
        applicationScope.launch {
            val state = itemStateFlow.value as? ProcessingDownloadItemState
            if (deletePartialFileOnDownloadCancellation.value) {
                downloadSystem.reset(downloadId)
            } else {
                if (state?.canBePaused() ?: false) {
                    downloadSystem.manualPause(downloadId)
                }
            }
        }
        scope.launch {
            onDismiss()
        }
    }


    private val threadCount: MutableStateFlow<Int>
    private val speedLimit: MutableStateFlow<Long>

    init {
        val dItem = runBlocking {
            downloadManager.dlListDb.getById(downloadId)
        }
        threadCount = MutableStateFlow(
            dItem?.preferredConnectionCount ?: ThreadCountLimitation.MAX_NORMAL_VALUE
        )
        speedLimit = MutableStateFlow(dItem?.speedLimit ?: 0)
        downloadManager.listOfJobsEvents
            .filterIsInstance<DownloadManagerEvents.OnJobChanged>()
            .filter {
                it.downloadItem.id == dItem?.id
            }
            .onEach { event ->
                threadCount.update {
                    event.downloadItem.preferredConnectionCount ?: ThreadCountLimitation.MAX_NORMAL_VALUE
                }
                speedLimit.update {
                    event.downloadItem.speedLimit
                }
            }.launchIn(scope)


        threadCount
            .drop(1)
            .debounce(500)
            .onEach { count ->
                downloadManager.updateDownloadItem(
                    id = downloadId,
                    downloadJobExtraConfig = null
                ) {
                    it.preferredConnectionCount = count.takeIf { it > 0 }
                }
            }.launchIn(scope)
        speedLimit
            .drop(1)
            .debounce(500)
            .onEach { limit ->
                downloadManager.updateDownloadItem(
                    id = downloadId,
                    downloadJobExtraConfig = null
                ) {
                    it.speedLimit = limit
                }
            }.launchIn(scope)
    }


    val settings by lazy {
        listOf(
            IntConfigurable(
                title = Res.string.download_item_settings_thread_count.asStringSource(),
                description = Res.string.download_item_settings_thread_count_description.asStringSource(),
                backedBy = threadCount,
                describe = {
                    Res.string.download_item_settings_thread_count_describe
                        .asStringSourceWithARgs(
                            Res.string.download_item_settings_thread_count_describe_createArgs(
                                count = it.toString()
                            )
                        )
                },
                range = 0..ThreadCountLimitation.MAX_ALLOWED_THREAD_COUNT,
                renderMode = IntConfigurable.RenderMode.TextField,
            ),
            SpeedLimitConfigurable(
                title = Res.string.download_item_settings_speed_limit.asStringSource(),
                description = Res.string.download_item_settings_speed_limit_description.asStringSource(),
                backedBy = speedLimit,
                describe = {
                    if (it == 0L) {
                        Res.string.unlimited.asStringSource()
                    } else {
                        convertPositiveSpeedToHumanReadable(it, appRepository.speedUnit.value).asStringSource()
                    }
                },
            ),
        )
    }


    interface Config {
        val id: Long
    }

    sealed interface Effects {
        sealed interface Common : Effects
        interface Platform : Effects
    }
}
