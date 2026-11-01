# Beacon Radar — Android (Port von beacon_radar.py)

Native Android-App (Kotlin + Jetpack Compose), die die Logik des Python-Laptop-Tools
`beacon_radar.py` aufs Handy bringt. Das Handy ist dann selbst der Sensor — tragbar,
mit Hintergrund-Scan und nativen Benachrichtigungen (auch bei ausgeschaltetem Display).

## Status
**v0.4** — baut per GitHub Actions fehlerfrei (Debug-APK, siehe unten). Ein Test auf einem
echten Gerät ist im Repo nicht dokumentiert (BLE geht nicht im Emulator).

**Download:** [Neuestes APK](https://github.com/mhd6271/beacon-radar-android/releases/tag/latest)
(Debug-Build, wird bei jedem Push auf `main` neu erzeugt).

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
- **Radar-Ansicht** (Mitte = du, Abstand zur Mitte = Signalstärke, Winkel bedeutungslos);
  blukii-Fobs werden hervorgehoben (ab v0.2)
- **Zonen per Touch**: Ringe im Radar ziehen, Schwellen werden gespeichert;
  „Zonen zurücksetzen" stellt die Standardwerte wieder her (v0.4)
- **Verfolgen**: Gerät antippen → Balken + 🔥 wärmer / ❄️ kälter (v0.4)

## Projektstruktur
```
app/src/main/
  AndroidManifest.xml
  java/com/maxm/beaconradar/
    Models.kt          Datenklassen (Device, IBeacon, HistoryEntry)
    Classify.kt        Zonen-Einstufung, iBeacon-Parsing, Typ-Erkennung
    Zones.kt           verschiebbare Zonen-Schwellen (SharedPreferences)
    RadarView.kt       Radar-Canvas + Ziehen der Zonen-Ringe
    Notifier.kt        Notification-Kanäle + Alarm
    BleRepository.kt   Scan + Zustand (StateFlow) + „sehr nah"-Logik + Verfolgen/Trend
    ScanService.kt     Foreground-Service
    MainActivity.kt    Compose-UI (Radar, Verfolgen, Geräteliste, Verlauf, Start/Stop)
  res/values/          strings.xml, themes.xml
```

## Bauen (Debug-APK)
Benötigt JDK 17 + Android SDK (Plattform 34). Der Gradle-Wrapper (8.9) ist eingecheckt.
```bash
./gradlew assembleDebug
# Ergebnis: app/build/outputs/apk/debug/app-debug.apk
```
Auf dem Handy: „Installation aus unbekannten Quellen" erlauben, APK öffnen/installieren.

Die CI (`.github/workflows/build.yml`) baut bei jedem Push und lädt das APK auf `main`
zusätzlich in das Release `latest` hoch (überschreibt das vorige).

## Nächste Schritte (TODO)
- Verlauf **persistent** speichern (Room oder Datei) — überlebt dann App-Neustart
- **Ignorier-Liste** für eigene Geräte (nur fremde lösen Alarm aus)
- Akku-Tuning (Scan-Intervall / `SCAN_MODE_BALANCED`)
