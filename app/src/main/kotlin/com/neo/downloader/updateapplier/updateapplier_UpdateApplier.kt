package com.neo.downloader.updateapplier

import com.neo.downloader.updatechecker.UpdateInfo

interface UpdateApplier {
    fun updateSupported(): Boolean
    suspend fun applyUpdate(updateInfo: UpdateInfo)
    suspend fun cleanup()
}