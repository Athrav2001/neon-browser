package com.neo.downloader.shared.singledownloadpage

import com.neo.downloader.resources.Res
import com.neo.downloader.downloaditem.DownloadJobStatus
import com.neo.downloader.monitor.IDownloadItemState
import com.neo.downloader.monitor.ProcessingDownloadItemState
import com.neo.downloader.monitor.statusOrFinished
import com.neo.downloader.utils.ExceptionUtils
import com.neo.downloader.util.compose.StringSource
import com.neo.downloader.util.compose.asStringSource

fun createStatusString(it: IDownloadItemState): StringSource {
    if (it is ProcessingDownloadItemState && it.isWaiting) {
        return Res.string.waiting.asStringSource()
    }
    return when (val status = it.statusOrFinished()) {
        is DownloadJobStatus.Canceled -> {
            if (ExceptionUtils.isNormalCancellation(status.e)) {
                Res.string.paused
            } else {
                Res.string.error
            }
        }

        DownloadJobStatus.Downloading -> Res.string.downloading
        DownloadJobStatus.Finished -> Res.string.finished
        DownloadJobStatus.IDLE -> Res.string.idle
        is DownloadJobStatus.PreparingFile -> Res.string.preparing_file
        DownloadJobStatus.Resuming -> Res.string.resuming
        is DownloadJobStatus.Retrying -> Res.string.retrying
    }.asStringSource()
}
