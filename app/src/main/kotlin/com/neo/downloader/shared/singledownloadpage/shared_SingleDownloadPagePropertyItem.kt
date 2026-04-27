package com.neo.downloader.shared.singledownloadpage

import androidx.compose.runtime.Immutable
import com.neo.downloader.util.compose.StringSource

@Immutable
data class SingleDownloadPagePropertyItem(
    val name: StringSource,
    val value: StringSource,
    val valueState: ValueType = ValueType.Normal,
) {
    enum class ValueType { Normal, Error, Success }
}
