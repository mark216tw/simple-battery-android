package com.simplebattery.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import com.simplebattery.app.data.AppSettings
import com.simplebattery.app.data.ThemeMode

fun themePrimary(hue: Float, dark: Boolean): Color = if (dark) {
    Color.hsv(hue, 0.52f, 0.94f)
} else {
    Color.hsv(hue, 0.58f, 0.72f)
}

@Composable
fun SimpleBatteryTheme(
    settings: AppSettings,
    activity: Activity,
    content: @Composable () -> Unit,
) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val primary = themePrimary(settings.hue, dark)
    val onPrimary = if (primary.luminance() > 0.179f) Color.Black else Color.White
    val scheme = if (dark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = Color.hsv(settings.hue, 0.40f, 0.34f),
            onPrimaryContainer = Color.hsv(settings.hue, 0.20f, 0.96f),
            background = Color(0xFF101513),
            surface = Color(0xFF101513),
            surfaceVariant = Color(0xFF29322F),
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = Color.hsv(settings.hue, 0.18f, 0.96f),
            onPrimaryContainer = Color.hsv(settings.hue, 0.72f, 0.22f),
            background = Color(0xFFF7FAF8),
            surface = Color(0xFFF7FAF8),
            surfaceVariant = Color(0xFFE3EBE7),
        )
    }

    SideEffect {
        activity.window.statusBarColor = Color.Transparent.toArgb()
        activity.window.navigationBarColor = Color.Transparent.toArgb()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity.window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }

    MaterialTheme(colorScheme = scheme, content = content)
}
