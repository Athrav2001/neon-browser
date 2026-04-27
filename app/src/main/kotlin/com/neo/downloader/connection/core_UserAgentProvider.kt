package com.neo.downloader.connection

interface UserAgentProvider {
    fun getUserAgent(): String?
}
