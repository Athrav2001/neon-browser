package com.neo.downloader.shared.pages.adddownload.multiple

import com.neo.downloader.shared.util.category.CategorySelectionMode
import com.neo.downloader.NewDownloadItemProps
import kotlinx.coroutines.Deferred

fun interface OnRequestAdd {
    operator fun invoke(
        items: List<NewDownloadItemProps>,
        queueId: Long?,
        categorySelectionMode: CategorySelectionMode?,
    ): Deferred<List<Long>>
}
