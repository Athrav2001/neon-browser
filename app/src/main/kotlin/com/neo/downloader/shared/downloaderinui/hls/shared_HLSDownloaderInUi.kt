package com.neo.downloader.shared.downloaderinui.hls

import com.neo.downloader.shared.downloaderinui.BasicDownloadItem
import com.neo.downloader.shared.downloaderinui.DownloaderInUi
import com.neo.downloader.shared.downloaderinui.DownloadSize
import com.neo.downloader.shared.downloaderinui.edit.DownloadConflictDetector
import com.neo.downloader.shared.downloaderinui.hls.add.HLSDownloadUIChecker
import com.neo.downloader.shared.downloaderinui.hls.add.HLSNewDownloadInputs
import com.neo.downloader.shared.downloaderinui.hls.edit.HLSEditDownloadChecker
import com.neo.downloader.shared.downloaderinui.hls.edit.HLSEditDownloadInputs
import com.neo.downloader.shared.downloaderinui.http.edit.EditDownloadChecker
import com.neo.downloader.shared.util.SizeAndSpeedUnitProvider
import com.neo.downloader.shared.util.DownloadSystem
import com.neo.downloader.downloaditem.IDownloadCredentials
import com.neo.downloader.downloaditem.hls.HLSDownloadCredentials
import com.neo.downloader.downloaditem.hls.HLSDownloadItem
import com.neo.downloader.downloaditem.hls.HLSDownloadJob
import com.neo.downloader.downloaditem.hls.HLSDownloader
import com.neo.downloader.downloaditem.hls.HLSResponseInfo
import com.neo.downloader.downloaditem.hls.IHLSCredentials
import com.neo.downloader.monitor.ProcessingDownloadItemFactoryInputs
import com.neo.downloader.monitor.ProcessingDownloadItemState
import com.neo.downloader.util.HttpUrlUtils
import com.neo.downloader.util.compose.StringSource
import com.neo.downloader.util.compose.asStringSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

class HLSDownloaderInUi(
    downloader: HLSDownloader,
    private val sizeAndSpeedUnitProvider: SizeAndSpeedUnitProvider,
) : DownloaderInUi<
        HLSDownloadCredentials,
        HLSResponseInfo,
        DownloadSize.Duration,
        HLSLinkChecker,
        HLSDownloadItem,
        HLSNewDownloadInputs,
        HLSEditDownloadInputs,
        HlsItemToCredentialMapper,
        HLSDownloadJob,
        HLSDownloader
        >(downloader) {
    override fun newDownloadUiChecker(
        initialCredentials: HLSDownloadCredentials,
        initialFolder: String,
        initialName: String,
        downloadSystem: DownloadSystem,
        scope: CoroutineScope
    ): HLSDownloadUIChecker {
        return HLSDownloadUIChecker(
            initCredentials = initialCredentials,
            linkCheckerFactory = this,
            initialFolder = initialFolder,
            initialName = initialName,
            downloadSystem = downloadSystem,
            scope = scope,
        )
    }

    override fun acceptDownloadCredentials(item: IDownloadCredentials): Boolean {
        return item is IHLSCredentials
    }

    override fun supportsThisLink(link: String): Boolean {
        return HttpUrlUtils.isValidUrl(link)
    }

    override fun createMinimumCredentials(link: String): HLSDownloadCredentials {
        return HLSDownloadCredentials(link = link)
    }

    override fun createBareDownloadItem(
        credentials: HLSDownloadCredentials,
        basicDownloadItem: BasicDownloadItem
    ): HLSDownloadItem {
        return HLSDownloadItem.createWithCredentials(
            id = -1,
            credentials = credentials,
            folder = basicDownloadItem.folder,
            name = basicDownloadItem.name,
            contentLength = basicDownloadItem.contentLength,
            preferredConnectionCount = basicDownloadItem.preferredConnectionCount,
            speedLimit = basicDownloadItem.speedLimit,
            fileChecksum = basicDownloadItem.fileChecksum,
        )
    }

    override fun createProcessingDownloadItemState(
        props: ProcessingDownloadItemFactoryInputs<HLSDownloadJob>
    ): ProcessingDownloadItemState {
        return UiProcessingItemForHSLFactory.create(
            props,
        )
    }

    override val name: StringSource = "HLS".asStringSource()

    override fun createLinkChecker(initialCredentials: HLSDownloadCredentials): HLSLinkChecker {
        return HLSLinkChecker(
            credentials = initialCredentials,
            client = downloader.client
        )
    }

    override fun createEditDownloadChecker(
        currentDownloadItem: MutableStateFlow<HLSDownloadItem>,
        editedDownloadItem: MutableStateFlow<HLSDownloadItem>,
        linkChecker: HLSLinkChecker,
        conflictDetector: DownloadConflictDetector,
        scope: CoroutineScope
    ): EditDownloadChecker<HLSDownloadItem, HLSDownloadCredentials, HLSResponseInfo, DownloadSize.Duration, HLSLinkChecker> {
        return HLSEditDownloadChecker(
            currentDownloadItem = currentDownloadItem,
            editedDownloadItem = editedDownloadItem,
            linkChecker = linkChecker,
            conflictDetector = conflictDetector,
            scope = scope,
        )
    }

    override fun createNewDownloadInputs(
        initialCredentials: HLSDownloadCredentials,
        initialFolder: String,
        initialName: String,
        downloadSystem: DownloadSystem,
        scope: CoroutineScope
    ): HLSNewDownloadInputs {
        return HLSNewDownloadInputs(
            newDownloadUiChecker(
                initialCredentials = initialCredentials,
                initialFolder = initialFolder,
                initialName = initialName,
                downloadSystem = downloadSystem,
                scope = scope,
            ),
            sizeAndSpeedUnitProvider,
            scope,
        )
    }

    override fun createEditDownloadInputs(
        currentDownloadItem: MutableStateFlow<HLSDownloadItem>,
        editedDownloadItem: MutableStateFlow<HLSDownloadItem>,
        conflictDetector: DownloadConflictDetector,
        scope: CoroutineScope
    ): HLSEditDownloadInputs {
        return HLSEditDownloadInputs(
            currentDownloadItem = currentDownloadItem,
            editedDownloadItem = editedDownloadItem,
            mapper = HlsItemToCredentialMapper(),
            conflictDetector = conflictDetector,
            scope = scope,
            linkCheckerFactory = this,
            editDownloadCheckerFactory = this,
            sizeAndSpeedUnitProvider = sizeAndSpeedUnitProvider,
        )
    }
}

