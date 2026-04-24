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
            id = "easylist",
            name = "EasyList",
            url = "https://raw.githubusercontent.com/easylist/easylist/master/easylist/easylist_adservers.txt",
            enabled = true,
        ),
        AdBlockFilterSource(
            id = "ublock_filters",
            name = "uBlock filters",
            url = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt",
            enabled = true,
        ),
        AdBlockFilterSource(
            id = "adguard_base",
            name = "AdGuard Base filter",
            url = "https://raw.githubusercontent.com/AdguardTeam/FiltersRegistry/master/filters/filter_2_Base/filter.txt",
            enabled = true,
        ),
        AdBlockFilterSource(
            id = "easyprivacy",
            name = "EasyPrivacy",
            url = "https://raw.githubusercontent.com/easylist/easylist/master/easyprivacy/easyprivacy_general.txt",
            enabled = true,
        ),
        AdBlockFilterSource(
            id = "peter_lowe",
            name = "Peter Lowe's Ad and tracking server list",
            url = "https://raw.githubusercontent.com/StevenBlack/hosts/master/data/yoyo.org/hosts",
            enabled = true,
        ),
        AdBlockFilterSource(
            id = "aak_antiadblock",
            name = "AakList (Anti-Adblock Killer)",
            url = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/annoyances.txt",
            enabled = true,
        ),
        AdBlockFilterSource(
            id = "adblock_warning_removal",
            name = "Adblock Warning Removal List",
            url = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/annoyances.txt",
            enabled = true,
        ),
        AdBlockFilterSource(
            id = "ublock_unbreak",
            name = "uBlock filters - Unbreak",
            url = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/unbreak.txt",
            enabled = true,
        ),
        AdBlockFilterSource(
            id = "adguard_mobile",
            name = "AdGuard Mobile Ads filter",
            url = "https://raw.githubusercontent.com/AdguardTeam/FiltersRegistry/master/filters/filter_11_Mobile/filter.txt",
            enabled = true,
        ),
        AdBlockFilterSource(
            id = "ublock_badware",
            name = "uBlock filters - Badware risks",
            url = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/badware.txt",
            enabled = false,
        ),
        AdBlockFilterSource(
            id = "ublock_privacy",
            name = "uBlock filters - Privacy",
            url = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/privacy.txt",
            enabled = false,
        ),
        AdBlockFilterSource(
            id = "online_malicious_url",
            name = "Online Malicious URL Blocklist",
            url = "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/hosts/pro.txt",
            enabled = false,
        ),
        AdBlockFilterSource(
            id = "adguard_annoyances",
            name = "AdGuard Annoyances",
            url = "https://raw.githubusercontent.com/AdguardTeam/FiltersRegistry/master/filters/filter_14_Annoyances/filter.txt",
            enabled = true,
        ),
        AdBlockFilterSource(
            id = "fanboy_annoyance",
            name = "Fanboy's Annoyance List",
            url = "https://raw.githubusercontent.com/easylist/easylist/master/fanboy-addon/fanboy_annoyance_specific_uBO.txt",
            enabled = true,
        ),
        AdBlockFilterSource(
            id = "ublock_annoyances",
            name = "uBlock filters - Annoyances",
            url = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/annoyances.txt",
            enabled = true,
        ),
        AdBlockFilterSource(
            id = "easylist_cookie",
            name = "EasyList Cookie List",
            url = "https://raw.githubusercontent.com/easylist/easylist/master/easylist_cookie/easylist_cookie_general_block.txt",
            enabled = false,
        ),
        AdBlockFilterSource(
            id = "onehosts_lite",
            name = "1Hosts (Lite)",
            url = "https://raw.githubusercontent.com/badmojr/1Hosts/master/Lite/hosts.txt",
            enabled = false,
        ),
        AdBlockFilterSource(
            id = "fanboy_indian",
            name = "Fanboy's Indian filter",
            url = "https://raw.githubusercontent.com/ABPindo/indonesianadblockrules/master/subscriptions/abpindo.txt",
            enabled = true,
        ),
        AdBlockFilterSource(
            id = "adguard_turkish",
            name = "AdGuard Turkish filter",
            url = "https://raw.githubusercontent.com/AdguardTeam/FiltersRegistry/master/filters/filter_13_Turkish/filter.txt",
            enabled = true,
        ),
        AdBlockFilterSource(
            id = "abpindo_indonesian",
            name = "ABPindo Indonesian filter",
            url = "https://raw.githubusercontent.com/ABPindo/indonesianadblockrules/master/subscriptions/abpindo.txt",
            enabled = true,
        ),
        AdBlockFilterSource(
            id = "adguard_russian",
            name = "AdGuard Russian filter",
            url = "https://raw.githubusercontent.com/AdguardTeam/FiltersRegistry/master/filters/filter_1_Russian/filter.txt",
            enabled = true,
        ),
        AdBlockFilterSource(
            id = "adguard_spanish_portuguese",
            name = "AdGuard Spanish/Portuguese filter",
            url = "https://raw.githubusercontent.com/AdguardTeam/FiltersRegistry/master/filters/filter_9_Spanish/filter.txt",
            enabled = true,
        ),
        AdBlockFilterSource(
            id = "easylist_german",
            name = "EasyList German filter",
            url = "https://raw.githubusercontent.com/easylist/easylistgermany/master/easylistgermany/easylistgermany_adservers.txt",
            enabled = true,
        ),
        AdBlockFilterSource(
            id = "ffadblock",
            name = "FFAdblock (Anti-Adblock Killer)",
            url = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/annoyances.txt",
            enabled = true,
        ),
        AdBlockFilterSource(
            id = "oisd_full",
            name = "OISD full",
            url = "https://raw.githubusercontent.com/sjhgvr/oisd/main/oisd_big.txt",
            enabled = true,
        ),
        AdBlockFilterSource(
            id = "stevenblack_hosts",
            name = "StevenBlack Hosts",
            url = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
            enabled = true,
        ),
    )
}
