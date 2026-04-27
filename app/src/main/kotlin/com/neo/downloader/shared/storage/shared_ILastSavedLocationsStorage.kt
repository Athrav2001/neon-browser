package com.neo.downloader.shared.storage

import kotlinx.coroutines.flow.MutableStateFlow

interface ILastSavedLocationsStorage {
    val lastUsedSaveLocations: MutableStateFlow<List<String>>
}
