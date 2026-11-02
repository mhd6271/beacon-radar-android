package com.maxm.beaconradar

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

/** Foreground-Service: scannt weiter, auch wenn die App im Hintergrund ist. */
class ScanService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

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
        BleRepository.start(this)
        if (!BleRepository.running.value) {
            // Bluetooth aus oder Scan verweigert: keine „aktiv“-Meldung stehen lassen
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        BleRepository.stop()
        super.onDestroy()
    }
}
