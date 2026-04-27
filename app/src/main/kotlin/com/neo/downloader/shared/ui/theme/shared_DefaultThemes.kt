package com.neo.downloader.shared.ui.theme

import androidx.compose.ui.graphics.Color
import com.neo.downloader.shared.util.ui.MyColors

object DefaultThemes {
    val dark = MyColors(
        id = "dark",
        name = "Amoled Neon",
        primary = Color(0xFFD154FF),
        primaryVariant = Color(0xFFB84CFF),
        onPrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFF7A5CFF),
        secondaryVariant = Color(0xFF9A7DFF),
        onSecondary = Color(0xFFFFFFFF),
        background = Color(0xFF000000),
        onBackground = Color(0xFFF2F2F2),
        surface = Color(0xFF0F0A14),
        onSurface = Color(0xFFECE7F6),
        error = Color(0xFFEA4C3C),
        onError = Color(0xFFFFFFFF),
        success = Color(0xFF45B36B),
        onSuccess = Color(0xFFFFFFFF),
        warning = Color(0xFFF6C244),
        onWarning = Color(0xFF141414),
        info = Color(0xFF41B8FF),
        onInfo = Color(0xFF111111),
        isLight = false
    )

    val light = MyColors(
        id = "light",
        name = "Light Mono",
        primary = Color(0xFF111111),
        primaryVariant = Color(0xFF000000),
        onPrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFF2A2A2A),
        secondaryVariant = Color(0xFF3A3A3A),
        onSecondary = Color(0xFFFFFFFF),
        background = Color(0xFFFFFFFF),
        onBackground = Color(0xFF121212),
        surface = Color(0xFFF6F6F8),
        onSurface = Color(0xFF1A1A1A),
        error = Color(0xFFEA4C3C),
        onError = Color(0xFFFFFFFF),
        success = Color(0xFF45B36B),
        onSuccess = Color(0xFFFFFFFF),
        warning = Color(0xFFF6C244),
        onWarning = Color(0xFF111111),
        info = Color(0xFF2677D9),
        onInfo = Color(0xFFFFFFFF),
        isLight = true
    )


    fun getAll(): List<MyColors> {
        return listOf(
            dark,
            light,
        )
    }

    fun getDefaultDark() = dark
    fun getDefaultLight() = light
}
