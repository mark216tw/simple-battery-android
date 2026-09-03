package com.simplebattery.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.simplebattery.app.R
import com.simplebattery.app.data.BatteryInfo
import java.util.concurrent.TimeUnit

class BatteryWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { update(context, appWidgetManager, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        update(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in BATTERY_EVENTS) updateAll(context)
    }

    override fun onEnabled(context: Context) {
        ensurePeriodicUpdates(context)
    }

    override fun onDisabled(context: Context) = Unit

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val preferences = WidgetPreferences(context)
        appWidgetIds.forEach(preferences::delete)
    }

    companion object {
        private const val WORK_NAME = "battery-widget-periodic-update"
        private val BATTERY_EVENTS = setOf(
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED,
            Intent.ACTION_BATTERY_LOW,
            Intent.ACTION_BATTERY_OKAY,
            Intent.ACTION_CONFIGURATION_CHANGED,
        )

        fun ensurePeriodicUpdates(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, TimeUnit.MINUTES).build(),
        )
        }

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, BatteryWidgetProvider::class.java))
            ids.forEach { update(context, manager, it) }
            TemperatureWidgetProvider.updateAll(context)
        }

        fun update(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            val battery = BatteryInfo.read(context)
            val settings = WidgetPreferences(context).get(appWidgetId)
            val bitmap = WidgetRenderer.render(
                context = context,
                appWidgetManager = appWidgetManager,
                appWidgetId = appWidgetId,
                content = WidgetContent(
                    text = batteryLevelText(battery.level, settings.showPercent),
                    progress = battery.level / 100f,
                    icon = if (battery.isCharging) WidgetIcon.LIGHTNING else WidgetIcon.BATTERY,
                ),
                settings = settings,
            )
            val configureIntent = Intent(context, WidgetConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse("simplebattery://widget/$appWidgetId")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                configureIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val views = RemoteViews(context.packageName, R.layout.battery_widget).apply {
                setImageViewBitmap(R.id.widget_image, bitmap)
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
