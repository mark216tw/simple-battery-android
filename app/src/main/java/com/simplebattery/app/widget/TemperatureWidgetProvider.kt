package com.simplebattery.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.simplebattery.app.R
import com.simplebattery.app.SystemSettingsIntents
import com.simplebattery.app.data.BatteryInfo
import java.util.Locale

class TemperatureWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
        BatteryWidgetProvider.ensurePeriodicUpdates(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) = update(context, manager, appWidgetId)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in UPDATE_EVENTS) updateAll(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val preferences = WidgetPreferences(context)
        appWidgetIds.forEach(preferences::delete)
    }

    companion object {
        private val UPDATE_EVENTS = setOf(
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED,
            Intent.ACTION_BATTERY_LOW,
            Intent.ACTION_BATTERY_OKAY,
            Intent.ACTION_CONFIGURATION_CHANGED,
        )

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, TemperatureWidgetProvider::class.java))
                .forEach { update(context, manager, it) }
        }

        fun update(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val temperature = BatteryInfo.read(context).temperatureCelsius
            val settings = WidgetPreferences(context).get(appWidgetId)
                .copy(type = WidgetType.BATTERY_TEMPERATURE)
            val content = WidgetContent(
                text = temperature?.let { String.format(Locale.getDefault(), "%.1f°C", it) }
                    ?: "--°C",
                progress = temperatureProgress(temperature),
                icon = WidgetIcon.THERMOMETER,
                warning = temperature != null && temperature >= 40f,
            )
            val bitmap = WidgetRenderer.render(context, manager, appWidgetId, content, settings)
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId + 100_000,
                SystemSettingsIntents.createIntent(
                    context,
                    SystemSettingsIntents.DESTINATION_BATTERY,
                ),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            manager.updateAppWidget(
                appWidgetId,
                RemoteViews(context.packageName, R.layout.battery_widget).apply {
                    setImageViewBitmap(R.id.widget_image, bitmap)
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                },
            )
        }
    }
}
