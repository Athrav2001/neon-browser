package com.neo.downloader.shared.util.ondownloadcompletion

import com.neo.downloader.downloaditem.IDownloadItem

interface OnDownloadCompletionAction {
    suspend fun onDownloadCompleted(downloadItem: IDownloadItem)
}
