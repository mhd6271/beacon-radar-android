package com.maxm.beaconradar

/** Ein erkanntes BLE-Gerät (portiert aus beacon_radar.py). */
data class Device(
    val id: String,              // MAC-Adresse
    var name: String?,
    var rssi: Double,            // geglättet (EWMA)
    var ibeacon: IBeacon?,
    var companies: List<Int>,    // Hersteller-Company-IDs aus den Advertising-Daten
    var lastSeen: Long,
) {
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
