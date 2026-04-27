package com.neo.downloader.utils

import java.io.File

interface IDiskStat {
    fun getRemainingSpace(path: File): Long
}
