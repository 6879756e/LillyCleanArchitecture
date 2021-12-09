package com.lilyddang.lilycleanarchitecture.devices.ble

import android.content.Context
import com.lilyddang.lilycleanarchitecture.devices.CHARACTERISTIC_COMMAND_STRING
import com.lilyddang.lilycleanarchitecture.devices.CHARACTERISTIC_RESPONSE_STRING
import com.lilyddang.lilycleanarchitecture.domain.ble.BleRepository
import com.lilyddang.lilycleanarchitecture.domain.utils.DeviceEvent
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.*

class BleRepositoryImpl : BleRepository {
    private var rxBleConnection: RxBleConnection? = null
    private var conStateDisposable: Disposable? = null
    private var mConnectSubscription: Disposable? = null

    override var deviceEvent = MutableSharedFlow<DeviceEvent<Boolean>>()
    override var isDeviceConnected = MutableStateFlow(false)
    override var deviceName = MutableStateFlow("")

    /**
     * Scan
     */
    override fun scanBleDevices(
        context: Context,
        settings: ScanSettings,
        scanFilter: ScanFilter
    ): Observable<ScanResult> = RxBleClient.create(context).scanBleDevices(settings, scanFilter)

    /**
     * Connect & Discover Services
     * @Saved rxBleConnection
     */
    override fun connectBleDevice(
        device: RxBleDevice
    ) {
        conStateDisposable = device.observeConnectionStateChanges()
            .subscribe(
                { connectionState ->
                    connectionStateListener(device, connectionState)
                }
            ) { throwable ->
                throwable.printStackTrace()
            }
        mConnectSubscription = device.establishConnection(false)
            .flatMapSingle { _rxBleConnection ->
                // All GATT operations are done through the rxBleConnection.
                rxBleConnection = _rxBleConnection
                // Discover services
                _rxBleConnection.discoverServices()
            }.subscribe({
                // Services
            }, {
            })
    }


    /**
     * Connection State Changed Listener
     */
    private fun connectionStateListener(
        device: RxBleDevice,
        connectionState: RxBleConnection.RxBleConnectionState
    ) {
        when (connectionState) {
            RxBleConnection.RxBleConnectionState.CONNECTED -> {
                CoroutineScope(Dispatchers.IO).launch {
                    deviceEvent.emit(DeviceEvent.isDeviceConnected(device.name ?: "", true))
                    isDeviceConnected.value = true
                    deviceName.value = device.name ?: ""
                }
            }
            RxBleConnection.RxBleConnectionState.CONNECTING -> {
            }
            RxBleConnection.RxBleConnectionState.DISCONNECTED -> {
                conStateDisposable?.dispose()
                CoroutineScope(Dispatchers.IO).launch {
                    deviceEvent.emit(DeviceEvent.isDeviceConnected(device.name ?: "", false))
                    isDeviceConnected.value = false
                    deviceName.value = ""
                }
            }
            RxBleConnection.RxBleConnectionState.DISCONNECTING -> {
            }
        }
    }


    /**
     * Notification
     */
    override fun bleNotification() = rxBleConnection
        ?.setupNotification(UUID.fromString(CHARACTERISTIC_RESPONSE_STRING))
        ?.doOnNext { notificationObservable ->
            // Notification has been set up
        }
        ?.flatMap { notificationObservable -> notificationObservable }


    /**
     * Write Data
     */
    override fun writeData(sendByteData: ByteArray) =
        rxBleConnection?.writeCharacteristic(
            UUID.fromString(CHARACTERISTIC_COMMAND_STRING),
            sendByteData
        )

    /**
     * Disconnect Device
     */
    override fun disconnectBleDevice(){
        mConnectSubscription?.dispose()
    }
}
