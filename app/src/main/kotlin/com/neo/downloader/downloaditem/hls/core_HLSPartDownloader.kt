package com.neo.downloader.downloaditem.hls

import com.neo.downloader.connection.Connection
import com.neo.downloader.connection.HttpDownloaderClient
import com.neo.downloader.connection.response.HttpResponseInfo
import com.neo.downloader.connection.response.expectSuccess
import com.neo.downloader.destination.DestWriter
import com.neo.downloader.downloaditem.http.HttpDownloadCredentials
import com.neo.downloader.part.MediaSegment
import com.neo.downloader.part.PartDownloader
import com.neo.downloader.util.HttpUrlUtils
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import okio.Throttler
import kotlin.coroutines.cancellation.CancellationException

class HLSPartDownloader(
    part: MediaSegment,
    getDestWriter: () -> DestWriter,
    private val baseURL: String,
    private val headers: Map<String, String>?,
    private val username: String?,
    private val password: String?,
    private val downloadPage: String?,
    private val userAgent: String?,
    private val client: HttpDownloaderClient,
    private val speedLimiters: List<Throttler>,
) : PartDownloader<
        MediaSegment
        >(
    part = part,
    getDestWriter = getDestWriter,
) {
    override fun howMuchCanRead(maxAllowed: Long): Long {
        return maxAllowed
    }

    override suspend fun connectAndVerify(): Connection<HttpResponseInfo> {
        val fullLink = part.link
            .takeIf { HttpUrlUtils.isValidUrl(it) }
            ?.let(HttpUrlUtils::createURL)
            ?: HttpUrlUtils.createURL(baseURL).resolve(part.link)
        requireNotNull(fullLink) {
            "link is incorrect! ${part.link}"
        }
        val connect = client.connect(
            HttpDownloadCredentials(
                link = fullLink.toString(),
                headers = headers,
                username = username,
                password = password,
                downloadPage = downloadPage,
                userAgent = userAgent,
            ),
            null, null,
        )
        if (stop || !currentCoroutineContext().isActive) {
            connect.close()
            throw CancellationException()
        }
        if (!connect.responseInfo.isSuccessFul) {
            connect.close()
            throw CancellationException()
        }
        part.length = connect.responseInfo.contentLength
        return connect.let {
            it.copy(
                source = speedLimiters.fold(it.source) { acc, thr ->
                    thr.source(acc)
                }
            )
        }

    }

    override fun onCanceled(e: Throwable) {
        if (!part.isCompleted) {
            // we should restart failed parts
            part.resetCurrent()
        }
        super.onCanceled(e)
    }

    override fun onFinish() {
        part.isCompleted = true
        super.onFinish()
    }
}
