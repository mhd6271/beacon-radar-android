package com.maxm.beaconradar

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

private fun colorFor(id: String): Color {
    val hue = (((hashOf(id, 31) % 360) + 360) % 360).toFloat()
    return Color.hsv(hue, 0.6f, 0.9f)
}

private fun angleFor(id: String): Double {
    val deg = ((hashOf(id, 131) % 360) + 360) % 360
    return deg * PI / 180.0
}

/** Text mit dunklem Rand (Halo) -> lesbar auch über Punkten/Überlappung. */
private fun DrawScope.drawLabel(text: String, x: Float, y: Float, color: Int, size: Float = 26f) {
    val c = drawContext.canvas.nativeCanvas
    val halo = Paint().apply {
        this.color = 0xDD0D1117.toInt(); textSize = size; isAntiAlias = true
        style = Paint.Style.STROKE; strokeWidth = 6f
    }
    val fill = Paint().apply { this.color = color; textSize = size; isAntiAlias = true }
    c.drawText(text, x, y, halo)
    c.drawText(text, x, y, fill)
}

@Composable
fun RadarView(devices: List<Device>, pinnedKey: String?, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .fillMaxWidth()
            .height(320.dp)
            .padding(top = 4.dp, bottom = 4.dp)
    ) {
        drawRadar(devices, pinnedKey)
    }
}

private fun DrawScope.drawRadar(devices: List<Device>, pinnedKey: String?) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val maxR = min(w, h) / 2f - 20f

    // dunkler Panel-Hintergrund
    drawCircle(Color(0xFF0D1117), radius = min(w, h) / 2f, center = Offset(cx, cy))

    val zones = listOf(
        "sehr nah" to Classify.TH_SEHRNAH,
        "nah" to Classify.TH_NAH,
        "mittel" to Classify.TH_MITTEL,
        "weit" to Classify.TH_WEIT,
    )
    // Zonen-Ringe: grün, deutlich sichtbar, nach außen leicht abnehmend
    zones.forEachIndexed { i, (label, th) ->
        val rad = (1f - nearFrac(th.toDouble())) * maxR
        val alpha = 0.60f - i * 0.10f
        drawCircle(
            Color(0xFF3FB950).copy(alpha = alpha),
            radius = rad, center = Offset(cx, cy),
            style = Stroke(width = 3.5f),
        )
        drawLabel("$label ($th)", cx + 8f, cy - rad + 26f, Color(0xFFADBAC7).toArgb(), 24f)
    }

    // Zentrum = du (mit Glow)
    drawCircle(Color(0xFF3FB950).copy(alpha = 0.25f), radius = 20f, center = Offset(cx, cy))
    drawCircle(Color(0xFF3FB950), radius = 11f, center = Offset(cx, cy))
    drawLabel("du", cx + 16f, cy + 8f, Color(0xFFE6EDF3).toArgb(), 28f)

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
        drawCircle(dotColor, radius = if (sel) 12f else 9f, center = Offset(x, y))
        if (sel || blk) {
            drawCircle(
                dotColor, radius = if (sel) 17f else 14f, center = Offset(x, y),
                style = Stroke(width = 3f),
            )
        }
        // Namens-Label nur für nähere Geräte, sonst wird's zu voll
        if (d.rssi >= Classify.TH_NAH) {
            val name = d.name ?: ("Gerät " + d.id.takeLast(5))
            val lbl = (if (blk) "🔑 " else "") + name
            drawLabel(lbl, x + 15f, y + 8f, dotColor.toArgb(), 24f)
        }
    }
}
