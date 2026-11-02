package com.maxm.beaconradar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClassifyTest {

    private fun dev(name: String? = null, ib: IBeacon? = null, companies: List<Int> = emptyList()) =
        Device("AA:BB:CC:DD:EE:FF", name, -70.0, ib, companies, 0L)

    /** 0x02 0x15 + UUID 00..0f + major 0x0102 + minor 0x0304 + tx -59 */
    private val ibeaconPayload = byteArrayOf(0x02, 0x15) +
        ByteArray(16) { it.toByte() } +
        byteArrayOf(0x01, 0x02, 0x03, 0x04, (-59).toByte())

    @Test fun parseIBeacon_liest_Felder() {
        val ib = assertNotNullRet(Classify.parseIBeacon(ibeaconPayload))
        assertEquals("000102030405060708090a0b0c0d0e0f", ib.uuid)
        assertEquals(0x0102, ib.major)
        assertEquals(0x0304, ib.minor)
        assertEquals(-59, ib.tx)
    }

    @Test fun parseIBeacon_lehnt_Kurzes_und_Fremdes_ab() {
        assertNull(Classify.parseIBeacon(null))
        assertNull(Classify.parseIBeacon(ibeaconPayload.copyOf(22)))
        assertNull(Classify.parseIBeacon(byteArrayOf(0x10, 0x05) + ByteArray(21)))
    }

    @Test fun iBeacon_ist_nicht_Apple() {
        val ib = Classify.parseIBeacon(ibeaconPayload)
        assertEquals("iBeacon", Classify.classify(dev(ib = ib, companies = listOf(0x004C))))
        assertEquals("Apple", Classify.classify(dev(companies = listOf(0x004C))))
    }

    @Test fun classify_Name_und_Audio() {
        assertEquals("blukii", Classify.classify(dev(name = "blukii Key")))
        assertEquals("Samsung · Audio", Classify.classify(dev(name = "Galaxy Buds2", companies = listOf(0x0075))))
        assertEquals("unbekannt", Classify.classify(dev()))
    }

    @Test fun nearLabel_Grenzen() {
        val th = ZoneTh()
        assertEquals("sehr nah", Classify.nearLabel(-60.0, th))
        assertEquals("nah", Classify.nearLabel(-60.1, th))
        assertEquals("mittel", Classify.nearLabel(-83.0, th))
        assertEquals("weit", Classify.nearLabel(-90.0, th))
        assertEquals("sehr weit", Classify.nearLabel(-90.1, th))
    }

    @Test fun updated_glaettet_und_liefert_neues_Objekt() {
        val a = dev(name = "X")
        val b = a.updated(-50.0, null, null, emptyList(), 5L, alpha = 0.35)
        assertNotSame(a, b)
        assertEquals(-70.0, a.rssi, 0.0)                    // altes Objekt unverändert
        assertEquals(0.35 * -50 + 0.65 * -70, b.rssi, 1e-9)
        assertEquals("X", b.name)                           // leerer Name überschreibt nicht
        assertEquals(5L, b.lastSeen)
        // Genau das war der Fehler: gleiche Liste => StateFlow meldet nichts Neues
        assertNotEquals(listOf(a), listOf(b))
    }

    private fun <T> assertNotNullRet(v: T?): T { assertNotNull(v); return v!! }
}
