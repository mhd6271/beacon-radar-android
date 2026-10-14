package com.maxm.beaconradar

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Notifier {
    const val CH_ALARM = "beacon_alarm"
    const val CH_SERVICE = "beacon_service"

    fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val alarm = NotificationChannel(CH_ALARM, "Annäherungs-Alarm", NotificationManager.IMPORTANCE_HIGH)
            alarm.enableVibration(true)
            val svc = NotificationChannel(CH_SERVICE, "Scanner aktiv", NotificationManager.IMPORTANCE_LOW)
            mgr.createNotificationChannel(alarm)
            mgr.createNotificationChannel(svc)
        }
    }

    private fun canNotify(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** Alarm bei Eintritt in „sehr nah" — mit Standard-Ton + Vibration. */
    fun alarm(ctx: Context, e: HistoryEntry) {
        ensureChannels(ctx)
        if (!canNotify(ctx)) return
        val n = NotificationCompat.Builder(ctx, CH_ALARM)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Gerät in „sehr nah“")
            .setContentText("${e.name} · ${e.typ} · ${e.rssi} dBm")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify((e.ts % Int.MAX_VALUE).toInt(), n)
    }
}
