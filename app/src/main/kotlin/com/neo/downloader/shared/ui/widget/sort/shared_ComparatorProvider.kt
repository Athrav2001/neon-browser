package com.neo.downloader.shared.ui.widget.sort

interface ComparatorProvider<T> {
    fun comparator(): Comparator<T>
}
