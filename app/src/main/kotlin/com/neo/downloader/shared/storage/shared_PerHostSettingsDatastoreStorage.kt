package com.neo.downloader.shared.storage

import androidx.datastore.core.DataStore
import com.neo.downloader.shared.util.ConfigBaseSettingsByJson
import com.neo.downloader.shared.util.perhostsettings.IPerHostSettingsStorage
import com.neo.downloader.shared.util.perhostsettings.PerHostSettingsItem
import kotlinx.coroutines.flow.MutableStateFlow

class PerHostSettingsDatastoreStorage(
    dataStore: DataStore<List<PerHostSettingsItem>>,
) : IPerHostSettingsStorage, ConfigBaseSettingsByJson<List<PerHostSettingsItem>>(dataStore) {
    override val perHostSettingsFlow: MutableStateFlow<List<PerHostSettingsItem>> = data
}
