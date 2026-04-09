package com.neo.downloader.android.pages.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neo.downloader.resources.Res
import com.neo.downloader.shared.ui.widget.Text
import com.neo.downloader.shared.util.FileIconProvider
import com.neo.downloader.shared.util.ui.WithContentAlpha
import com.neo.downloader.shared.util.ui.myColors
import ir.amirab.downloader.monitor.IDownloadItemState
import ir.amirab.util.compose.resources.myStringResource


@Composable
fun DownloadList(
    downloadList: List<IDownloadItemState>,
    selectionList: List<Long>,
    onItemSelectionChange: (Long, Boolean) -> Unit,
    onItemClicked: (IDownloadItemState) -> Unit,
    fileIconProvider: FileIconProvider,
    onNewSelection: (List<Long>) -> Unit,
    lazyListState: LazyListState,
    modifier: Modifier,
    contentPadding: PaddingValues,
) {

    fun newSelection(ids: List<Long>, isSelected: Boolean) {
        onNewSelection(ids.filter { isSelected })
    }

    fun changeAllSelection(isSelected: Boolean) {
        newSelection(downloadList.map { it.id }, isSelected)
    }

    val isInSelectMode = selectionList.isNotEmpty()
    BackHandler(
        isInSelectMode
    ) {
        changeAllSelection(false)
    }
    Box {
        LazyColumn(
            state = lazyListState,
            modifier = modifier,
            contentPadding = contentPadding
        ) {
            itemsIndexed(
                items = downloadList,
                key = { _, item -> item.id }
            ) { _, item ->
                Column(
                    modifier = Modifier
                        .animateItem()
                        .padding(
                            horizontal = 10.dp,
                            vertical = 6.dp,
                        )
                ) {
                    RenderDownloadItem(
                        downloadItem = item,
                        checked = if (isInSelectMode) {
                            item.id in selectionList
                        } else {
                            null
                        },
                        onClick = {
                            if (isInSelectMode) {
                                val wasInSelections = item.id in selectionList
                                onItemSelectionChange(item.id, !wasInSelections)
                            } else {
                                onItemClicked(item)
                            }
                        },
                        onLongClick = {
                            val wasInSelections = item.id in selectionList
                            onItemSelectionChange(item.id, !wasInSelections)
                        },
                        fileIconProvider = fileIconProvider,
                        modifier = Modifier,
                    )
                }
            }
        }
        if (downloadList.isEmpty()) {
            Box(
                Modifier
                    .padding()
                    .fillMaxSize()
            ) {
                WithContentAlpha(0.75f) {
                    Text(
                        myStringResource(Res.string.list_is_empty),
                        Modifier.align(Alignment.Center),
                        maxLines = 1,
                    )
                }
            }
        }
    }

}
