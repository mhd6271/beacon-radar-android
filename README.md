# Beacon Radar — Android (Port von beacon_radar.py)

Native Android-App (Kotlin + Jetpack Compose), die die Logik des Python-Laptop-Tools
`beacon_radar.py` aufs Handy bringt. Das Handy ist dann selbst der Sensor — tragbar,
mit Hintergrund-Scan und nativen Benachrichtigungen (auch bei ausgeschaltetem Display).

## Status
**v0.6** — baut per GitHub Actions fehlerfrei (Debug-APK, siehe unten). v0.5 am 01.11.2026
auf einem Pixel 6a getestet: läuft (BLE geht nicht im Emulator). Die Ignorier-Liste (v0.6)
ist auf dem Pixel installiert, ihre Bedienung dort aber noch nicht geprüft.

**Download:** [Neuestes APK](https://github.com/mhd6271/beacon-radar-android/releases/tag/latest)
(Debug-Build, wird bei jedem Push auf `main` neu erzeugt). Ab v0.5.1 signiert die CI mit
einem festen Schlüssel: Neue Fassungen lassen sich drüberinstallieren, Zonen und Verlauf
bleiben erhalten. (Wer noch v0.5 oder älter hat, muss einmal deinstallieren.)

## Features (portiert)
- BLE-Scan über `BluetoothLeScanner`
- EWMA-Glättung des RSSI
- Zonen: **sehr nah −60 / nah −72 / mittel −83 / weit −90 dBm**
- **Alarm bei Eintritt in „sehr nah"**: Notification + Ton + Vibration, mit Hysterese
  (erster Kontakt löst nicht aus) und 1,5-s-Entprellung
- **Verlauf/History** der Eintritte (Zeit · Gerät · Typ · Signal), wird gespeichert
  (`history.json` im App-Speicher, max. 500 Einträge) und lässt sich leeren (v0.5)
- **Typ-Erkennung** über Hersteller-Company-ID + iBeacon-Parsing
  (inkl. Fix: iBeacons werden nicht mehr fälschlich als „Apple" gelabelt)
- **Foreground-Service** für Scan im Hintergrund
- **Radar-Ansicht** (Mitte = du, Abstand zur Mitte = Signalstärke, Winkel bedeutungslos);
  blukii-Fobs werden hervorgehoben (ab v0.2)
- **Zonen per Touch**: Ringe im Radar ziehen, Schwellen werden gespeichert;
  „Zonen zurücksetzen" stellt die Standardwerte wieder her (v0.4)
- **Verfolgen**: Gerät antippen → Balken + 🔥 wärmer / ❄️ kälter (v0.4)
- **Eigene Geräte** (Ignorier-Liste): Gerät in der Liste lange antippen → 🏠, grau im Radar,
  löst keinen Alarm und keinen Verlaufseintrag aus; nochmal lange tippen hebt das auf.
  Gespeichert pro Gerät (iBeacon-Kennung bzw. MAC). Handys wechseln ihre MAC regelmäßig,
  dort hält die Markierung deshalb nur begrenzt (v0.6)

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
zusätzlich in das Release `latest` hoch (überschreibt das vorige). Ein Commit mit
`[skip ci]` in der Nachricht löst keinen Build aus (z. B. reine Doku-Änderungen).

### Signatur
Die CI signiert mit einem festen Schlüssel aus den GitHub-Secrets `KEYSTORE_BASE64`,
`KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Original auf Max' Mac:
`~/.android-signing/beacon-radar/` (`beacon-radar.jks` + `credentials.env`, nicht im Repo).
Zertifikat SHA-256: `4c01b02c…45b0964b`.
**Den Schlüssel nicht verlieren:** Ohne ihn lässt sich keine Fassung mehr über die
installierte App installieren.

Lokal mit demselben Schlüssel bauen (sonst nimmt Gradle den Debug-Schlüssel des Rechners):
```bash
export KEYSTORE_FILE=~/.android-signing/beacon-radar/beacon-radar.jks
set -a; . ~/.android-signing/beacon-radar/credentials.env; set +a
./gradlew assembleDebug
```

## Nächste Schritte (TODO)
- (derzeit nichts offen)

## Entscheidungen
- **Scan-Modus bleibt `SCAN_MODE_LOW_LATENCY`**, auch im Hintergrund (Max, 01.11.2026):
  schnelle Alarme sind wichtiger als Akku. Kein `BALANCED`-Tuning.
