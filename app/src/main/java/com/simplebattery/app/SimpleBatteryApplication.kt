package com.simplebattery.app

import android.app.Application
import android.content.IntentFilter
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
    }
}
