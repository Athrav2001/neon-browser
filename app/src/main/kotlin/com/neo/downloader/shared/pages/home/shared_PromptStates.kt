package com.neo.downloader.shared.pages.home

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.neo.downloader.shared.util.category.Category
import com.neo.downloader.util.compose.StringSource

@Stable
class DeletePromptState(
    val downloadList: List<Long>,
    val finishedCount: Int,
    val unfinishedCount: Int,
) {
    val hasFinishedDownloads = finishedCount > 0
    var hasUnfinishedDownloads = unfinishedCount > 0
    var alsoDeleteFile by mutableStateOf(false)

    fun hasBothFinishedAndUnfinished(): Boolean {
        return hasFinishedDownloads && hasUnfinishedDownloads
    }
}

@Immutable
data class CategoryDeletePromptState(
    val category: Category,
)

@Immutable
data class ConfirmPromptState(
    val title: StringSource,
    val description: StringSource,
    val onConfirm: () -> Unit,
)
