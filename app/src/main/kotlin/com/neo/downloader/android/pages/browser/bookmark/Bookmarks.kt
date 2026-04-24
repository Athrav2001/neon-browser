package com.neo.downloader.android.pages.browser.bookmark

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neo.downloader.android.storage.BrowserBookmark
import com.neo.downloader.android.ui.SheetHeader
import com.neo.downloader.android.ui.SheetTitle
import com.neo.downloader.android.ui.SheetUI
import com.neo.downloader.resources.Res
import com.neo.downloader.shared.ui.widget.Text
import com.neo.downloader.shared.ui.widget.TransparentIconActionButton
import com.neo.downloader.shared.util.ResponsiveDialog
import com.neo.downloader.shared.util.div
import com.neo.downloader.shared.util.rememberResponsiveDialogState
import com.neo.downloader.shared.util.ui.LocalContentColor
import com.neo.downloader.shared.util.ui.WithContentAlpha
import com.neo.downloader.shared.util.ui.icon.MyIcons
import com.neo.downloader.shared.util.ui.theme.mySpacings
import com.neo.downloader.shared.util.ui.theme.myTextSizes
import ir.amirab.util.compose.asStringSource
import ir.amirab.util.compose.resources.myStringResource

@Composable
fun BookmarkList(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    bookmarks: List<BrowserBookmark>,
    onRequestNewBookmark: () -> Unit,
    onRequestEditBookmark: (BrowserBookmark) -> Unit,
    onBookmarkClick: (BrowserBookmark) -> Unit,
    onRemoveBookmarkRequest: (BrowserBookmark) -> Unit,
) {
    val responsiveState = rememberResponsiveDialogState(visible)
    LaunchedEffect(visible) {
        if (visible) {
            responsiveState.show()
        } else {
            responsiveState.hide()
        }
    }
    ResponsiveDialog(
        state = responsiveState,
        onDismiss = onDismissRequest,
    ) {
        SheetUI(
            header = {
                SheetHeader(
                    headerTitle = {
                        SheetTitle(
                            myStringResource(Res.string.browser_bookmarks),
                        )
                    },
                    headerActions = {
                        TransparentIconActionButton(
                            MyIcons.add,
                            Res.string.add.asStringSource(),
                        ) {
                            onRequestNewBookmark()
                        }
                        TransparentIconActionButton(
                            MyIcons.close,
                            Res.string.close.asStringSource(),
                        ) {
                            onDismissRequest()
                        }
                    }
                )
            }
        ) {
            LazyColumn {
                items(bookmarks) { bookmark ->
                    Row(
                        modifier = Modifier
                            .heightIn(mySpacings.thumbSize)
                            .combinedClickable(
                                onLongClick = { onRequestEditBookmark(bookmark) },
                                onClick = { onBookmarkClick(bookmark) }
                            )
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BookmarkFavicon(
                            bookmark = bookmark,
                            size = 24.dp,
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f),
                        ) {
                            Text(
                                text = bookmark.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = bookmark.url,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = myTextSizes.sm,
                                color = LocalContentColor.current / 0.75f
                            )
                        }

                        WithContentAlpha(0.5f) {
                            TransparentIconActionButton(
                                MyIcons.remove,
                                Res.string.remove.asStringSource(),
                            ) {
                                onRemoveBookmarkRequest(bookmark)
                            }
                        }
                    }
                }
            }
        }
    }
}
