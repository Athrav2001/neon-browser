package com.neo.downloader.shared.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neo.downloader.shared.util.ui.myColors
import com.neo.downloader.shared.util.div

@Composable
fun ActionContainer(
    modifier: Modifier,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = 16.dp,
        vertical = 8.dp,
    ),
    content: @Composable () -> Unit,
) {
    Column(modifier) {
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(myColors.onBackground / 0.15f)
        )
        Box(
            Modifier
                .fillMaxWidth()
                .background(myColors.surface / 0.5f)
                .padding(contentPadding),
        ) {
            content()
        }
    }
}
