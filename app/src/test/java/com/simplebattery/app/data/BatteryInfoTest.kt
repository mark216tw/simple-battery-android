package com.simplebattery.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import android.os.BatteryManager

class BatteryInfoTest {
    @Test
    fun `converts battery level using supplied scale`() {
        assertEquals(50, scaledBatteryLevel(rawLevel = 1, scale = 2))
        assertEquals(85, scaledBatteryLevel(rawLevel = 85, scale = 100))
    }

    @Test
    fun `clamps malformed values to valid percentage`() {
        assertEquals(100, scaledBatteryLevel(rawLevel = 120, scale = 100))
    }

    @Test
    fun `returns null when battery values are unavailable`() {
        assertNull(scaledBatteryLevel(rawLevel = -1, scale = 100))
        assertNull(scaledBatteryLevel(rawLevel = 50, scale = 0))
    }

    @Test
    fun `calculates charging power from voltage and current`() {
        val battery = BatteryInfo(
            level = 50,
            status = BatteryManager.BATTERY_STATUS_CHARGING,
            plugged = BatteryManager.BATTERY_PLUGGED_USB,
            health = BatteryManager.BATTERY_HEALTH_GOOD,
            temperatureCelsius = 30f,
            voltageMillivolts = 5_000,
            currentMicroamps = 2_000_000,
            chargeTimeRemainingMillis = null,
        )

        assertEquals(10f, battery.chargingPowerWatts)
    }
}
