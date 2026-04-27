package com.neo.downloader.db

import com.neo.downloader.part.Parts

interface IDownloadPartListDb {
    suspend fun getParts(id: Long): Parts?
    suspend fun setParts(id: Long, parts: Parts)
    suspend fun clear()
    suspend fun removeParts(id: Long)
}
