package com.neo.downloader.shared.util

import com.neo.downloader.shared.storage.BaseAppSettingsStorage
import com.neo.downloader.connection.UserAgentProvider

class UserAgentProviderFromSettings(
    private val appSettingsStorage: BaseAppSettingsStorage
) : UserAgentProvider {
    override fun getUserAgent(): String? {
        return appSettingsStorage.userAgent.value.takeIf { it.isNotBlank() }
    }
}
