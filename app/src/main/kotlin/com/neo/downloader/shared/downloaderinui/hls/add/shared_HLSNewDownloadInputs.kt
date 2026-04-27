package com.neo.downloader.shared.downloaderinui.hls.add

import com.neo.downloader.resources.Res
import com.neo.downloader.shared.downloaderinui.DownloadSize
import com.neo.downloader.shared.downloaderinui.add.NewDownloadInputs
import com.neo.downloader.downloaditem.hls.HLSDownloadCredentials
import com.neo.downloader.shared.downloaderinui.hls.HLSLinkChecker
import com.neo.downloader.downloaditem.hls.HLSResponseInfo
import com.neo.downloader.shared.downloaderinui.http.applyToHttpDownload
import com.neo.downloader.shared.ui.configurable.item.BooleanConfigurable
import com.neo.downloader.shared.ui.configurable.item.FileChecksumConfigurable
import com.neo.downloader.shared.ui.configurable.item.IntConfigurable
import com.neo.downloader.shared.ui.configurable.item.SpeedLimitConfigurable
import com.neo.downloader.shared.ui.configurable.item.StringConfigurable
import com.neo.downloader.shared.util.SizeAndSpeedUnitProvider
import com.neo.downloader.shared.util.ThreadCountLimitation
import com.neo.downloader.shared.util.FileChecksum
import com.neo.downloader.shared.util.FilenameFixer
import com.neo.downloader.shared.util.convertPositiveSpeedToHumanReadable
import com.neo.downloader.shared.util.perhostsettings.PerHostSettingsItem
import com.neo.downloader.downloaditem.DownloadJobExtraConfig
import com.neo.downloader.downloaditem.DownloadStatus
import com.neo.downloader.downloaditem.hls.HLSDownloadItem
import com.neo.downloader.downloaditem.hls.HLSDownloadJobExtraConfig
import com.neo.downloader.util.HttpUrlUtils
import com.neo.downloader.util.compose.StringSource
import com.neo.downloader.util.compose.asStringSource
import com.neo.downloader.util.compose.asStringSourceWithARgs
import com.neo.downloader.util.flow.combineStateFlows
import com.neo.downloader.util.flow.createMutableStateFlowFromStateFlow
import com.neo.downloader.util.flow.mapStateFlow
import com.neo.downloader.util.flow.mapTwoWayStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class HLSNewDownloadInputs(
    downloadUiChecker: HLSDownloadUIChecker,
    private val sizeAndSpeedUnitProvider: SizeAndSpeedUnitProvider,
    private val scope: CoroutineScope,
) : NewDownloadInputs<
        HLSDownloadItem,
        HLSDownloadCredentials,
        HLSResponseInfo,
        DownloadSize.Duration,
        HLSLinkChecker,
        >(
    downloadUiChecker = downloadUiChecker
) {

    //extra settings
    private var threadCount = MutableStateFlow(ThreadCountLimitation.MAX_NORMAL_VALUE as Int?)
    private var speedLimit = MutableStateFlow(0L)
    private var fileChecksum = MutableStateFlow(null as FileChecksum?)
    private var remuxToMp4 = MutableStateFlow(true)
    private val initialWebpageTitleName = name.value
    private var useWebpageTitleAsFileName = MutableStateFlow(true)
    override val downloadItem: StateFlow<HLSDownloadItem> = combineStateFlows(
        this.credentials,
        this.folder,
        this.name,
        this.downloadSize,
        this.speedLimit,
        this.threadCount,
        this.fileChecksum,
        this.remuxToMp4,
    ) { credentials,
        folder,
        name,
        duration,
        speedLimit,
        threadCount,
        fileChecksum,
        remuxToMp4
        ->
        HLSDownloadItem(
            id = -1,
            folder = folder,
            name = name,
            link = credentials.link,
            dateAdded = openedTime,
            startTime = null,
            completeTime = null,
            status = DownloadStatus.Added,
            preferredConnectionCount = threadCount,
            speedLimit = speedLimit,
            fileChecksum = fileChecksum?.toString(),
            duration = duration?.duration,
            remuxToMp4 = remuxToMp4,
        ).withCredentials(credentials)
    }
    override val downloadJobConfig: StateFlow<DownloadJobExtraConfig?> = combineStateFlows(
        downloadUiChecker.responseInfo,
        remuxToMp4,
    ) { responseInfo, remuxToMp4 ->
        responseInfo?.let {
            HLSDownloadJobExtraConfig(
                hlsManifest = it.hlsManifest,
                remuxToMp4 = remuxToMp4,
            )
        }
    }

    init {
        useWebpageTitleAsFileName.onEach { enabled ->
            if (enabled) {
                if (initialWebpageTitleName.isNotBlank()) {
                    name.value = ensureHlsFileName(FilenameFixer.fix(initialWebpageTitleName))
                }
            } else {
                deriveNameFromLink(credentials.value.link)?.let {
                    name.value = ensureHlsFileName(FilenameFixer.fix(it))
                }
            }
        }.launchIn(scope)
    }

    override fun applyHostSettingsToExtraConfig(extraConfig: PerHostSettingsItem) {
        extraConfig.applyToHttpDownload(
            setUsername = { setCredentials(credentials.value.copy(username = it)) },
            setPassword = { setCredentials(credentials.value.copy(password = it)) },
            setUserAgent = { setCredentials(credentials.value.copy(userAgent = it)) },
            setThreadCount = { threadCount.value = it },
            setSpeedLimit = { speedLimit.value = it }
        )
    }

    override val configurableList = listOf(
        SpeedLimitConfigurable(
            Res.string.download_item_settings_speed_limit.asStringSource(),
            Res.string.download_item_settings_speed_limit_description.asStringSource(),
            backedBy = speedLimit,
            describe = {
                if (it == 0L) Res.string.unlimited.asStringSource()
                else convertPositiveSpeedToHumanReadable(
                    it, sizeAndSpeedUnitProvider.speedUnit.value
                ).asStringSource()
            }
        ),
        FileChecksumConfigurable(
            Res.string.download_item_settings_file_checksum.asStringSource(),
            Res.string.download_item_settings_file_checksum_description.asStringSource(),
            backedBy = fileChecksum,
            describe = { "".asStringSource() }
        ),
        IntConfigurable(
            Res.string.settings_download_thread_count.asStringSource(),
            Res.string.settings_download_thread_count_description.asStringSource(),
            backedBy = threadCount.mapTwoWayStateFlow(
                map = {
                    it ?: ThreadCountLimitation.MAX_NORMAL_VALUE
                },
                unMap = {
                    it.takeIf { it >= 1 }
                }
            ),
            range = 0..ThreadCountLimitation.MAX_ALLOWED_THREAD_COUNT,
            describe = {
                Res.string.download_item_settings_thread_count_describe
                    .asStringSourceWithARgs(
                        Res.string.download_item_settings_thread_count_describe_createArgs(
                            count = it.toString()
                        )
                    )
            }
        ),
        BooleanConfigurable(
            title = "Use FFmpeg MP4 conversion".asStringSource(),
            description = "Enable FFmpeg conversion for this HLS download".asStringSource(),
            backedBy = remuxToMp4,
            describe = {
                if (it) "Enabled".asStringSource() else "Disabled".asStringSource()
            }
        ),
        BooleanConfigurable(
            title = "Use webpage title as file name".asStringSource(),
            description = "When enabled, default name comes from webpage title".asStringSource(),
            backedBy = useWebpageTitleAsFileName,
            describe = {
                if (it) "Enabled".asStringSource() else "Disabled".asStringSource()
            }
        ),
        StringConfigurable(
            Res.string.username.asStringSource(),
            Res.string.download_item_settings_username_description.asStringSource(),
            backedBy = createMutableStateFlowFromStateFlow(
                flow = credentials.mapStateFlow {
                    it.username.orEmpty()
                },
                updater = {
                    setCredentials(credentials.value.copy(username = it.takeIf { it.isNotBlank() }))
                }, scope
            ),
            describe = {
                "".asStringSource()
            }
        ),
        StringConfigurable(
            Res.string.password.asStringSource(),
            Res.string.download_item_settings_password_description.asStringSource(),
            backedBy = createMutableStateFlowFromStateFlow(
                flow = credentials.mapStateFlow {
                    it.password.orEmpty()
                },
                updater = {
                    setCredentials(credentials.value.copy(password = it.takeIf { it.isNotBlank() }))
                }, scope
            ),
            describe = {
                "".asStringSource()
            }
        ),
        StringConfigurable(
            Res.string.download_item_settings_user_agent.asStringSource(),
            Res.string.download_item_settings_user_agent_description.asStringSource(),
            backedBy = credentials.mapTwoWayStateFlow(
                map = {
                    it.userAgent.orEmpty()
                },
                unMap = {
                    copy(userAgent = it.takeIf { it.isNotEmpty() })
                }
            ),
            describe = {
                "".asStringSource()
            }
        ),
        StringConfigurable(
            Res.string.download_item_settings_download_page.asStringSource(),
            Res.string.download_item_settings_download_page_description.asStringSource(),
            backedBy = credentials.mapTwoWayStateFlow(
                map = {
                    it.downloadPage.orEmpty()
                },
                unMap = {
                    copy(downloadPage = it.takeIf { it.isNotEmpty() })
                }
            ),
            describe = {
                "".asStringSource()
            }
        )
    )

    override fun downloadSizeToStringSource(downloadSize: DownloadSize.Duration): StringSource {
        return downloadSize.asStringSource()
    }

    private fun deriveNameFromLink(link: String): String? {
        val rawName = HttpUrlUtils.extractNameFromLink(link)?.takeIf { it.isNotBlank() } ?: return null
        return if (rawName.endsWith(".m3u8", ignoreCase = true)) {
            rawName.removeSuffix(".m3u8").removeSuffix(".M3U8") + ".mp4"
        } else {
            rawName
        }
    }

    private fun ensureHlsFileName(fileName: String): String {
        val trimmed = fileName.trim()
        if (trimmed.isBlank()) return "video.mp4"
        return if ('.' in trimmed) trimmed else "$trimmed.mp4"
    }
}
