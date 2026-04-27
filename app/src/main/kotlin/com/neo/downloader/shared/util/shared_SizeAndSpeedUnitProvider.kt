package com.neo.downloader.shared.util

import com.neo.downloader.util.datasize.ConvertSizeConfig
import kotlinx.coroutines.flow.StateFlow

interface SizeAndSpeedUnitProvider {
    val sizeUnit: StateFlow<ConvertSizeConfig>
    val speedUnit: StateFlow<ConvertSizeConfig>
}
