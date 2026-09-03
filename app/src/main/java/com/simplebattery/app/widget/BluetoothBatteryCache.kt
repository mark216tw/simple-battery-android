package com.simplebattery.app.widget

import android.content.Context

class BluetoothBatteryCache(context: Context) {
    private val preferences = context.getSharedPreferences(
        "bluetooth_battery_cache",
        Context.MODE_PRIVATE,
    )

    fun getLevel(address: String): Int? {
        val key = levelKey(address)
        return if (preferences.contains(key)) preferences.getInt(key, -1).takeIf { it in 0..100 }
        else null
    }

    fun saveLevel(address: String, level: Int) {
        preferences.edit()
            .putInt(levelKey(address), level.coerceIn(0, 100))
            .putBoolean(connectedKey(address), true)
            .apply()
    }

    fun setConnected(address: String, connected: Boolean) {
        preferences.edit().putBoolean(connectedKey(address), connected).apply()
    }

    fun isKnownDisconnected(address: String): Boolean =
        preferences.contains(connectedKey(address)) && !preferences.getBoolean(connectedKey(address), false)

    private fun levelKey(address: String) = "level_$address"
    private fun connectedKey(address: String) = "connected_$address"
}
