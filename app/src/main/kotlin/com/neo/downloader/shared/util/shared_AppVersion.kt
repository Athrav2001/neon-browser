package com.neo.downloader.shared.util

import com.neo.downloader.shared.BuildConfig
import io.github.z4kn4fein.semver.Version

object AppVersion {
    private val currentVersion = Version.parse(BuildConfig.APP_VERSION)
    fun get() = currentVersion
}
