package com.simplebattery.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp

@Composable
fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
    onHueChangeFinished: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var displayedHue by remember { mutableFloatStateOf(hue) }
    var isDragging by remember { mutableStateOf(false) }
    val currentHue by rememberUpdatedState(hue)
    val currentOnHueChange by rememberUpdatedState(onHueChange)
    val currentOnHueChangeFinished by rememberUpdatedState(onHueChangeFinished)

    LaunchedEffect(hue, isDragging) {
        if (!isDragging) displayedHue = hue
    }

    val colors = (0..12).map { Color.hsv(it * 30f, 0.82f, 0.92f) }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(displayedHue, 0f..360f)
                setProgress { value ->
                    val updatedHue = value.coerceIn(0f, 360f)
                    displayedHue = updatedHue
                    currentOnHueChange(updatedHue)
                    currentOnHueChangeFinished(updatedHue)
                    true
                }
            }
            .pointerInput(Unit) {
                fun updateHue(x: Float) {
                    displayedHue = (x / size.width * 360f).coerceIn(0f, 360f)
                    currentOnHueChange(displayedHue)
                }
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        updateHue(offset.x)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        updateHue(change.position.x)
                    },
                    onDragEnd = {
                        isDragging = false
                        currentOnHueChangeFinished(displayedHue)
                    },
                    onDragCancel = {
                        isDragging = false
                        displayedHue = currentHue
                        currentOnHueChange(currentHue)
                    },
                )
            },
    ) {
        val trackHeight = 18.dp.toPx()
        val top = (size.height - trackHeight) / 2f
        drawRoundRect(
            brush = Brush.horizontalGradient(colors),
            topLeft = Offset(0f, top),
            size = androidx.compose.ui.geometry.Size(size.width, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2f),
        )
        val thumbX = size.width * (displayedHue.coerceIn(0f, 360f) / 360f)
        drawCircle(Color.White, radius = 12.dp.toPx(), center = Offset(thumbX, size.height / 2f))
        drawCircle(
            color = Color(0xFF25302C),
            radius = 12.dp.toPx(),
            center = Offset(thumbX, size.height / 2f),
            style = Stroke(width = 2.dp.toPx()),
        )
        drawCircle(
            color = Color.hsv(displayedHue, 0.82f, 0.92f),
            radius = 7.dp.toPx(),
            center = Offset(thumbX, size.height / 2f),
        )
    }
}
