package com.neo.downloader.shared.util.ui.widget

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
fun MPBackHandler(isEnabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = isEnabled, onBack = onBack)
}
