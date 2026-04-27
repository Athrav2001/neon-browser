package com.neo.downloader.shared.util

import kotlinx.coroutines.flow.StateFlow

interface ShouldValidate {
    val valid: StateFlow<Boolean>
}
