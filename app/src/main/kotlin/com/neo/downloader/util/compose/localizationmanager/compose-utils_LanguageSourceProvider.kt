package com.neo.downloader.util.compose.localizationmanager

import com.neo.downloader.resources.contracts.MyLanguageResource

/**
 * at the moment we only use bundled strings
 */
class LanguageSourceProvider(
    val defaultLanguageResource: MyLanguageResource,
    val allLanguageResources: List<MyLanguageResource>,
)
