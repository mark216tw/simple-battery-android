package com.simplebattery.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class ThemeColorOption(val label: String, val argb: Long) {
    WARM_YELLOW("暖陽黃", 0xFF765B00),
    CORAL_RED("珊瑚紅", 0xFFA63C4A),
    VIBRANT_ORANGE("活力橘", 0xFF974700),
    GRASS_GREEN("青草綠", 0xFF386A20),
    SKY_BLUE("天空藍", 0xFF00658B),
    GRAPE_PURPLE("葡萄紫", 0xFF6555C7),
    CUSTOM("自訂色彩", 0),
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeColor: ThemeColorOption = ThemeColorOption.GRASS_GREEN,
    val hue: Float = DEFAULT_HUE,
) {
    companion object {
        const val DEFAULT_HUE = 157f
    }
}

class AppSettingsRepository(private val context: Context) {
    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val themeColor = stringPreferencesKey("theme_color")
        val hue = floatPreferencesKey("theme_hue")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map(::mapSettings)

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.themeMode] = mode.name }
    }

    suspend fun setThemeColor(color: ThemeColorOption) {
        require(color != ThemeColorOption.CUSTOM)
        context.settingsDataStore.edit { it[Keys.themeColor] = color.name }
    }

    suspend fun setCustomHue(hue: Float) {
        context.settingsDataStore.edit {
            it[Keys.themeColor] = ThemeColorOption.CUSTOM.name
            it[Keys.hue] = hue.coerceIn(0f, 360f)
        }
    }

    private fun mapSettings(preferences: Preferences): AppSettings {
        val mode = preferences[Keys.themeMode]
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
        val color = preferences[Keys.themeColor]
            ?.let { runCatching { ThemeColorOption.valueOf(it) }.getOrNull() }
            ?: if (preferences[Keys.hue] != null) {
                ThemeColorOption.CUSTOM
            } else {
                ThemeColorOption.GRASS_GREEN
            }
        return AppSettings(
            themeMode = mode,
            themeColor = color,
            hue = preferences[Keys.hue] ?: AppSettings.DEFAULT_HUE,
        )
    }
}
