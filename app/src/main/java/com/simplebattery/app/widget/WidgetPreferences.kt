package com.simplebattery.app.widget

import android.content.Context

enum class WidgetBackground {
    THEME_COLOR,
    TRANSPARENT,
}

enum class WidgetFrame {
    NONE,
    CIRCLE,
    BATTERY,
}

enum class WidgetType {
    PHONE_BATTERY,
    BATTERY_TEMPERATURE,
    BLUETOOTH_BATTERY,
}

data class WidgetSettings(
    val hue: Float = DEFAULT_HUE,
    val fontSizeSp: Float = DEFAULT_FONT_SIZE_SP,
    val background: WidgetBackground = WidgetBackground.THEME_COLOR,
    val frame: WidgetFrame = WidgetFrame.BATTERY,
    val type: WidgetType = WidgetType.PHONE_BATTERY,
    val showPercent: Boolean = true,
    val bluetoothAddress: String? = null,
    val showDeviceName: Boolean = true,
) {
    companion object {
        const val DEFAULT_HUE = 157f
        const val DEFAULT_FONT_SIZE_SP = 40f
    }
}

class WidgetPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("widget_settings", Context.MODE_PRIVATE)

    fun get(appWidgetId: Int): WidgetSettings = WidgetSettings(
        hue = preferences.getFloat(key(appWidgetId, "hue"), WidgetSettings.DEFAULT_HUE),
        fontSizeSp = preferences.getFloat(
            key(appWidgetId, "font_size"),
            WidgetSettings.DEFAULT_FONT_SIZE_SP,
        ),
        background = preferences.getString(key(appWidgetId, "background"), null)
            ?.let { runCatching { WidgetBackground.valueOf(it) }.getOrNull() }
            ?: WidgetBackground.THEME_COLOR,
        frame = enumValue(
            preferences.getString(key(appWidgetId, "frame"), null),
            WidgetFrame.BATTERY,
        ),
        type = enumValue(
            preferences.getString(key(appWidgetId, "type"), null),
            WidgetType.PHONE_BATTERY,
        ),
        showPercent = preferences.getBoolean(key(appWidgetId, "show_percent"), true),
        bluetoothAddress = preferences.getString(key(appWidgetId, "bluetooth_address"), null),
        showDeviceName = preferences.getBoolean(key(appWidgetId, "show_device_name"), true),
    )

    fun save(appWidgetId: Int, settings: WidgetSettings) {
        preferences.edit()
            .putFloat(key(appWidgetId, "hue"), settings.hue.coerceIn(0f, 360f))
            .putFloat(key(appWidgetId, "font_size"), settings.fontSizeSp.coerceIn(28f, 52f))
            .putString(key(appWidgetId, "background"), settings.background.name)
            .putString(key(appWidgetId, "frame"), settings.frame.name)
            .putString(key(appWidgetId, "type"), settings.type.name)
            .putBoolean(key(appWidgetId, "show_percent"), settings.showPercent)
            .putString(key(appWidgetId, "bluetooth_address"), settings.bluetoothAddress)
            .putBoolean(key(appWidgetId, "show_device_name"), settings.showDeviceName)
            .apply()
    }

    fun delete(appWidgetId: Int) {
        preferences.edit()
            .remove(key(appWidgetId, "hue"))
            .remove(key(appWidgetId, "font_size"))
            .remove(key(appWidgetId, "background"))
            .remove(key(appWidgetId, "frame"))
            .remove(key(appWidgetId, "type"))
            .remove(key(appWidgetId, "show_percent"))
            .remove(key(appWidgetId, "bluetooth_address"))
            .remove(key(appWidgetId, "show_device_name"))
            .apply()
    }

    private fun key(appWidgetId: Int, name: String) = "widget_${appWidgetId}_$name"

    private inline fun <reified T : Enum<T>> enumValue(value: String?, fallback: T): T =
        value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback
}

internal fun batteryLevelText(level: Int, showPercent: Boolean): String =
    if (showPercent) "${level.coerceIn(0, 100)}%" else "${level.coerceIn(0, 100)}"

internal fun temperatureProgress(temperatureCelsius: Float?): Float? =
    temperatureCelsius?.div(50f)?.coerceIn(0f, 1f)
