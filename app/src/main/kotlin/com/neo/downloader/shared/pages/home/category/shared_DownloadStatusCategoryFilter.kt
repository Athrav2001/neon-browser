package com.neo.downloader.shared.pages.home.category

import com.neo.downloader.monitor.IDownloadItemState
import com.neo.downloader.util.compose.IconSource
import com.neo.downloader.util.compose.StringSource

abstract class DownloadStatusCategoryFilter(
    val name: StringSource,
    val icon: IconSource,
) {
    abstract fun accept(iDownloadStatus: IDownloadItemState): Boolean
}
