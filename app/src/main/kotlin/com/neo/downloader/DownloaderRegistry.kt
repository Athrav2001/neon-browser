package com.neo.downloader

import com.neo.downloader.downloaditem.DownloadJob
import com.neo.downloader.downloaditem.IDownloadCredentials
import com.neo.downloader.downloaditem.IDownloadItem

class DownloaderRegistry {
    private val list = mutableSetOf<Downloader<IDownloadItem, DownloadJob, IDownloadCredentials>>()
    fun add(downloader: Downloader<*, *, *>) {
        @Suppress("UNCHECKED_CAST")
        list.add(downloader as Downloader<IDownloadItem, DownloadJob, IDownloadCredentials>)
    }

    fun remove(downloader: Downloader<*, *, *>) {
        list.remove(downloader)
    }

    fun createJob(
        downloadItem: IDownloadItem,
        downloadManager: DownloadManager,
    ): DownloadJob {
        val downloader = requireNotNull(
            list.firstOrNull {
                it.accept(downloadItem)
            }
        ) {
            "Download item '${downloadItem::class.qualifiedName}' not supported!"
        }
        return downloader.createJob(downloadItem, downloadManager)
    }

    fun getAll() = list.toList()
}
