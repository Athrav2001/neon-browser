package com.neo.downloader.shared.pages.enterurl

import com.neo.downloader.shared.downloaderinui.TADownloaderInUI

sealed interface DownloaderSelection {
    data object Auto : DownloaderSelection
    data class Fixed(
        val downloaderInUi: TADownloaderInUI,
    ) : DownloaderSelection
}
