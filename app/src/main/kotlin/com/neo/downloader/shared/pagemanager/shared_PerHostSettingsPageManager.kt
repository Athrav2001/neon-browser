package com.neo.downloader.shared.pagemanager

interface PerHostSettingsPageManager {
    fun openPerHostSettings(openedHost: String?)
    fun closePerHostSettings()
}
