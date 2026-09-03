package com.simplebattery.app.widget

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.util.UUID
import kotlin.coroutines.resume
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

data class BluetoothDeviceChoice(val address: String, val name: String)

enum class BluetoothBatteryStatus {
    AVAILABLE,
    OFFLINE,
    UNSUPPORTED,
    PERMISSION_REQUIRED,
    NOT_CONFIGURED,
    WAITING_FOR_DATA,
}

data class BluetoothBatteryResult(
    val level: Int? = null,
    val deviceName: String? = null,
    val status: BluetoothBatteryStatus,
)

class BluetoothBatteryReader(private val context: Context) {
    fun hasPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
        PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun bondedDevices(): List<BluetoothDeviceChoice> {
        if (!hasPermission()) return emptyList()
        return adapter()?.bondedDevices.orEmpty()
            .map { BluetoothDeviceChoice(it.address, it.name ?: "未命名裝置") }
            .sortedBy { it.name.lowercase() }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    suspend fun read(address: String?): BluetoothBatteryResult {
        if (address == null) return BluetoothBatteryResult(status = BluetoothBatteryStatus.NOT_CONFIGURED)
        if (!hasPermission()) return BluetoothBatteryResult(status = BluetoothBatteryStatus.PERMISSION_REQUIRED)
        val adapter = adapter() ?: return BluetoothBatteryResult(status = BluetoothBatteryStatus.OFFLINE)
        val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull()
            ?: return BluetoothBatteryResult(status = BluetoothBatteryStatus.OFFLINE)
        val name = device.name ?: "未命名裝置"
        val connected = isDeviceConnected(adapter, address)
        if (!connected) {
            return BluetoothBatteryResult(deviceName = name, status = BluetoothBatteryStatus.OFFLINE)
        }
        BluetoothBatteryCache(context).getLevel(address)?.let { cachedLevel ->
            return BluetoothBatteryResult(
                level = cachedLevel,
                deviceName = name,
                status = BluetoothBatteryStatus.AVAILABLE,
            )
        }
        val gattResult = withTimeoutOrNull(READ_TIMEOUT_MILLIS) {
            suspendCancellableCoroutine { continuation ->
                var gatt: BluetoothGatt? = null
                var completed = false
                fun complete(result: BluetoothBatteryResult) {
                    if (completed) return
                    completed = true
                    gatt?.close()
                    if (continuation.isActive) continuation.resume(result)
                }
                val callback = object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(gattClient: BluetoothGatt, status: Int, newState: Int) {
                        gatt = gattClient
                        if (status != BluetoothGatt.GATT_SUCCESS || newState == BluetoothProfile.STATE_DISCONNECTED) {
                            complete(BluetoothBatteryResult(deviceName = name, status = BluetoothBatteryStatus.OFFLINE))
                        } else if (newState == BluetoothProfile.STATE_CONNECTED && !gattClient.discoverServices()) {
                            complete(BluetoothBatteryResult(deviceName = name, status = BluetoothBatteryStatus.UNSUPPORTED))
                        }
                    }

                    override fun onServicesDiscovered(gattClient: BluetoothGatt, status: Int) {
                        gatt = gattClient
                        val characteristic = gattClient
                            .getService(BATTERY_SERVICE_UUID)
                            ?.getCharacteristic(BATTERY_LEVEL_UUID)
                        if (status != BluetoothGatt.GATT_SUCCESS || characteristic == null) {
                            complete(BluetoothBatteryResult(deviceName = name, status = BluetoothBatteryStatus.UNSUPPORTED))
                        } else if (!gattClient.readCharacteristic(characteristic)) {
                            complete(BluetoothBatteryResult(deviceName = name, status = BluetoothBatteryStatus.UNSUPPORTED))
                        }
                    }

                    @Deprecated("Used for Android 8 through Android 12 compatibility")
                    override fun onCharacteristicRead(
                        gattClient: BluetoothGatt,
                        characteristic: BluetoothGattCharacteristic,
                        status: Int,
                    ) {
                        gatt = gattClient
                        if (characteristic.uuid != BATTERY_LEVEL_UUID || status != BluetoothGatt.GATT_SUCCESS) {
                            complete(BluetoothBatteryResult(deviceName = name, status = BluetoothBatteryStatus.UNSUPPORTED))
                            return
                        }
                        val value = characteristic.getIntValue(
                            BluetoothGattCharacteristic.FORMAT_UINT8,
                            0,
                        )
                        complete(
                            BluetoothBatteryResult(
                                level = value?.coerceIn(0, 100),
                                deviceName = name,
                                status = if (value == null) {
                                    BluetoothBatteryStatus.UNSUPPORTED
                                } else {
                                    BluetoothBatteryStatus.AVAILABLE
                                },
                            ),
                        )
                    }

                    override fun onCharacteristicRead(
                        gattClient: BluetoothGatt,
                        characteristic: BluetoothGattCharacteristic,
                        value: ByteArray,
                        status: Int,
                    ) {
                        gatt = gattClient
                        val level = value.firstOrNull()?.toInt()?.and(0xFF)
                        complete(
                            BluetoothBatteryResult(
                                level = level?.coerceIn(0, 100),
                                deviceName = name,
                                status = if (status == BluetoothGatt.GATT_SUCCESS && level != null) {
                                    BluetoothBatteryStatus.AVAILABLE
                                } else {
                                    BluetoothBatteryStatus.UNSUPPORTED
                                },
                            ),
                        )
                    }
                }
                gatt = device.connectGatt(context, false, callback, android.bluetooth.BluetoothDevice.TRANSPORT_LE)
                continuation.invokeOnCancellation { gatt?.close() }
            }
        } ?: BluetoothBatteryResult(deviceName = name, status = BluetoothBatteryStatus.WAITING_FOR_DATA)
        return if (gattResult.status == BluetoothBatteryStatus.UNSUPPORTED) {
            gattResult.copy(status = BluetoothBatteryStatus.WAITING_FOR_DATA)
        } else {
            gattResult
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun isDeviceConnected(adapter: BluetoothAdapter, address: String): Boolean {
        val manager = context.getSystemService(BluetoothManager::class.java)
        if (manager?.getConnectedDevices(BluetoothProfile.GATT)?.any { it.address == address } == true) {
            return true
        }
        val profiles = buildList {
            add(BluetoothProfile.HEADSET)
            add(BluetoothProfile.A2DP)
            add(BluetoothProfile.GATT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) add(BluetoothProfile.HEARING_AID)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(BluetoothProfile.LE_AUDIO)
        }
        return coroutineScope {
            profiles.distinct().map { profile ->
                async { isConnectedOnProfile(adapter, profile, address) }
            }.awaitAll().any { it }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun isConnectedOnProfile(
        adapter: BluetoothAdapter,
        profile: Int,
        address: String,
    ): Boolean = withTimeoutOrNull(PROFILE_TIMEOUT_MILLIS) {
        suspendCancellableCoroutine { continuation ->
            var proxy: BluetoothProfile? = null
            fun finish(result: Boolean) {
                proxy?.let { adapter.closeProfileProxy(profile, it) }
                if (continuation.isActive) continuation.resume(result)
            }
            val listener = object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profileId: Int, connectedProxy: BluetoothProfile) {
                    proxy = connectedProxy
                    finish(connectedProxy.connectedDevices.any { it.address == address })
                }

                override fun onServiceDisconnected(profileId: Int) = finish(false)
            }
            val requested = runCatching {
                adapter.getProfileProxy(context, listener, profile)
            }.getOrDefault(false)
            if (!requested) finish(false)
            continuation.invokeOnCancellation {
                proxy?.let { adapter.closeProfileProxy(profile, it) }
            }
        }
    } ?: false

    private fun adapter(): BluetoothAdapter? =
        context.getSystemService(BluetoothManager::class.java)?.adapter

    companion object {
        private val BATTERY_SERVICE_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        private val BATTERY_LEVEL_UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        private const val READ_TIMEOUT_MILLIS = 10_000L
        private const val PROFILE_TIMEOUT_MILLIS = 2_000L
    }
}
