package com.simplebattery.app

import android.content.Context
import android.content.Intent
import android.provider.Settings

object SystemSettingsIntents {
    const val DESTINATION_BATTERY = "battery"
    const val DESTINATION_BLUETOOTH = "bluetooth"
    private const val ACTION_POWER_USAGE_SUMMARY = "android.intent.action.POWER_USAGE_SUMMARY"

    fun createIntent(context: Context, destination: String): Intent {
        val candidates = when (destination) {
            DESTINATION_BLUETOOTH -> listOf(
                Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
                Intent(Settings.ACTION_SETTINGS),
            )
            else -> listOf(
                Intent(ACTION_POWER_USAGE_SUMMARY),
                Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS),
                Intent(Settings.ACTION_SETTINGS),
            )
        }
        candidates.forEach { candidate ->
            candidate.resolveActivity(context.packageManager)?.let { component ->
                return candidate.apply {
                    this.component = component
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            }
        }
        return Intent(Settings.ACTION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }
}
