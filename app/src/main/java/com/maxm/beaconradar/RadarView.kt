package com.maxm.beaconradar

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Signalstärke -> Anteil 0..1 (nah..fern), wie im Python-/Web-Tool. */
private fun nearFrac(rssi: Double): Float {
    val r = rssi.coerceIn(-95.0, -45.0)
    return ((r + 95.0) / 50.0).toFloat()
}

private fun hashOf(s: String, mult: Int): Int {
    var h = 0
    for (ch in s) h = h * mult + ch.code   // Int-Overflow wrappt absichtlich
    return h
}

/** Stabile Farbe pro Geräte-ID (wie colorFor im Web). */
private fun colorFor(id: String): Color {
    val hue = (((hashOf(id, 31) % 360) + 360) % 360).toFloat()
    return Color.hsv(hue, 0.6f, 0.9f)
}

/** Stabiler Winkel pro Geräte-ID (nur Optik, ohne Bedeutung – wie angleFor im Web). */
private fun angleFor(id: String): Double {
    val deg = ((hashOf(id, 131) % 360) + 360) % 360
    return deg * PI / 180.0
}

@Composable
fun RadarView(devices: List<Device>, pinnedKey: String?, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(max = 380.dp)
            .aspectRatio(1f)
            .padding(4.dp)
    ) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
            drawRadar(devices, pinnedKey)
        }
    }
}

private fun DrawScope.drawRadar(devices: List<Device>, pinnedKey: String?) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val maxR = min(w, h) / 2f - 24f

    val ringColor = Color(0xFF30363D)
    val zoneLabel = Paint().apply {
        color = Color(0xFF8B949E).toArgb(); textSize = 26f; isAntiAlias = true
    }

    val zones = listOf(
        "sehr nah" to Classify.TH_SEHRNAH,
        "nah" to Classify.TH_NAH,
        "mittel" to Classify.TH_MITTEL,
        "weit" to Classify.TH_WEIT,
    )
    for ((label, th) in zones) {
        val rad = (1f - nearFrac(th.toDouble())) * maxR
        drawCircle(ringColor, radius = rad, center = Offset(cx, cy), style = Stroke(width = 2f))
        drawContext.canvas.nativeCanvas.drawText("$label ($th)", cx + 6f, cy - rad + 26f, zoneLabel)
    }

    // Zentrum = du
    drawCircle(Color(0xFF3FB950), radius = 9f, center = Offset(cx, cy))
    val duPaint = Paint().apply { color = Color(0xFFE6EDF3).toArgb(); textSize = 28f; isAntiAlias = true }
    drawContext.canvas.nativeCanvas.drawText("du", cx + 14f, cy + 8f, duPaint)

    for (d in devices) {
        val blk = Classify.isBlukii(d)
        val sel = pinnedKey != null && d.key == pinnedKey
        val rad = (1f - nearFrac(d.rssi)) * maxR
        val a = angleFor(d.id)
        val x = cx + rad * cos(a).toFloat()
        val y = cy + rad * sin(a).toFloat()
        val dotColor = when {
            sel -> Color(0xFFF0B429)
            blk -> Color(0xFF2DD4BF)
            else -> colorFor(d.id)
        }
        drawCircle(dotColor, radius = if (sel) 11f else 8f, center = Offset(x, y))
        if (sel || blk) {
            drawCircle(
                dotColor, radius = if (sel) 16f else 13f, center = Offset(x, y),
                style = Stroke(width = 3f),
            )
        }
        // Label nur für nähere Geräte, sonst wird's zu voll
        if (d.rssi >= Classify.TH_MITTEL) {
            val name = d.name ?: ("Gerät " + d.id.takeLast(5))
            val lbl = (if (blk) "🔑 " else "") + name
            val p = Paint().apply { color = dotColor.toArgb(); textSize = 24f; isAntiAlias = true }
            drawContext.canvas.nativeCanvas.drawText(lbl, x + 14f, y + 8f, p)
        }
    }
}
