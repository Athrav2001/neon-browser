package com.neo.downloader.downloaditem.http

import com.neo.downloader.downloaditem.IDownloadCredentials

interface IHttpBasedDownloadCredentials : IDownloadCredentials {
    val headers: Map<String, String>?
    val username: String?
    val password: String?
    val userAgent: String?
}
