package com.simplebattery.app

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import androidx.core.content.ContextCompat
import com.simplebattery.app.widget.BluetoothBatteryEventReceiver

class SimpleBatteryApplication : Application() {
    private val bluetoothBatteryReceiver = BluetoothBatteryEventReceiver()

    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(
            this,
            bluetoothBatteryReceiver,
            IntentFilter(BluetoothBatteryEventReceiver.ACTION_BATTERY_LEVEL_CHANGED),
            ContextCompat.RECEIVER_EXPORTED,
        )
        registerBatteryShortcut()
    }

    private fun registerBatteryShortcut() {
        val shortcutManager = getSystemService(ShortcutManager::class.java) ?: return
        val shortcut = ShortcutInfo.Builder(this, BATTERY_SHORTCUT_ID)
            .setShortLabel(getString(R.string.shortcut_battery))
            .setLongLabel(getString(R.string.shortcut_battery))
            .setIcon(Icon.createWithResource(this, R.drawable.ic_battery_shortcut))
            .setIntent(
                SystemSettingsIntents.createIntent(
                    this,
                    SystemSettingsIntents.DESTINATION_BATTERY,
                ).setAction(Intent.ACTION_VIEW),
            )
            .build()
        shortcutManager.dynamicShortcuts = listOf(shortcut)
    }

    companion object {
        private const val BATTERY_SHORTCUT_ID = "battery_settings"
    }
}
