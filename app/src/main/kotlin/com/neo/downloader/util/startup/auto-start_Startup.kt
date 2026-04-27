package com.neo.downloader.util.startup

import android.content.BroadcastReceiver
import android.content.Context

object Startup {
    fun getStartUpManager(
        context: Context,
        bootReceiver: Class<out BroadcastReceiver>,
    ): AndroidStartupManager {
        return AndroidStartupManager(context, bootReceiver)
    }
}
