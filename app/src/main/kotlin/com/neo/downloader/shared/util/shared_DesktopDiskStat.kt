package com.neo.downloader.shared.util

import com.neo.downloader.utils.IDiskStat
import java.io.File

typealias PlatformDiskStat = AndroidDiskStat

class AndroidDiskStat : IDiskStat {
    override fun getRemainingSpace(path: File): Long {
        return path.freeSpace
    }
}
