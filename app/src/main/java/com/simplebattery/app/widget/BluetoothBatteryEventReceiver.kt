package com.simplebattery.app.widget

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BluetoothBatteryEventReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        } ?: return
        val address = runCatching { device.address }.getOrNull() ?: return
        val cache = BluetoothBatteryCache(context)
        when (intent.action) {
            ACTION_BATTERY_LEVEL_CHANGED -> {
                val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
                if (level in 0..100) cache.saveLevel(address, level)
            }
            BluetoothDevice.ACTION_ACL_CONNECTED -> cache.setConnected(address, true)
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> cache.setConnected(address, false)
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                BluetoothBatteryWidgetProvider.updateAll(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_BATTERY_LEVEL_CHANGED =
            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"
    }
}
