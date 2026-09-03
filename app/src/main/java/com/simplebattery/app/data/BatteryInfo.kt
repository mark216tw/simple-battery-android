package com.simplebattery.app.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import kotlin.math.abs

data class BatteryInfo(
    val level: Int,
    val status: Int,
    val plugged: Int,
    val health: Int,
    val temperatureCelsius: Float?,
    val voltageMillivolts: Int?,
    val currentMicroamps: Int?,
    val chargeTimeRemainingMillis: Long?,
) {
    val isCharging: Boolean
        get() = status == BatteryManager.BATTERY_STATUS_CHARGING

    val isFull: Boolean
        get() = status == BatteryManager.BATTERY_STATUS_FULL

    companion object {
        fun read(context: Context, intent: Intent? = null): BatteryInfo {
            val batteryIntent = intent ?: context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            )
            val rawLevel = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val level = scaledBatteryLevel(rawLevel, scale) ?: run {
                val manager = context.getSystemService(BatteryManager::class.java)
                manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    ?.coerceIn(0, 100) ?: 0
            }
            val rawTemperature = batteryIntent?.getIntExtra(
                BatteryManager.EXTRA_TEMPERATURE,
                Int.MIN_VALUE,
            ) ?: Int.MIN_VALUE
            val rawVoltage = batteryIntent?.getIntExtra(
                BatteryManager.EXTRA_VOLTAGE,
                Int.MIN_VALUE,
            ) ?: Int.MIN_VALUE
            val manager = context.getSystemService(BatteryManager::class.java)
            val rawCurrent = manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                ?: Int.MIN_VALUE
            val chargeTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                manager?.computeChargeTimeRemaining() ?: -1L
            } else {
                -1L
            }
            return BatteryInfo(
                level = level,
                status = batteryIntent?.getIntExtra(
                    BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN,
                ) ?: BatteryManager.BATTERY_STATUS_UNKNOWN,
                plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0,
                health = batteryIntent?.getIntExtra(
                    BatteryManager.EXTRA_HEALTH,
                    BatteryManager.BATTERY_HEALTH_UNKNOWN,
                ) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN,
                temperatureCelsius = rawTemperature
                    .takeUnless { it == Int.MIN_VALUE }
                    ?.div(10f),
                voltageMillivolts = rawVoltage.takeUnless { it == Int.MIN_VALUE },
                currentMicroamps = rawCurrent
                    .takeUnless { it == Int.MIN_VALUE || it == 0 }
                    ?.let(::abs),
                chargeTimeRemainingMillis = chargeTime.takeIf { it > 0L },
            )
        }
    }

    val chargingPowerWatts: Float?
        get() = if (isCharging) {
            val current = currentMicroamps ?: return null
            val voltage = voltageMillivolts ?: return null
            current.toDouble().times(voltage).div(1_000_000_000.0).toFloat()
        } else {
            null
        }
}

internal fun scaledBatteryLevel(rawLevel: Int, scale: Int): Int? =
    if (rawLevel >= 0 && scale > 0) {
        (rawLevel * 100f / scale).toInt().coerceIn(0, 100)
    } else {
        null
    }
