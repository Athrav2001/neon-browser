package com.neo.downloader.shared.downloaderinui.add

import com.neo.downloader.shared.downloaderinui.DownloadSize
import com.neo.downloader.shared.downloaderinui.LinkChecker
import com.neo.downloader.shared.util.DownloadSystem
import com.neo.downloader.connection.IResponseInfo
import com.neo.downloader.downloaditem.IDownloadCredentials
import com.neo.downloader.downloaditem.IDownloadItem
import kotlinx.coroutines.CoroutineScope

interface NewDownloadInputsFactory<
        TDownloadItem : IDownloadItem,
        TCredentials : IDownloadCredentials,
        TResponseInfoType : IResponseInfo,
        TDownloadSize : DownloadSize,
        TLinkChecker : LinkChecker<TCredentials, TResponseInfoType, TDownloadSize>,
        TNewDownloadInputs : NewDownloadInputs<
                TDownloadItem,
                TCredentials,
                TResponseInfoType,
                TDownloadSize,
                TLinkChecker,
                >
        > {
    fun createNewDownloadInputs(
        initialCredentials: TCredentials,
        initialFolder: String,
        initialName: String,
        downloadSystem: DownloadSystem,
        scope: CoroutineScope
    ): TNewDownloadInputs
}
