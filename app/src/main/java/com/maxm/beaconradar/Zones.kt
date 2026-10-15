package com.maxm.beaconradar

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Zonen-Schwellen in dBm. */
data class ZoneTh(
    val sehrnah: Int = -60,
    val nah: Int = -72,
    val mittel: Int = -83,
    val weit: Int = -90,
)

/**
 * Verschiebbare, persistente Zonen-Schwellen (wie die zieh­baren Ringe im Web-Tool).
 * Von Radar-Zeichnung, Liste UND Alarm-Logik gemeinsam genutzt.
 */
object Zones {
    private var prefs: SharedPreferences? = null
    private val _th = MutableStateFlow(ZoneTh())
    val th: StateFlow<ZoneTh> = _th

    fun init(ctx: Context) {
        val p = ctx.applicationContext.getSharedPreferences("beacon_radar", Context.MODE_PRIVATE)
        prefs = p
        _th.value = ZoneTh(
            p.getInt("z_sehrnah", -60),
            p.getInt("z_nah", -72),
            p.getInt("z_mittel", -83),
            p.getInt("z_weit", -90),
        )
    }

    fun set(t: ZoneTh) {
        _th.value = t
        prefs?.edit()?.apply {
            putInt("z_sehrnah", t.sehrnah)
            putInt("z_nah", t.nah)
            putInt("z_mittel", t.mittel)
            putInt("z_weit", t.weit)
            apply()
        }
    }

    fun reset() = set(ZoneTh())
}
