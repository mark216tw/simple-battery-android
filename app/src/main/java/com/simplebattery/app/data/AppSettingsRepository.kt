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

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val hue: Float = DEFAULT_HUE,
) {
    companion object {
        const val DEFAULT_HUE = 157f
    }
}

class AppSettingsRepository(private val context: Context) {
    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
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

    suspend fun setHue(hue: Float) {
        context.settingsDataStore.edit { it[Keys.hue] = hue.coerceIn(0f, 360f) }
    }

    private fun mapSettings(preferences: Preferences): AppSettings {
        val mode = preferences[Keys.themeMode]
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
        return AppSettings(
            themeMode = mode,
            hue = preferences[Keys.hue] ?: AppSettings.DEFAULT_HUE,
        )
    }
}
