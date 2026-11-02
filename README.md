# Beacon Radar — Android (Port von beacon_radar.py)

Native Android-App (Kotlin + Jetpack Compose), die die Logik des Python-Laptop-Tools
`beacon_radar.py` ([mhd6271/beacon-radar](https://github.com/mhd6271/beacon-radar)) aufs Handy bringt. Das Handy ist dann selbst der Sensor — tragbar,
mit Hintergrund-Scan und nativen Benachrichtigungen (auch bei ausgeschaltetem Display).

## Status
**v0.6.1** — baut fehlerfrei, Unit-Tests grün (Debug-APK, siehe unten). v0.5 am 01.11.2026
auf einem Pixel 6a getestet: läuft (BLE geht nicht im Emulator). Ignorier-Liste (v0.6) und
die Korrekturen aus v0.6.1 sind auf einem Gerät noch nicht durchgeklickt.

**Download:** [Neuestes APK](https://github.com/mhd6271/beacon-radar-android/releases/tag/latest)
(Debug-Build, wird bei jedem Push auf `main` neu erzeugt). Ab v0.5.1 signiert die CI mit
einem festen Schlüssel: Neue Fassungen lassen sich drüberinstallieren, Zonen und Verlauf
bleiben erhalten. (Wer noch v0.5 oder älter hat, muss einmal deinstallieren.)

## Features (portiert)
- BLE-Scan über `BluetoothLeScanner`
- EWMA-Glättung des RSSI
- Zonen (Standardwerte, verstellbar): **sehr nah −60 / nah −72 / mittel −83 / weit −90 dBm**
- **Alarm bei Eintritt in „sehr nah"**: Notification + Ton + Vibration, mit Hysterese
  (erster Kontakt löst nicht aus; erst unter Schwelle −5 dBm gilt ein Gerät wieder als
  „draußen") und höchstens einem Alarm-Ton je 1,5 s
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

## Bedienung
1. App öffnen, Bluetooth- und Benachrichtigungs-Berechtigung erlauben, Bluetooth einschalten.
2. **Start** – scannt ab jetzt auch im Hintergrund (Meldung „Beacon Radar aktiv"). **Stop** beendet das.
3. **Radar:** Je näher ein Punkt an „du", desto stärker das Signal. Namen stehen ab Zone „nah".
   Ringe mit dem Finger ziehen verstellt die Zonen; „Zonen zurücksetzen" holt die Standardwerte.
4. **Gerät antippen** = verfolgen (Balken + wärmer/kälter), nochmal antippen oder „Lösen" beendet das.
5. **Gerät lange antippen** = eigenes Gerät (🏠, kein Alarm), nochmal lange antippen hebt es auf.
6. **Verlauf** listet jeden Eintritt in „sehr nah"; „Leeren" löscht ihn.

## Berechtigungen und Daten
- **Bluetooth-Scan/-Verbindung** (Android 12+) bzw. **Standort** (Android 8–11, dort für BLE-Scans
  Pflicht), **Benachrichtigungen** (Android 13+) für Alarm und Dauer-Meldung.
- Gespeichert wird nur auf dem Handy: Zonen, verfolgtes Gerät, eigene Geräte
  (SharedPreferences) und der Verlauf (`history.json`: Zeit, MAC, Name, Typ, Signal).
  Nichts wird ins Netz geschickt; die App hat keine Internet-Berechtigung.

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
app/src/test/java/com/maxm/beaconradar/
  ClassifyTest.kt      Unit-Tests (iBeacon-Parsing, Typ-Erkennung, Zonen, Glättung)
.github/workflows/build.yml   CI: Tests + APK + Release `latest`
```

## Bauen (Debug-APK)
Benötigt JDK 17 + Android SDK (Plattform 34; fehlende SDK-Pakete lädt Gradle selbst nach,
wenn die Lizenzen akzeptiert sind). Der Gradle-Wrapper (8.9) ist eingecheckt.
```bash
./gradlew testDebugUnitTest   # Unit-Tests
./gradlew assembleDebug       # Ergebnis: app/build/outputs/apk/debug/app-debug.apk
```
Auf dem Handy: „Installation aus unbekannten Quellen" erlauben, APK öffnen/installieren.

Die CI (`.github/workflows/build.yml`) führt bei jedem Push die Unit-Tests aus, baut und lädt das APK auf `main`
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

## Versionen
- **0.6.1** (02.11.2026, noch nicht veröffentlicht) — Liste/Radar aktualisieren sich bei jeder
  Signaländerung (vorher nur bei geänderter Reihenfolge), Start prüft Rechte und Bluetooth
  statt abzustürzen bzw. leer „aktiv" zu melden, alte Geräte werden aus dem Speicher
  entfernt, Unit-Tests, CI-Actions auf Node 24
- **0.6** (01.11.2026) — eigene Geräte (Ignorier-Liste)
- **0.5.1** (01.11.2026) — fester Signaturschlüssel, Updates ohne Deinstallieren
- **0.5** (01.11.2026) — Verlauf wird gespeichert + „Leeren"; Start/Stop zeigt echten Zustand
- **0.4** (15.10.2026) — Zonen ziehen, Verfolgen (wärmer/kälter)
- **0.3** (15.10.2026) — hellblaue Ringe, lesbare Beschriftung
- **0.2** (15.10.2026) — Radar-Ansicht, APK automatisch als Release
- **0.1** (14.10.2026) — Grundgerüst: Scan, Zonen, Alarm, Verlauf, Typ-Erkennung
