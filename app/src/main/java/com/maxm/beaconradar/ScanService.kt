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
        startForeground(1, notification)
        BleRepository.start(this)
        return START_STICKY
    }

    override fun onDestroy() {
        BleRepository.stop()
        super.onDestroy()
    }
}
