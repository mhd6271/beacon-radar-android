package com.maxm.beaconradar

/**
 * Ein erkanntes BLE-Gerät (portiert aus beacon_radar.py).
 * Unveränderlich: StateFlow vergleicht Listen per equals – würde man dieselben Objekte
 * verändern, sähe die neue Liste aus wie die alte und die UI bekäme kein Update.
 */
data class Device(
    val id: String,              // MAC-Adresse
    val name: String?,
    val rssi: Double,            // geglättet (EWMA)
    val ibeacon: IBeacon?,
    val companies: List<Int>,    // Hersteller-Company-IDs aus den Advertising-Daten
    val lastSeen: Long,
) {
    /** Neues Paket einarbeiten: RSSI per EWMA glätten, fehlende Angaben behalten. */
    fun updated(rssi: Double, name: String?, ib: IBeacon?, companies: List<Int>, now: Long, alpha: Double) =
        copy(
            rssi = alpha * rssi + (1 - alpha) * this.rssi,
            name = if (!name.isNullOrBlank()) name else this.name,
            ibeacon = ib ?: this.ibeacon,
            companies = companies.ifEmpty { this.companies },
            lastSeen = now,
        )

    /** Stabiler Schlüssel: iBeacon-Identität, sonst MAC. */
    val key: String
        get() = ibeacon?.let { "ib:${it.uuid}:${it.major}:${it.minor}" } ?: "mac:$id"
}

data class IBeacon(val uuid: String, val major: Int, val minor: Int, val tx: Int)

data class HistoryEntry(
    val ts: Long,
    val id: String,
    val name: String,
    val typ: String,
    val rssi: Int,
)
