# Beacon Radar — Android (Port von beacon_radar.py)

Native Android-App (Kotlin + Jetpack Compose), die die Logik des Python-Laptop-Tools
`beacon_radar.py` aufs Handy bringt. Das Handy ist dann selbst der Sensor — tragbar,
mit Hintergrund-Scan und nativen Benachrichtigungen (auch bei ausgeschaltetem Display).

## Status
**v0.1 Gerüst** — vollständige Projektstruktur + Kernlogik. Muss noch einmal gebaut,
auf Kompilierfehler geprüft und auf einem echten Gerät getestet werden (BLE geht nicht
im Emulator).

## Features (portiert)
- BLE-Scan über `BluetoothLeScanner`
- EWMA-Glättung des RSSI
- Zonen: **sehr nah −60 / nah −72 / mittel −83 / weit −90 dBm**
- **Alarm bei Eintritt in „sehr nah"**: Notification + Ton + Vibration, mit Hysterese
  (erster Kontakt löst nicht aus) und 1,5-s-Entprellung
- **Verlauf/History** der Eintritte (Zeit · Gerät · Typ · Signal)
- **Typ-Erkennung** über Hersteller-Company-ID + iBeacon-Parsing
  (inkl. Fix: iBeacons werden nicht mehr fälschlich als „Apple" gelabelt)
- **Foreground-Service** für Scan im Hintergrund

## Projektstruktur
```
app/src/main/
  AndroidManifest.xml
  java/com/maxm/beaconradar/
    Models.kt          Datenklassen (Device, IBeacon, HistoryEntry)
    Classify.kt        Zonen-Schwellen, iBeacon-Parsing, Typ-Erkennung
    Notifier.kt        Notification-Kanäle + Alarm
    BleRepository.kt   Scan + Zustand (StateFlow) + „sehr nah"-Logik
    ScanService.kt     Foreground-Service
    MainActivity.kt    Compose-UI (Geräteliste + Verlauf, Start/Stop)
  res/values/          strings.xml, themes.xml
```

## Bauen (Debug-APK)
Benötigt Android SDK + Gradle (im Cloud-Container installierbar).
```bash
# einmalig: Wrapper erzeugen (falls nicht vorhanden)
gradle wrapper --gradle-version 8.9

# Debug-APK bauen
./gradlew assembleDebug
# Ergebnis: app/build/outputs/apk/debug/app-debug.apk
```
Auf dem Handy: „Installation aus unbekannten Quellen" erlauben, APK öffnen/installieren.

## Nächste Schritte (TODO)
- Verlauf **persistent** speichern (Room oder Datei) — überlebt dann App-Neustart
- **Ignorier-Liste** für eigene Geräte (nur fremde lösen Alarm aus)
- **Zonen per Touch** verstellbar (wie im Web-Tool die Maus-Ringe)
- Optional: **Radar-Canvas**-Ansicht
- Akku-Tuning (Scan-Intervall / `SCAN_MODE_BALANCED`)
