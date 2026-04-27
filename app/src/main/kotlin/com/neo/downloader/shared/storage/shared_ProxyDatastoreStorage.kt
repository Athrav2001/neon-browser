package com.neo.downloader.shared.storage

import androidx.datastore.core.DataStore
import com.neo.downloader.shared.util.ConfigBaseSettingsByJson
import com.neo.downloader.shared.util.proxy.IProxyStorage
import com.neo.downloader.shared.util.proxy.ProxyData

class ProxyDatastoreStorage(
    dataStore: DataStore<ProxyData>,
) : IProxyStorage, ConfigBaseSettingsByJson<ProxyData>(dataStore) {
    override val proxyDataFlow = data
}
