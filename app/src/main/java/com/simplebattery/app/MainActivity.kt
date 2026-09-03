package com.simplebattery.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.simplebattery.app.data.AppSettings
import com.simplebattery.app.data.AppSettingsRepository
import com.simplebattery.app.data.BatteryInfo
import com.simplebattery.app.data.ThemeMode
import com.simplebattery.app.ui.HueSlider
import com.simplebattery.app.ui.theme.SimpleBatteryTheme
import com.simplebattery.app.ui.theme.themePrimary
import com.simplebattery.app.widget.BatteryWidgetProvider
import java.util.Locale
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val showingSettings = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = AppSettingsRepository(applicationContext)
        setContent {
            val settings by repository.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings(),
            )
            SimpleBatteryTheme(settings = settings, activity = this) {
                val battery = rememberBatteryInfo()
                val showSettings by showingSettings
                BackHandler(enabled = showSettings) {
                    showingSettings.value = false
                }
                if (showSettings) {
                    SettingsScreen(
                        settings = settings,
                        repository = repository,
                        battery = battery,
                        onBack = { showingSettings.value = false },
                    )
                } else {
                    BatteryScreen(
                        battery = battery,
                        onOpenSettings = { showingSettings.value = true },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
            showingSettings.value = false
        }
    }
}

@Composable
private fun BatteryScreen(battery: BatteryInfo, onOpenSettings: () -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "簡單電池",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "目前電池狀態",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = "設定",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(38.dp))
            BatteryGauge(level = battery.level, isCharging = battery.isCharging)
            Spacer(Modifier.height(22.dp))
            StatusPill(battery)
            if (battery.isCharging) {
                Spacer(Modifier.height(36.dp))
                ChargingDetails(battery)
                Spacer(Modifier.height(18.dp))
            } else {
                Spacer(Modifier.height(36.dp))
            }
            BatteryDetails(battery)
        }
    }
}

@Composable
private fun rememberBatteryInfo(): BatteryInfo {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var battery by remember { mutableStateOf(BatteryInfo.read(context)) }
    DisposableEffect(context, lifecycleOwner) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                battery = BatteryInfo.read(receiverContext, intent)
                BatteryWidgetProvider.updateAll(receiverContext)
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) battery = BatteryInfo.read(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            context.unregisterReceiver(receiver)
        }
    }
    return battery
}

@Composable
private fun BatteryGauge(level: Int, isCharging: Boolean) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(232.dp)) {
        val primary = MaterialTheme.colorScheme.primary
        val track = MaterialTheme.colorScheme.surfaceVariant
        Canvas(Modifier.fillMaxSize()) {
            val width = 20.dp.toPx()
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = width, cap = StrokeCap.Round),
            )
            drawArc(
                color = primary,
                startAngle = -90f,
                sweepAngle = 360f * level / 100f,
                useCenter = false,
                style = Stroke(width = width, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$level%",
                fontSize = 58.sp,
                lineHeight = 64.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (isCharging) {
                Text(
                    text = "⚡ 充電中",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun StatusPill(battery: BatteryInfo) {
    val label = when {
        battery.isFull -> "電池已充滿"
        battery.isCharging -> "正在充電"
        else -> "使用電池中"
    }
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 11.dp),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun BatteryDetails(battery: BatteryInfo) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 12.dp)) {
            DetailRow("電源", pluggedLabel(battery.plugged))
            DetailRow("健康狀態", healthLabel(battery.health))
            DetailRow(
                "溫度",
                battery.temperatureCelsius?.let {
                    String.format(Locale.getDefault(), "%.1f °C", it)
                } ?: "無法取得",
            )
            DetailRow(
                "電壓",
                battery.voltageMillivolts?.let {
                    String.format(Locale.getDefault(), "%.2f V", it / 1000f)
                } ?: "無法取得",
            )
        }
    }
}

@Composable
private fun ChargingDetails(battery: BatteryInfo) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 12.dp)) {
            Text(
                text = "充電資料",
                modifier = Modifier.padding(vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            DetailRow(
                "即時電流",
                battery.currentMicroamps?.let {
                    String.format(Locale.getDefault(), "%,d mA", it / 1000)
                } ?: "裝置不支援",
            )
            DetailRow(
                "估算功率",
                battery.chargingPowerWatts?.let {
                    String.format(Locale.getDefault(), "%.1f W", it)
                } ?: "裝置不支援",
            )
            DetailRow(
                "預估充滿",
                battery.chargeTimeRemainingMillis?.let(::formatDuration) ?: "裝置不支援",
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsScreen(
    settings: AppSettings,
    repository: AppSettingsRepository,
    battery: BatteryInfo,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onBack),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = "返回",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "設定",
                    modifier = Modifier.padding(start = 18.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("外觀模式")
            ThemeMode.entries.forEach { mode ->
                val label = when (mode) {
                    ThemeMode.SYSTEM -> "跟隨系統"
                    ThemeMode.LIGHT -> "淺色"
                    ThemeMode.DARK -> "深色"
                }
                SelectionRow(
                    label = label,
                    selected = settings.themeMode == mode,
                    onClick = { scope.launch { repository.setThemeMode(mode) } },
                )
                Spacer(Modifier.height(7.dp))
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionTitle("APP 主題色")
                Box(
                    Modifier
                        .size(30.dp)
                        .background(themePrimary(settings.hue, false), CircleShape),
                )
            }
            Spacer(Modifier.height(6.dp))
            HueSlider(
                hue = settings.hue,
                onHueChange = { hue -> scope.launch { repository.setHue(hue) } },
            )
            Text(
                text = "色相 ${settings.hue.toInt()}°",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { scope.launch { repository.setHue(AppSettings.DEFAULT_HUE) } },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("恢復預設色彩", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle("桌面小工具")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(22.dp),
            ) {
                Text(
                    text = "目前 ${battery.level}%。長按桌面可加入手機電量、電池溫度或藍牙裝置電量小工具；點擊已加入的小工具可再次調整。",
                    modifier = Modifier.padding(16.dp),
                    lineHeight = 22.sp,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun SelectionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = background,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            if (selected) Text("✓", fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}

private fun pluggedLabel(plugged: Int): String = when (plugged) {
    BatteryManager.BATTERY_PLUGGED_AC -> "交流電"
    BatteryManager.BATTERY_PLUGGED_USB -> "USB"
    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "無線充電"
    else -> "未連接"
}

private fun healthLabel(health: Int): String = when (health) {
    BatteryManager.BATTERY_HEALTH_GOOD -> "良好"
    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "溫度過高"
    BatteryManager.BATTERY_HEALTH_DEAD -> "需要更換"
    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "電壓過高"
    BatteryManager.BATTERY_HEALTH_COLD -> "溫度過低"
    else -> "未知"
}

private fun formatDuration(milliseconds: Long): String {
    val totalMinutes = milliseconds / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0L) "${hours} 小時 ${minutes} 分" else "${minutes} 分鐘"
}
