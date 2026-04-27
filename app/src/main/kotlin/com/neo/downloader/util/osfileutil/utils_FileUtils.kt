package com.neo.downloader.util.osfileutil

fun getPlatformFileUtil(): FileUtils {
    return AndroidFileUtil()
}
