package com.neo.downloader.shared.pages.home

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.neo.downloader.shared.pages.home.category.DefinedStatusCategories
import com.neo.downloader.shared.pages.home.category.DownloadStatusCategoryFilter
import com.neo.downloader.shared.util.category.Category
import com.neo.downloader.db.QueueModel

@Stable
class FilterState {
    var textToSearch by mutableStateOf("")
    var typeCategoryFilter by mutableStateOf(null as Category?)
    var queueFilter by mutableStateOf(null as QueueModel?)
    var statusFilter by mutableStateOf<DownloadStatusCategoryFilter>(DefinedStatusCategories.All)
}
