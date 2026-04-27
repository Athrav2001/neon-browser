package com.neo.downloader.shared.downloaderinui.edit

import com.neo.downloader.shared.util.DownloadSystem
import com.neo.downloader.downloaditem.IDownloadItem

interface IDownloadConflictDetector<in TDownloadItem : IDownloadItem> {
    fun checkAlreadyExists(
        current: TDownloadItem,
        edited: TDownloadItem,
    ): Boolean
}

class DownloadConflictDetector(
    private val downloadSystem: DownloadSystem
) : IDownloadConflictDetector<IDownloadItem> {
    override fun checkAlreadyExists(current: IDownloadItem, edited: IDownloadItem): Boolean {
        val editedDownloadFile = downloadSystem.getDownloadFile(edited)
        val alreadyExists = editedDownloadFile.exists()
        if (alreadyExists) {
            return true
        }
        return downloadSystem
            .getAllRegisteredDownloadFiles()
            .contains(editedDownloadFile)
    }
}
