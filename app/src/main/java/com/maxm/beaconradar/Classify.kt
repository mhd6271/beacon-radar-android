package com.maxm.beaconradar

/** Typ-/Hersteller-Erkennung und Nähe-Einstufung (portiert aus beacon_radar.py). */
object Classify {

    // Zonen-Schwellen in dBm (wie im Python-Tool)
    const val TH_SEHRNAH = -60
    const val TH_NAH = -72
    const val TH_MITTEL = -83
    const val TH_WEIT = -90

    private val COMPANY = mapOf(
        0x004C to "Apple", 0x0006 to "Microsoft", 0x0075 to "Samsung", 0x00E0 to "Google",
        0x0059 to "Nordic", 0x012D to "Sony", 0x0157 to "Huawei", 0x038F to "Xiaomi",
        0x0087 to "Garmin", 0x009E to "Bose", 0x00D2 to "Logitech",
    )

    /** blukii-Fob? (Kennzeichnung im Radar/Liste), wie isBlukii im Web-Tool. */
    fun isBlukii(d: Device): Boolean = (d.name ?: "").contains("blukii", ignoreCase = true)

    fun nearLabel(rssi: Double): String = when {
        rssi >= TH_SEHRNAH -> "sehr nah"
        rssi >= TH_NAH -> "nah"
        rssi >= TH_MITTEL -> "mittel"
        rssi >= TH_WEIT -> "weit"
        else -> "sehr weit"
    }

    /** iBeacon-Payload NACH der Apple-Company-ID: 0x02 0x15 <uuid16> <major2> <minor2> <tx1>. */
    fun parseIBeacon(appleData: ByteArray?): IBeacon? {
        if (appleData == null || appleData.size < 23) return null
        if ((appleData[0].toInt() and 0xff) != 0x02 || (appleData[1].toInt() and 0xff) != 0x15) return null
        val uuid = appleData.copyOfRange(2, 18).joinToString("") { "%02x".format(it) }
        val major = ((appleData[18].toInt() and 0xff) shl 8) or (appleData[19].toInt() and 0xff)
        val minor = ((appleData[20].toInt() and 0xff) shl 8) or (appleData[21].toInt() and 0xff)
        val tx = appleData[22].toInt()
        return IBeacon(uuid, major, minor, tx)
    }

    fun classify(d: Device): String {
        val low = (d.name ?: "").lowercase()
        val parts = mutableListOf<String>()

        var vendor = d.companies.firstNotNullOfOrNull { COMPANY[it] }
        if ("blukii" in low) vendor = "blukii"
        // iBeacons tragen IMMER Apples Company-ID — das macht sie nicht zu Apple-Geräten:
        if (d.ibeacon != null && vendor == "Apple") vendor = null
        if (vendor != null) parts.add(vendor)

        if (d.ibeacon != null) parts.add("iBeacon")
        if (listOf("jbl", "bose", "buds", "airpod", "nothing", "soundcore", "speaker", "flip")
                .any { it in low } && "Audio" !in parts) {
            parts.add("Audio")
        }
        return if (parts.isEmpty()) "unbekannt" else parts.joinToString(" · ")
    }
}
