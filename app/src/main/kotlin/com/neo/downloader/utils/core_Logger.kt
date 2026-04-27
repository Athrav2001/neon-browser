package com.neo.downloader.utils

import java.util.logging.Logger

inline fun <reified T> T.thisLogger(): Logger {
    return Logger.getLogger(T::class.qualifiedName ?: T::class.simpleName ?: "Unknown")
}
