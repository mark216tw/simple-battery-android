package com.simplebattery.app.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WidgetUpdateWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        BatteryWidgetProvider.updateAll(applicationContext)
        BluetoothBatteryWidgetProvider.updateAll(applicationContext)
        return Result.success()
    }
}
