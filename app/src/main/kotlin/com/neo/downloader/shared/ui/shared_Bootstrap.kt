package com.neo.downloader.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.neo.downloader.shared.repository.BaseAppRepository
import com.neo.downloader.shared.storage.BaseAppSettingsStorage
import com.neo.downloader.shared.ui.configurable.ConfigurableRendererRegistry
import com.neo.downloader.shared.ui.configurable.LocalConfigurationRendererRegistry
import com.neo.downloader.shared.util.LocalUseRelativeDateTime
import com.neo.downloader.shared.util.ProvideSizeAndSpeedUnit
import com.neo.downloader.util.compose.IIconResolver
import com.neo.downloader.util.compose.LocalIconFromUriResolver


@Composable
fun ProvideCommonSettings(
    appSettings: BaseAppSettingsStorage,
    iconProvider: IIconResolver,
    configurableRendererRegistry: ConfigurableRendererRegistry,
    content: @Composable () -> Unit,
) {
    val useNativeDateTime by appSettings.useRelativeDateTime.collectAsState()
    CompositionLocalProvider(
        LocalUseRelativeDateTime provides useNativeDateTime,
        LocalIconFromUriResolver provides iconProvider,
        LocalConfigurationRendererRegistry provides configurableRendererRegistry,
    ) {
        content()
    }
}

@Composable
fun ProvideSizeUnits(
    appRepository: BaseAppRepository,
    content: @Composable () -> Unit,
) {
    ProvideSizeAndSpeedUnit(
        sizeUnitConfig = appRepository.sizeUnit.collectAsState().value,
        speedUnitConfig = appRepository.speedUnit.collectAsState().value,
        content = content
    )
}
