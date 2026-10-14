package com.maxm.beaconradar

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Zentrales BLE-Scanning + Zustand (Singleton). Portiert die Kernlogik aus beacon_radar.py:
 * EWMA-Glättung, Zonen, „sehr nah"-Eintritt mit Hysterese -> Alarm + Verlauf.
 */
object BleRepository {
    private const val ALPHA = 0.35
    private const val STALE_MS = 20_000L
    private const val BEEP_THROTTLE_MS = 1_500L

    private val devices = mutableMapOf<String, Device>()
    private val nearState = mutableMapOf<String, Boolean>()
    private var lastBeep = 0L

    private val _state = MutableStateFlow<List<Device>>(emptyList())
    val state: StateFlow<List<Device>> = _state

    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history

    private var scanner: BluetoothLeScanner? = null
    private var appCtx: Context? = null
    private var running = false

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) = handle(result)
        override fun onBatchScanResults(results: MutableList<ScanResult>) = results.forEach { handle(it) }
    }

    @SuppressLint("MissingPermission")
    fun start(context: Context) {
        if (running) return
        appCtx = context.applicationContext
        val mgr = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        scanner = mgr?.adapter?.bluetoothLeScanner ?: return
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        try {
            scanner?.startScan(null, settings, callback)
            running = true
        } catch (_: SecurityException) {
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        try { scanner?.stopScan(callback) } catch (_: Exception) {}
        running = false
    }

    @SuppressLint("MissingPermission")
    private fun handle(r: ScanResult) {
        val rssi = r.rssi.toDouble()
        val rec = r.scanRecord
        val apple = rec?.getManufacturerSpecificData(0x004C)
        val ib = Classify.parseIBeacon(apple)

        val companies = mutableListOf<Int>()
        rec?.manufacturerSpecificData?.let { sa ->
            for (i in 0 until sa.size()) companies.add(sa.keyAt(i))
        }

        val id = r.device.address
        val name = rec?.deviceName
        val now = System.currentTimeMillis()

        val existing = devices[id]
        if (existing == null) {
            devices[id] = Device(id, name, rssi, ib, companies, now)
        } else {
            existing.rssi = ALPHA * rssi + (1 - ALPHA) * existing.rssi
            if (!name.isNullOrBlank()) existing.name = name
            if (ib != null) existing.ibeacon = ib
            if (companies.isNotEmpty()) existing.companies = companies
            existing.lastSeen = now
        }
        checkProximity(devices.getValue(id))
        emit()
    }

    private fun emit() {
        val now = System.currentTimeMillis()
        _state.value = devices.values
            .filter { now - it.lastSeen < STALE_MS }
            .sortedByDescending { it.rssi }
            .toList()
    }

    /** Eintritt in „sehr nah" mit Hysterese: erster Kontakt löst nicht aus. */
    private fun checkProximity(d: Device) {
        val enter = Classify.TH_SEHRNAH
        val was = nearState[d.key]
        when {
            was == null -> nearState[d.key] = d.rssi >= enter
            !was && d.rssi >= enter -> {
                nearState[d.key] = true
                onEnterSehrNah(d)
            }
            was && d.rssi < enter - 5 -> nearState[d.key] = false
        }
    }

    private fun onEnterSehrNah(d: Device) {
        val entry = HistoryEntry(
            ts = System.currentTimeMillis(),
            id = d.id,
            name = d.name ?: ("Gerät " + d.id.takeLast(5)),
            typ = Classify.classify(d),
            rssi = d.rssi.toInt(),
        )
        _history.value = (_history.value + entry).takeLast(500)
        appCtx?.let { ctx ->
            val now = System.currentTimeMillis()
            if (now - lastBeep > BEEP_THROTTLE_MS) {
                lastBeep = now
                Notifier.alarm(ctx, entry)
            }
        }
        // TODO (neue Session): Verlauf persistent speichern (Room oder Datei).
    }
}
