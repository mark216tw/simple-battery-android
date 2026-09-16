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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BluetoothBatteryWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ids.forEach { update(context, manager, it) }
                BatteryWidgetProvider.ensurePeriodicUpdates(context)
            } finally {
                pending.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                update(context, manager, appWidgetId)
            } finally {
                pending.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in UPDATE_EVENTS) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    updateAll(context)
                } finally {
                    pending.finish()
                }
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val preferences = WidgetPreferences(context)
        appWidgetIds.forEach(preferences::delete)
    }

    companion object {
        private val UPDATE_EVENTS = setOf(
            Intent.ACTION_CONFIGURATION_CHANGED,
            android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED,
            android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED,
        )

        suspend fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, BluetoothBatteryWidgetProvider::class.java))
                .forEach { update(context, manager, it) }
        }

        suspend fun update(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val settings = WidgetPreferences(context).get(appWidgetId)
                .copy(type = WidgetType.BLUETOOTH_BATTERY)
            val result = BluetoothBatteryReader(context).read(settings.bluetoothAddress)
            val statusText = when (result.status) {
                BluetoothBatteryStatus.AVAILABLE -> result.deviceName
                BluetoothBatteryStatus.OFFLINE -> "離線"
                BluetoothBatteryStatus.UNSUPPORTED -> "不支援"
                BluetoothBatteryStatus.PERMISSION_REQUIRED -> "需要權限"
                BluetoothBatteryStatus.NOT_CONFIGURED -> "未設定"
                BluetoothBatteryStatus.WAITING_FOR_DATA -> "等待電量"
            }
            val text = result.level?.let { batteryLevelText(it, settings.showPercent) } ?: "--"
            val content = WidgetContent(
                text = text,
                progress = result.level?.div(100f),
                icon = WidgetIcon.BLUETOOTH,
                secondaryText = if (settings.showDeviceName || result.level == null) {
                    statusText?.take(12)
                } else {
                    null
                },
            )
            val bitmap = WidgetRenderer.render(context, manager, appWidgetId, content, settings)
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId + 200_000,
                SystemSettingsIntents.createIntent(
                    context,
                    SystemSettingsIntents.DESTINATION_BLUETOOTH,
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
