package com.simplebattery.app.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetFormattingTest {
    @Test
    fun `formats battery level with optional percent symbol`() {
        assertEquals("85%", batteryLevelText(85, showPercent = true))
        assertEquals("85", batteryLevelText(85, showPercent = false))
        assertEquals("100%", batteryLevelText(120, showPercent = true))
    }

    @Test
    fun `normalizes temperature to zero through fifty degrees`() {
        assertEquals(0f, temperatureProgress(-5f))
        assertEquals(0.5f, temperatureProgress(25f))
        assertEquals(1f, temperatureProgress(55f))
        assertNull(temperatureProgress(null))
    }
}
