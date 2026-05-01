package com.neo.downloader.android.pages.browser.adblock

import androidx.datastore.core.DataStore
import com.neo.downloader.shared.util.ConfigBaseSettingsByJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable

@Serializable
data class AdBlockFilterSource(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean = true,
    val etag: String? = null,
    val lastModified: String? = null,
    val downloadedBytes: Long? = null,
    val lastUpdatedAt: Long? = null,
)

interface IAdBlockSourceStorage {
    val sourcesFlow: MutableStateFlow<List<AdBlockFilterSource>>
}

class AdBlockSourceDatastoreStorage(
    dataStore: DataStore<List<AdBlockFilterSource>>,
) : IAdBlockSourceStorage, ConfigBaseSettingsByJson<List<AdBlockFilterSource>>(dataStore) {
    override val sourcesFlow: MutableStateFlow<List<AdBlockFilterSource>> = data
}

fun defaultAdBlockSources(): List<AdBlockFilterSource> {
    return listOf(
        AdBlockFilterSource(
            id = "ublock_filters",
            name = "uBlock filters",
            url = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt",
            enabled = true,
        ),
        AdBlockFilterSource(
            id = "adguard_dns",
            name = "AdGuard DNS filter",
            url = "https://adguardteam.github.io/AdGuardSDNSFilter/Filters/filter.txt",
            enabled = true,
        ),
    )
}
