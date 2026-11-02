package com.maxm.beaconradar

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/** Foreground-Service: scannt weiter, auch wenn die App im Hintergrund ist. */
class ScanService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    // Bluetooth wird ausgeschaltet -> Scan ist tot; Service beenden statt „aktiv“ zu melden
    private val btOff = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            val st = i.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            if (st == BluetoothAdapter.STATE_TURNING_OFF || st == BluetoothAdapter.STATE_OFF) stopSelf()
        }
    }
    private var btOffRegistered = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notifier.ensureChannels(this)
        val notification = NotificationCompat.Builder(this, Notifier.CH_SERVICE)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Beacon Radar aktiv")
            .setContentText("Scanne nach Geräten …")
            .setOngoing(true)
            .build()
        try {
            startForeground(1, notification)
        } catch (_: Exception) {
            // z. B. Bluetooth-Rechte entzogen (Android 14 verlangt sie für connectedDevice)
            stopSelf()
            return START_NOT_STICKY
        }
        // Nach Neustart durch START_STICKY läuft keine Activity: Zonen selbst laden,
        // sonst gälten für den Alarm die Standard- statt der eingestellten Schwellen
        Zones.init(this)
        BleRepository.start(this)
        if (!BleRepository.running.value) {
            // Bluetooth aus oder Scan verweigert: keine „aktiv“-Meldung stehen lassen
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        if (!btOffRegistered) {
            ContextCompat.registerReceiver(
                this, btOff, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            btOffRegistered = true
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (btOffRegistered) {
            unregisterReceiver(btOff)
            btOffRegistered = false
        }
        BleRepository.stop()
        super.onDestroy()
    }
}
