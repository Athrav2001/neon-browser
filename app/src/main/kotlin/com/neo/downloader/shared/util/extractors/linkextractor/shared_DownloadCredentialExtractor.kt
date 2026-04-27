package com.neo.downloader.shared.util.extractors.linkextractor

import com.neo.downloader.shared.downloaderinui.DownloaderInUiRegistry
import com.neo.downloader.shared.util.extractors.Extractor
import com.neo.downloader.downloaditem.IDownloadCredentials
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


interface DownloadCredentialExtractor<T> : Extractor<T, List<IDownloadCredentials>> {
    override fun extract(input: T): List<IDownloadCredentials>
}

object DownloadCredentialFromStringExtractor :
    DownloadCredentialExtractor<String>, KoinComponent {
    val downloaderInUiRegistry: DownloaderInUiRegistry by inject()
    override fun extract(input: String): List<IDownloadCredentials> {
        return StringUrlExtractor.extract(input)
            .mapNotNull {
                downloaderInUiRegistry
                    .bestMatchForThisLink(it)
                    ?.createMinimumCredentials(it)
            }
    }
}
