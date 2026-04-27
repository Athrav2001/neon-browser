package com.neo.downloader.connection.proxy

interface ProxyStrategyProvider {
    fun getProxyStrategyFor(url: String): ProxyStrategy
}
