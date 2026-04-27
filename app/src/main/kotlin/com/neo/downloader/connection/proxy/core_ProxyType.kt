package com.neo.downloader.connection.proxy

import kotlinx.serialization.SerialName

enum class ProxyType {
    @SerialName("http")
    HTTP,

    @SerialName("socks")
    SOCKS;
}