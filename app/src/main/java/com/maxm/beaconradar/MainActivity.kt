package com.maxm.beaconradar

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNeededPermissions()
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { AppScreen(::startScan, ::stopScan) } }
    }

    private fun requestNeededPermissions() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms += Manifest.permission.BLUETOOTH_SCAN
            perms += Manifest.permission.BLUETOOTH_CONNECT
        } else {
            perms += Manifest.permission.ACCESS_FINE_LOCATION
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += Manifest.permission.POST_NOTIFICATIONS
        }
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permLauncher.launch(missing.toTypedArray())
    }

    private fun startScan() {
        val i = Intent(this, ScanService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i) else startService(i)
    }

    private fun stopScan() {
        stopService(Intent(this, ScanService::class.java))
    }
}

private val timeFmt = SimpleDateFormat("dd.MM. HH:mm:ss", Locale.GERMANY)

@Composable
fun AppScreen(onStart: () -> Unit, onStop: () -> Unit) {
    val devices by BleRepository.state.collectAsState()
    val history by BleRepository.history.collectAsState()
    var scanning by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📡 Beacon Radar", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            Button(onClick = {
                scanning = !scanning
                if (scanning) onStart() else onStop()
            }) { Text(if (scanning) "Stop" else "Start") }
        }
        Spacer(Modifier.height(8.dp))
        Text("${devices.size} Geräte", color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(8.dp))

        LazyColumn(Modifier.weight(1f)) {
            items(devices, key = { it.id }) { d ->
                DeviceRow(d)
                Divider()
            }
        }

        if (history.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("📋 Verlauf — Eintritte in „sehr nah“", style = MaterialTheme.typography.titleMedium)
            LazyColumn(Modifier.heightIn(max = 220.dp)) {
                items(history.reversed(), key = { it.ts }) { h ->
                    Text(
                        "${timeFmt.format(Date(h.ts))}  ·  ${h.name}  ·  ${h.typ}  ·  ${h.rssi} dBm",
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceRow(d: Device) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(d.name ?: ("Gerät " + d.id.takeLast(5)))
        Text(d.id, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
        Text(
            "🏷️ ${Classify.classify(d)}   ·   ${d.rssi.toInt()} dBm   ·   ${Classify.nearLabel(d.rssi)}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}
