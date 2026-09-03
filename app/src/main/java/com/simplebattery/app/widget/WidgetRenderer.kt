package com.simplebattery.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.TypedValue
import androidx.core.graphics.ColorUtils
import kotlin.math.max
import kotlin.math.min

enum class WidgetIcon {
    BATTERY,
    LIGHTNING,
    THERMOMETER,
    BLUETOOTH,
}

data class WidgetContent(
    val text: String,
    val progress: Float?,
    val icon: WidgetIcon,
    val secondaryText: String? = null,
    val warning: Boolean = false,
)

object WidgetRenderer {
    fun render(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        content: WidgetContent,
        settings: WidgetSettings,
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val scaledDensity = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            1f,
            context.resources.displayMetrics,
        )
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 72)
            .coerceAtLeast(40)
        val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 72)
            .coerceAtLeast(40)
        val requestedWidth = (widthDp * density).toInt().coerceAtLeast(1)
        val requestedHeight = (heightDp * density).toInt().coerceAtLeast(1)
        val bitmapScale = min(1f, MAX_BITMAP_SIDE_PX / max(requestedWidth, requestedHeight))
        val width = (requestedWidth * bitmapScale).toInt().coerceAtLeast(1)
        val height = (requestedHeight * bitmapScale).toInt().coerceAtLeast(1)
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            val dark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
            drawArtwork(
                canvas = Canvas(bitmap),
                width = width.toFloat(),
                height = height.toFloat(),
                density = density * bitmapScale,
                scaledDensity = scaledDensity * bitmapScale,
                content = content,
                settings = settings,
                dark = dark,
            )
        }
    }

    internal fun drawArtwork(
        canvas: Canvas,
        width: Float,
        height: Float,
        density: Float,
        scaledDensity: Float,
        content: WidgetContent,
        settings: WidgetSettings,
        dark: Boolean,
    ) {
        val primary = if (content.warning) WARNING_COLOR else colorForHue(settings.hue, dark)
        val themed = settings.background == WidgetBackground.THEME_COLOR
        val backdrop = if (themed) {
            ColorUtils.blendARGB(primary, if (dark) Color.BLACK else Color.WHITE, 0.22f)
        } else {
            Color.TRANSPARENT
        }
        if (themed) {
            canvas.drawRoundRect(
                RectF(0f, 0f, width, height),
                min(width, height) * 0.18f,
                min(width, height) * 0.18f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = backdrop },
            )
        }

        val margin = max(4f * density, min(width, height) * 0.07f)
        val area = RectF(margin, margin, width - margin, height - margin)
        val foreground = if (themed) highContrastColor(backdrop) else primary
        val outline = if (ColorUtils.calculateLuminance(foreground) > 0.179) {
            Color.BLACK
        } else {
            Color.WHITE
        }
        val frameRect = when (settings.frame) {
            WidgetFrame.NONE -> area
            WidgetFrame.CIRCLE -> drawCircleFrame(canvas, area, primary, content.progress, density)
            WidgetFrame.BATTERY -> drawBatteryFrame(canvas, area, primary, content.progress, density)
        }
        drawContent(
            canvas = canvas,
            rect = frameRect,
            density = density,
            scaledDensity = scaledDensity,
            content = content,
            settings = settings,
            color = foreground,
            outlineColor = outline,
            outlined = !themed,
        )
    }

    fun colorForHue(hue: Float, dark: Boolean): Int = Color.HSVToColor(
        floatArrayOf(hue.coerceIn(0f, 360f), if (dark) 0.78f else 0.88f, if (dark) 1f else 0.92f),
    )

    private fun drawCircleFrame(
        canvas: Canvas,
        area: RectF,
        color: Int,
        progress: Float?,
        density: Float,
    ): RectF {
        val side = min(area.width(), area.height())
        val rect = RectF(
            area.centerX() - side / 2f,
            area.centerY() - side / 2f,
            area.centerX() + side / 2f,
            area.centerY() + side / 2f,
        )
        val stroke = max(3f * density, side * 0.065f)
        val arcRect = RectF(rect).apply { inset(stroke / 2f, stroke / 2f) }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
        }
        paint.color = ColorUtils.setAlphaComponent(color, 55)
        canvas.drawOval(arcRect, paint)
        paint.color = color
        canvas.drawArc(arcRect, -90f, 360f * (progress ?: 1f).coerceIn(0f, 1f), false, paint)
        return RectF(rect).apply { inset(stroke * 1.35f, stroke * 1.35f) }
    }

    private fun drawBatteryFrame(
        canvas: Canvas,
        area: RectF,
        color: Int,
        progress: Float?,
        density: Float,
    ): RectF {
        val ratio = 0.66f
        val shapeWidth = min(area.width(), area.height() * ratio)
        val shapeHeight = min(area.height(), shapeWidth / ratio)
        val left = area.centerX() - shapeWidth / 2f
        val top = area.centerY() - shapeHeight / 2f
        val terminalHeight = shapeHeight * 0.07f
        val gap = shapeHeight * 0.018f
        val body = RectF(left, top + terminalHeight + gap, left + shapeWidth, top + shapeHeight)
        val terminalWidth = shapeWidth * 0.34f
        val terminal = RectF(
            area.centerX() - terminalWidth / 2f,
            top,
            area.centerX() + terminalWidth / 2f,
            top + terminalHeight,
        )
        val stroke = max(2f * density, shapeWidth * 0.045f)
        val radius = shapeWidth * 0.15f
        val bodyPath = Path().apply { addRoundRect(body, radius, radius, Path.Direction.CW) }
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = ColorUtils.setAlphaComponent(color, 35)
        }
        canvas.drawPath(bodyPath, trackPaint)
        progress?.let {
            canvas.save()
            canvas.clipPath(bodyPath)
            val fillTop = body.bottom - body.height() * it.coerceIn(0f, 1f)
            canvas.drawRect(body.left, fillTop, body.right, body.bottom, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = ColorUtils.setAlphaComponent(color, 145)
            })
            canvas.restore()
        }
        val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            this.color = color
        }
        canvas.drawPath(bodyPath, outlinePaint)
        canvas.drawRoundRect(terminal, terminalHeight * 0.3f, terminalHeight * 0.3f, outlinePaint)
        return RectF(body).apply { inset(stroke * 1.8f, stroke * 1.8f) }
    }

    private fun drawContent(
        canvas: Canvas,
        rect: RectF,
        density: Float,
        scaledDensity: Float,
        content: WidgetContent,
        settings: WidgetSettings,
        color: Int,
        outlineColor: Int,
        outlined: Boolean,
    ) {
        val iconSize = min(14f * scaledDensity, min(rect.width() * 0.26f, rect.height() * 0.18f))
        val secondarySize = if (content.secondaryText != null) min(11f * scaledDensity, rect.height() * 0.12f) else 0f
        val gap = 2f * density
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = settings.fontSizeSp * scaledDensity
        }
        val availableTextHeight = (rect.height() - iconSize - secondarySize - gap * 2f).coerceAtLeast(1f)
        val initialMetrics = textPaint.fontMetrics
        val textScale = min(
            rect.width() / textPaint.measureText(content.text).coerceAtLeast(1f),
            availableTextHeight / (initialMetrics.descent - initialMetrics.ascent),
        ).coerceAtMost(1f)
        textPaint.textSize *= textScale
        val metrics = textPaint.fontMetrics
        val textHeight = metrics.descent - metrics.ascent
        val groupHeight = iconSize + gap + textHeight + if (secondarySize > 0f) gap + secondarySize else 0f
        val top = rect.centerY() - groupHeight / 2f
        drawIcon(canvas, rect.centerX(), top, iconSize, content.icon, color, outlineColor, outlined)
        val baseline = top + iconSize + gap - metrics.ascent
        drawOutlinedText(canvas, content.text, rect.centerX(), baseline, textPaint, color, outlineColor, outlined, density)

        content.secondaryText?.let { secondary ->
            val secondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                textSize = secondarySize
            }
            while (secondaryPaint.measureText(secondary) > rect.width() && secondaryPaint.textSize > 6f * density) {
                secondaryPaint.textSize *= 0.92f
            }
            drawOutlinedText(
                canvas,
                secondary,
                rect.centerX(),
                baseline + metrics.descent + gap - secondaryPaint.fontMetrics.ascent,
                secondaryPaint,
                color,
                outlineColor,
                outlined,
                density,
            )
        }
    }

    private fun drawOutlinedText(
        canvas: Canvas,
        text: String,
        x: Float,
        baseline: Float,
        paint: Paint,
        color: Int,
        outlineColor: Int,
        outlined: Boolean,
        density: Float,
    ) {
        if (outlined) {
            paint.style = Paint.Style.STROKE
            paint.strokeJoin = Paint.Join.ROUND
            paint.strokeWidth = max(1.5f * density, paint.textSize * 0.08f)
            paint.color = outlineColor
            canvas.drawText(text, x, baseline, paint)
        }
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawText(text, x, baseline, paint)
    }

    private fun drawIcon(
        canvas: Canvas,
        centerX: Float,
        top: Float,
        size: Float,
        icon: WidgetIcon,
        color: Int,
        outlineColor: Int,
        outlined: Boolean,
    ) {
        val path = when (icon) {
            WidgetIcon.LIGHTNING -> lightningPath(centerX, top, size)
            WidgetIcon.BATTERY -> batteryIconPath(centerX, top, size)
            WidgetIcon.THERMOMETER -> thermometerPath(centerX, top, size)
            WidgetIcon.BLUETOOTH -> bluetoothPath(centerX, top, size)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeJoin = Paint.Join.ROUND }
        if (outlined) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = size * 0.16f
            paint.color = outlineColor
            canvas.drawPath(path, paint)
        }
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawPath(path, paint)
    }

    private fun lightningPath(x: Float, y: Float, s: Float) = Path().apply {
        moveTo(x + s * 0.10f, y); lineTo(x - s * 0.34f, y + s * 0.55f)
        lineTo(x - s * 0.05f, y + s * 0.55f); lineTo(x - s * 0.16f, y + s)
        lineTo(x + s * 0.36f, y + s * 0.37f); lineTo(x + s * 0.06f, y + s * 0.37f); close()
    }

    private fun batteryIconPath(x: Float, y: Float, s: Float) = Path().apply {
        val left = x - s * 0.42f
        addRoundRect(RectF(left, y + s * 0.18f, x + s * 0.34f, y + s * 0.88f), s * 0.12f, s * 0.12f, Path.Direction.CW)
        addRect(x + s * 0.34f, y + s * 0.39f, x + s * 0.48f, y + s * 0.67f, Path.Direction.CW)
    }

    private fun thermometerPath(x: Float, y: Float, s: Float) = Path().apply {
        addCircle(x, y + s * 0.76f, s * 0.22f, Path.Direction.CW)
        addRoundRect(RectF(x - s * 0.11f, y, x + s * 0.11f, y + s * 0.78f), s * 0.11f, s * 0.11f, Path.Direction.CW)
    }

    private fun bluetoothPath(x: Float, y: Float, s: Float) = Path().apply {
        moveTo(x, y); lineTo(x + s * 0.32f, y + s * 0.28f); lineTo(x, y + s * 0.5f)
        lineTo(x + s * 0.32f, y + s * 0.72f); lineTo(x, y + s); close()
        moveTo(x, y); lineTo(x - s * 0.34f, y + s * 0.28f); lineTo(x + s * 0.32f, y + s * 0.72f)
        lineTo(x - s * 0.34f, y + s * 0.72f); lineTo(x, y + s * 0.5f)
    }

    private fun highContrastColor(background: Int): Int =
        if (ColorUtils.calculateLuminance(background) > 0.179) Color.BLACK else Color.WHITE

    private const val MAX_BITMAP_SIDE_PX = 768f
    private const val WARNING_COLOR = 0xFFFF3B30.toInt()
}
