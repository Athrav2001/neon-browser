package com.neo.downloader.connection.proxy

import java.net.ProxySelector

interface SystemProxySelectorProvider {
    fun getSystemProxySelector(): ProxySelector?
}

class NoopSystemProxySelectorProvider : SystemProxySelectorProvider {
    override fun getSystemProxySelector(): ProxySelector? {
        return null
    }
}
