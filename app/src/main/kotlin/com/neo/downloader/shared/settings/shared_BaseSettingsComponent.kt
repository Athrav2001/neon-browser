package com.neo.downloader.shared.settings

import com.neo.downloader.shared.ui.configurable.ConfigurableGroup
import com.neo.downloader.shared.util.BaseComponent
import com.neo.downloader.shared.util.mvi.ContainsEffects
import com.neo.downloader.shared.util.mvi.supportEffects
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.StateFlow

abstract class BaseSettingsComponent(
    context: ComponentContext
) : BaseComponent(
    context
),
    ContainsEffects<BaseSettingsComponent.Effects> by supportEffects() {
    abstract val configurables: StateFlow<List<ConfigurableGroup>>

    sealed interface Effects {
        interface Platform : Effects
    }
}
