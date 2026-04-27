package com.neo.downloader.shared.storage.impl

import androidx.datastore.core.DataStore
import com.neo.downloader.shared.storage.ILastSavedLocationsStorage
import com.neo.downloader.shared.util.ConfigBaseSettingsByJson
import kotlinx.coroutines.flow.MutableStateFlow

class LastSavedLocationStorage(
    dataStore: DataStore<List<String>>
) : ConfigBaseSettingsByJson<List<String>>(dataStore), ILastSavedLocationsStorage {
    override val lastUsedSaveLocations: MutableStateFlow<List<String>> = data
}
