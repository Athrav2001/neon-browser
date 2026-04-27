package com.neo.downloader.shared.downloaderinui.hls.add

import com.neo.downloader.shared.downloaderinui.DownloadSize
import com.neo.downloader.shared.downloaderinui.DownloadUiChecker
import com.neo.downloader.shared.downloaderinui.LinkCheckerFactory
import com.neo.downloader.downloaditem.hls.HLSDownloadCredentials
import com.neo.downloader.shared.downloaderinui.hls.HLSLinkChecker
import com.neo.downloader.downloaditem.hls.HLSResponseInfo
import com.neo.downloader.shared.util.DownloadSystem
import kotlinx.coroutines.CoroutineScope

class HLSDownloadUIChecker(
    initCredentials: HLSDownloadCredentials,
    linkCheckerFactory: LinkCheckerFactory<HLSDownloadCredentials, HLSResponseInfo, DownloadSize.Duration, HLSLinkChecker>,
    initialFolder: String,
    initialName: String,
    downloadSystem: DownloadSystem,
    scope: CoroutineScope,
) : DownloadUiChecker<HLSDownloadCredentials, HLSResponseInfo, DownloadSize.Duration, HLSLinkChecker>(
    initialCredentials = initCredentials,
    linkCheckerFactory = linkCheckerFactory,
    initialFolder = initialFolder,
    initialName = initialName,
    downloadSystem = downloadSystem,
    scope = scope,
) {
}
