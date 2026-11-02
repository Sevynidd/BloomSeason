# BloomSeason

Persönliche Perioden-App für Android. Der Zyklus wird als Abfolge von Jahreszeiten erzählt. Alle Daten bleiben auf dem Gerät.

- Konzept: [docs/KONZEPT.md](docs/KONZEPT.md)
- Designvorschlag „Saisonlicht“: [docs/design-opus.html](docs/design-opus.html)
- Frühe Wireframes: [docs/wireframes.html](docs/wireframes.html)

## Aufbau

| Modul | Inhalt |
|---|---|
| `core` | Reines Kotlin ohne Android: Periodenerkennung, Statistik, Vorhersage, Phasen (mit Unit-Tests) |
| `app` | Android-App mit Jetpack Compose, Room und DataStore |

## Bauen und testen

Voraussetzungen: JDK 17, Android SDK (compileSdk 37). `local.properties` mit `sdk.dir` ist nicht eingecheckt.

```bash
./gradlew :core:test
./gradlew :app:assembleDebug
```

Die APK liegt danach unter `app/build/outputs/apk/debug/`. Im Debug-Build gibt es in den Einstellungen den Eintrag „Beispieldaten laden“ mit erfundenen Daten, um das Design anzusehen.

## Datenschutz

Keine Internetberechtigung, kein Android-Backup (`allowBackup=false`). Updates über [Obtainium](https://obtainium.imranr.dev/) mit der Repo-URL. Keine echten Zyklusdaten ins Repo einchecken.

## Lizenz

GPL-3.0, siehe [LICENSE](LICENSE).
