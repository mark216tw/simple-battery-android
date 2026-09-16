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
import com.simplebattery.app.data.ThemeColorOption
import com.simplebattery.app.data.ThemeMode

fun themeSeed(settings: AppSettings): Color = if (settings.themeColor == ThemeColorOption.CUSTOM) {
    Color.hsv(settings.hue, 0.82f, 0.92f)
} else {
    Color(settings.themeColor.argb)
}

private fun colorHue(color: Color): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return hsv[0]
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
    val seed = themeSeed(settings)
    val hue = colorHue(seed)
    val primary = if (dark) Color.hsv(hue, 0.52f, 0.94f) else seed
    val onPrimary = if (primary.luminance() > 0.179f) Color.Black else Color.White
    val scheme = if (dark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = Color.hsv(hue, 0.40f, 0.34f),
            onPrimaryContainer = Color.hsv(hue, 0.20f, 0.96f),
            background = Color.hsv(hue, 0.18f, 0.10f),
            surface = Color.hsv(hue, 0.14f, 0.11f),
            surfaceVariant = Color.hsv(hue, 0.22f, 0.20f),
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = Color.hsv(hue, 0.18f, 0.96f),
            onPrimaryContainer = Color.hsv(hue, 0.72f, 0.22f),
            background = Color.hsv(hue, 0.07f, 0.985f),
            surface = Color.hsv(hue, 0.04f, 0.995f),
            surfaceVariant = Color.hsv(hue, 0.13f, 0.93f),
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
