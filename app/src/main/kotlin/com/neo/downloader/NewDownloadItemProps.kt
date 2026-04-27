package com.neo.downloader

import com.neo.downloader.downloaditem.DownloadItemContext
import com.neo.downloader.downloaditem.DownloadJobExtraConfig
import com.neo.downloader.downloaditem.IDownloadItem
import com.neo.downloader.utils.OnDuplicateStrategy

data class NewDownloadItemProps(
    val downloadItem: IDownloadItem,
    val extraConfig: DownloadJobExtraConfig?,
    val onDuplicateStrategy: OnDuplicateStrategy,
    val context: DownloadItemContext,
)
