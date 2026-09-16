package com.simplebattery.app.widget

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import com.simplebattery.app.data.AppSettings
import com.simplebattery.app.data.AppSettingsRepository
import com.simplebattery.app.data.BatteryInfo
import com.simplebattery.app.MainActivity
import com.simplebattery.app.ui.HueSlider
import com.simplebattery.app.ui.theme.SimpleBatteryTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        enableEdgeToEdge()
        val appRepository = AppSettingsRepository(applicationContext)
        val widgetPreferences = WidgetPreferences(applicationContext)
        val type = widgetTypeFor(appWidgetId)
        val bluetoothReader = BluetoothBatteryReader(applicationContext)
        setContent {
            val appSettings by appRepository.settings.collectAsStateWithLifecycle(AppSettings())
            SimpleBatteryTheme(settings = appSettings, activity = this) {
                val initial = remember { widgetPreferences.get(appWidgetId).copy(type = type) }
                var hue by remember { mutableFloatStateOf(initial.hue) }
                var fontSize by remember { mutableFloatStateOf(initial.fontSizeSp) }
                var background by remember { mutableStateOf(initial.background) }
                var frame by remember { mutableStateOf(initial.frame) }
                var showPercent by remember { mutableStateOf(initial.showPercent) }
                var showDeviceName by remember { mutableStateOf(initial.showDeviceName) }
                var bluetoothAddress by remember { mutableStateOf(initial.bluetoothAddress) }
                var bluetoothPermission by remember { mutableStateOf(bluetoothReader.hasPermission()) }
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted -> bluetoothPermission = granted }
                val devices = remember(bluetoothPermission) {
                    if (bluetoothPermission) bluetoothReader.bondedDevices() else emptyList()
                }
                val currentSettings = initial.copy(
                    hue = hue,
                    fontSizeSp = fontSize,
                    background = background,
                    frame = frame,
                    showPercent = showPercent,
                    showDeviceName = showDeviceName,
                    bluetoothAddress = bluetoothAddress,
                )
                WidgetConfigScreen(
                    settings = currentSettings,
                    onHueChange = { hue = it },
                    onFontSizeChange = { fontSize = it },
                    onBackgroundChange = { background = it },
                    onFrameChange = { frame = it },
                    onShowPercentChange = { showPercent = it },
                    onShowDeviceNameChange = { showDeviceName = it },
                    bluetoothDevices = devices,
                    hasBluetoothPermission = bluetoothPermission,
                    onBluetoothDeviceChange = { bluetoothAddress = it },
                    onRequestBluetoothPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        }
                    },
                    onBackToApp = ::openMainScreen,
                    onSave = {
                        widgetPreferences.save(appWidgetId, currentSettings)
                        updateConfiguredWidget(currentSettings.type)
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                        )
                        finishAndRemoveTask()
                    },
                )
            }
        }
    }

    private fun widgetTypeFor(id: Int): WidgetType {
        val provider = AppWidgetManager.getInstance(this).getAppWidgetInfo(id)?.provider?.className
        return when (provider) {
            TemperatureWidgetProvider::class.java.name -> WidgetType.BATTERY_TEMPERATURE
            BluetoothBatteryWidgetProvider::class.java.name -> WidgetType.BLUETOOTH_BATTERY
            else -> WidgetType.PHONE_BATTERY
        }
    }

    private fun updateConfiguredWidget(type: WidgetType) {
        val manager = AppWidgetManager.getInstance(this)
        when (type) {
            WidgetType.PHONE_BATTERY -> BatteryWidgetProvider.update(this, manager, appWidgetId)
            WidgetType.BATTERY_TEMPERATURE -> TemperatureWidgetProvider.update(this, manager, appWidgetId)
            WidgetType.BLUETOOTH_BATTERY -> CoroutineScope(Dispatchers.IO).launch {
                BluetoothBatteryWidgetProvider.update(
                    applicationContext,
                    manager,
                    appWidgetId,
                )
            }
        }
    }

    private fun openMainScreen() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
        )
        finishAndRemoveTask()
    }
}

@Composable
private fun WidgetConfigScreen(
    settings: WidgetSettings,
    onHueChange: (Float) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onBackgroundChange: (WidgetBackground) -> Unit,
    onFrameChange: (WidgetFrame) -> Unit,
    onShowPercentChange: (Boolean) -> Unit,
    onShowDeviceNameChange: (Boolean) -> Unit,
    bluetoothDevices: List<BluetoothDeviceChoice>,
    hasBluetoothPermission: Boolean,
    onBluetoothDeviceChange: (String) -> Unit,
    onRequestBluetoothPermission: () -> Unit,
    onBackToApp: () -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onBackToApp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = "返回",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = when (settings.type) {
                        WidgetType.PHONE_BATTERY -> "電量小工具"
                        WidgetType.BATTERY_TEMPERATURE -> "溫度小工具"
                        WidgetType.BLUETOOTH_BATTERY -> "藍牙電量小工具"
                    },
                    modifier = Modifier.padding(start = 14.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "每個小工具都可以有自己的樣式",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(10.dp))
            WidgetPreview(settings)
            if (settings.type != WidgetType.BATTERY_TEMPERATURE) {
                Spacer(Modifier.size(4.dp))
                ToggleRow("顯示 % 符號", settings.showPercent, onShowPercentChange)
            }
            Spacer(Modifier.size(12.dp))

            if (settings.type == WidgetType.BLUETOOTH_BATTERY) {
                ConfigTitle("藍牙裝置")
                if (!hasBluetoothPermission) {
                    Button(
                        onClick = onRequestBluetoothPermission,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("允許存取已配對裝置", fontWeight = FontWeight.Bold)
                    }
                } else if (bluetoothDevices.isEmpty()) {
                    Text(
                        text = "沒有已配對的藍牙裝置",
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    bluetoothDevices.forEach { device ->
                        WidgetChoice(
                            text = device.name,
                            selected = settings.bluetoothAddress == device.address,
                            onClick = { onBluetoothDeviceChange(device.address) },
                        )
                        Spacer(Modifier.size(6.dp))
                    }
                    Text(
                        text = "僅支援提供標準 BLE 電池服務的裝置",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.size(12.dp))
            }

            ConfigTitle("主題色")
            HueSlider(hue = settings.hue, onHueChange = onHueChange)
            Text(
                text = "色相 ${settings.hue.toInt()}°",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.size(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ConfigTitle("字級大小")
                Text("${settings.fontSizeSp.toInt()} sp", fontWeight = FontWeight.Bold)
            }
            Slider(
                value = settings.fontSizeSp,
                onValueChange = onFontSizeChange,
                valueRange = 28f..52f,
            )

            Spacer(Modifier.size(10.dp))
            ConfigTitle("外框")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WidgetCompactChoice(
                    text = "無外框",
                    selected = settings.frame == WidgetFrame.NONE,
                    modifier = Modifier.weight(1f),
                    onClick = { onFrameChange(WidgetFrame.NONE) },
                )
                WidgetCompactChoice(
                    text = "圓形",
                    selected = settings.frame == WidgetFrame.CIRCLE,
                    modifier = Modifier.weight(1f),
                    onClick = { onFrameChange(WidgetFrame.CIRCLE) },
                )
                WidgetCompactChoice(
                    text = "電池",
                    selected = settings.frame == WidgetFrame.BATTERY,
                    modifier = Modifier.weight(1f),
                    onClick = { onFrameChange(WidgetFrame.BATTERY) },
                )
            }

            Spacer(Modifier.size(10.dp))
            ConfigTitle("背景樣式")
            WidgetChoice(
                text = "主題色背景",
                selected = settings.background == WidgetBackground.THEME_COLOR,
                onClick = { onBackgroundChange(WidgetBackground.THEME_COLOR) },
            )
            Spacer(Modifier.size(8.dp))
            WidgetChoice(
                text = "透明背景",
                selected = settings.background == WidgetBackground.TRANSPARENT,
                onClick = { onBackgroundChange(WidgetBackground.TRANSPARENT) },
            )

            if (settings.type == WidgetType.BLUETOOTH_BATTERY) {
                Spacer(Modifier.size(6.dp))
                ToggleRow("顯示裝置名稱", settings.showDeviceName, onShowDeviceNameChange)
            }

            Spacer(Modifier.size(12.dp))
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = settings.type != WidgetType.BLUETOOTH_BATTERY ||
                    settings.bluetoothAddress != null,
            ) {
                Text("完成", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.size(10.dp))
        }
    }
}

@Composable
private fun WidgetPreview(settings: WidgetSettings) {
    val battery = rememberCurrentBatteryInfo()
    val dark = isSystemInDarkTheme()
    val density = LocalDensity.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(126.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(
                modifier = Modifier
                    .width(96.dp)
                    .height(116.dp),
            ) {
                drawIntoCanvas { canvas ->
                    WidgetRenderer.drawArtwork(
                        canvas = canvas.nativeCanvas,
                        width = size.width,
                        height = size.height,
                        density = density.density,
                        scaledDensity = density.density * density.fontScale,
                        content = previewContent(settings, battery),
                        settings = settings,
                        dark = dark,
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberCurrentBatteryInfo(): BatteryInfo {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var battery by remember { mutableStateOf(BatteryInfo.read(context)) }
    DisposableEffect(context, lifecycleOwner) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                battery = BatteryInfo.read(receiverContext, intent)
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

private fun previewContent(settings: WidgetSettings, battery: BatteryInfo): WidgetContent =
    when (settings.type) {
        WidgetType.PHONE_BATTERY -> WidgetContent(
            text = batteryLevelText(battery.level, settings.showPercent),
            progress = battery.level / 100f,
            icon = if (battery.isCharging) WidgetIcon.LIGHTNING else WidgetIcon.BATTERY,
        )
        WidgetType.BATTERY_TEMPERATURE -> WidgetContent(
            text = battery.temperatureCelsius?.let { "%.1f°C".format(it) } ?: "--°C",
            progress = temperatureProgress(battery.temperatureCelsius),
            icon = WidgetIcon.THERMOMETER,
            warning = (battery.temperatureCelsius ?: 0f) >= 40f,
        )
        WidgetType.BLUETOOTH_BATTERY -> WidgetContent(
            text = if (settings.showPercent) "85%" else "85",
            progress = 0.85f,
            icon = WidgetIcon.BLUETOOTH,
            secondaryText = if (settings.showDeviceName) "藍牙裝置" else null,
        )
    }

@Composable
private fun ConfigTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun WidgetChoice(text: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val content = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = background,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            if (selected) Text("✓", fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun WidgetCompactChoice(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val background = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val content = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = background,
        contentColor = content,
    ) {
        Text(
            text = if (selected) "✓ $text" else text,
            modifier = Modifier.padding(vertical = 12.dp),
            textAlign = TextAlign.Center,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun ToggleRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, fontWeight = FontWeight.Bold)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
