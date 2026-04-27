package com.neo.downloader.shared.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.getSystemService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


object ClipboardUtil : KoinComponent {
    private val context: Context by inject()
    private val clipboardManager get() = context.getSystemService<ClipboardManager>()
    fun copy(text: String) {
        runCatching {
            clipboardManager?.let {
                val clip = ClipData.newPlainText("Copied Text", text)
                it.setPrimaryClip(clip)
            }
        }
    }

    fun read(): String? {
        return runCatching {
            clipboardManager?.let {
                val clip: ClipData? = it.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    return clip.getItemAt(0).coerceToText(context)
                        .toString()
                }
                return null
            }
        }.getOrNull()
    }
}
