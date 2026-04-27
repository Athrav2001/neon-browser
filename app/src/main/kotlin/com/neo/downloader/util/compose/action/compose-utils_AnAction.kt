package com.neo.downloader.util.compose.action

import com.neo.downloader.util.compose.IconSource
import com.neo.downloader.util.compose.StringSource

abstract class AnAction(
    title: StringSource,
    icon: IconSource? = null,
) : MenuItem.SingleItem(
    title = title,
    icon = icon,
) {
    override fun onClick() = actionPerformed()

    abstract fun actionPerformed()
}


