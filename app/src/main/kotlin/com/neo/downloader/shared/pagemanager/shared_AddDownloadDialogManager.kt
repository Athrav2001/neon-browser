package com.neo.downloader.shared.pagemanager

import com.neo.downloader.shared.pages.adddownload.AddDownloadCredentialsInUiProps
import com.neo.downloader.shared.pages.adddownload.ImportOptions

interface AddDownloadDialogManager {
    fun closeAddDownloadDialog()
    fun openAddDownloadDialog(
        links: List<AddDownloadCredentialsInUiProps>,
        importOptions: ImportOptions = ImportOptions(),
    )
}
