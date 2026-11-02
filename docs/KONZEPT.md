# BloomSeason – Konzept (Entwurf v0.1)

> Status: Diskussionsgrundlage. Alles mit **(Annahme)** ist ein Vorschlag, der noch bestätigt oder verworfen werden muss. Offene Entscheidungen stehen am Ende.

## 1. Vision

BloomSeason ist eine Perioden- und Zyklus-App, die den Zyklus als **Abfolge von Jahreszeiten** erzählt: Jede Phase hat ihren eigenen Charakter, und die App hilft, den eigenen Rhythmus zu verstehen, statt nur Tage zu zählen.

Leitidee: *ruhig, privat, ohne Scham und ohne Marketing-Rosa.*

## 2. Zielgruppe

- **Ein einziger Nutzer: der Entwickler selbst.** BloomSeason ist eine persönliche App, keine Veröffentlichung.
- Folge: Funktionen, Sprache und Design richten sich nach den eigenen Bedürfnissen. Es braucht keine Rücksicht auf fremde Zielgruppen, Stores oder Mehrsprachigkeit.
- Nicht im Fokus (MVP): Verhütung, medizinische Diagnostik
- Dauerhaft ausgeschlossen: Kinderwunsch und Fruchtbarkeits-Optimierung

## 3. Grundprinzipien

1. **Privacy first / local-first (Annahme):** Alle Daten bleiben auf dem Gerät. Kein Account, kein Tracking, keine Werbung, keine Drittanbieter-SDKs.
2. **Offen (Annahme):** Das Repo steht unter GPL-3.0, die App ist Open Source.
3. **Ehrlich bei Unsicherheit:** Vorhersagen werden als Bereich mit Konfidenz angezeigt, nicht als exaktes Datum.
4. **Kein Medizinprodukt:** Die App gibt keine Diagnosen. Sie ist im MVP keine Verhütungsmethode; eine spätere Verhütungs-Unterstützung muss diese Grenze ausdrücklich klären (siehe Abschnitt 5, „Später").
5. **Einfach halten:** Ein Nutzer, ein Gerät, ein Backup-Weg. Keine Funktion, die nur für andere Nutzer nötig wäre.

## 4. Die Jahreszeiten-Metapher

| Phase | Jahreszeit | Charakter |
|---|---|---|
| Menstruation | Winter | Rückzug, Ruhe, Erholung |
| Follikelphase | Frühling | Aufbau, Energie kommt zurück |
| Eisprung | Sommer | Höhepunkt, Offenheit |
| Lutealphase | Herbst | Abschluss, Sensibilität, Vorbereitung |

Die Metapher ist das **zentrale Gestaltungsmerkmal** der App: Startseite, Kalender, Farben und Texte sprechen in Jahreszeiten. Sie bleibt **Orientierung, keine Wissenschaft**. Die nüchternen Phasennamen (Menstruation, Follikelphase, Eisprung, Lutealphase) erscheinen als Zusatzinfo in den Details.

## 5. Funktionsumfang

### MVP
- Periodenbeginn und -ende eintragen (Kalender- und Schnell-Eintrag)
- Vorhersage der nächsten Periode und der aktuellen Phase
- Tägliches Logging, bewusst schlank: **Blutungsstärke** und **Schmerzen** (Ort und Stärke)
- **Kalender:** Monatsansicht mit Periode, Phasen und Vorhersage
- **Phasen-Startseite:** aktuelle Jahreszeit und Tage bis zur nächsten Periode
- **Statistik:** durchschnittliche Zyklus- und Periodenlänge
- **Muster:** Schmerzen im Rückblick je Phase (setzt einige erfasste Zyklen voraus)
- Erinnerungen als lokale Benachrichtigungen: Hinweis vor der erwarteten Periode **und** tägliche Erinnerung zum Eintragen (Text optional neutral)
- App-Sperre (Biometrie/PIN)
- Export und Import der Daten (JSON/CSV), Backup-Datei vom Nutzer kontrolliert

### Später
- Weitere Werte, falls gewünscht: Stimmung, Energie, Schlaf, Temperatur, Zervixschleim, Medikamente, Freitext-Notiz (im MVP bewusst nicht enthalten)
- Verhütungs-Unterstützung (z. B. Erinnerung an die Pilleneinnahme oder Hinweis auf fruchtbare Tage). Eine reine Kalendervorhersage ist als alleinige Verhütungsmethode **unzuverlässig**; bevor das kommt, muss geklärt werden, was die App genau leisten soll und wie sie das ehrlich kommuniziert.
- Widgets (aktuelle Phase, Tage bis zur nächsten Periode)
- Verschlüsseltes Backup (Datei oder eigener Sync)
- Partner-Freigabe (nur falls irgendwann gewünscht, nur Teilinformationen)

### Bewusst nicht enthalten
- Mehrsprachigkeit (nur Deutsch)
- Store-Veröffentlichung (Installation per APK)
- Cloud-Konto, Social-Features, Community-Feed
- Werbung, Analytics, Datenverkauf
- Gesundheitsversprechen
- Kinderwunsch und Fruchtbarkeits-Optimierung (dauerhaft)

## 6. Vorhersagelogik (MVP)

Einfach und nachvollziehbar statt „KI":

- Durchschnitt und Standardabweichung der letzten *n* Zyklen (Annahme: n = 6, gewichtet nach Aktualität)
- Prognose als **Zeitfenster** („voraussichtlich in 3–5 Tagen") statt Einzeldatum
- Weniger als 3 erfasste Zyklen: Standardwerte (28 Tage Zyklus, 5 Tage Periode) mit deutlichem Hinweis „noch zu wenig Daten"
- Ausreißer (z. B. Zyklus > 60 Tage) können vom Nutzer aus der Berechnung ausgeschlossen werden
- Phasenberechnung: Eisprung etwa 14 Tage vor der nächsten Periode, daraus werden die vier Phasen abgeleitet

## 7. Datenmodell (Skizze)

- `Cycle` – Start, Ende (berechnet), Länge
- `PeriodDay` – Datum, Blutungsstärke
- `PainEntry` – Datum, Ort (Bauch, Kopf, Rücken …), Stärke
- `Settings` – Erinnerungen (Periode erwartet, tägliche Erinnerung), Sperre

Speicherung lokal, verschlüsselt (Annahme: SQLite + SQLCipher oder Room mit verschlüsseltem Backend).

## 8. Technik

Entschieden: Der Nutzer kennt Kotlin und Compose bereits und ist der einzige Nutzer der App.

- **Plattform:** Android (bestätigt: eigenes Gerät ist ein Android-Telefon)
- **Sprache/UI:** Kotlin, Jetpack Compose
- **Persistenz:** Room (+ Verschlüsselung)
- **Architektur:** MVVM, Berechnungslogik als reines Kotlin-Modul ohne Android-Abhängigkeit (leicht testbar). Kotlin Multiplatform ist **nicht** nötig; die Trennung hält die Tür offen, falls später iOS gebraucht wird.
- **Verteilung:** Debug-/Release-APK direkt aufs eigene Gerät, kein Play Store, kein Entwicklerkonto
- **minSdk:** 34 (Android 14), passend zum eigenen Gerät
- **Backup:** Export als verschlüsselte Datei in einen frei gewählten Ordner auf dem Gerät (Storage Access Framework). Wer den Ordner weitersynchronisiert (z. B. Syncthing, Nextcloud), tut das außerhalb der App.
- **Tests:** Unit-Tests für Vorhersagelogik, UI-Tests nur für die Kernflows

### Stand der Umsetzung (v0.9)

Das Design „Saisonlicht“ ([design-opus.html](design-opus.html)) ist in Compose umgesetzt, Hell und Dunkel folgen dem System, die aktuelle Jahreszeit bestimmt die Akzentfarbe.

- **Fertig:** Vorhersagelogik mit Tests (`core`), Heute, Kalender, Tag eintragen (beginnt rückwirkend eine Periode, werden die folgenden Tage in durchschnittlicher Länge vorbelegt), Monatswechsel per Wischen, Statistik und Muster, App-Sperre über die Bildschirmsperre des Geräts (beim Öffnen und nach 15 Sekunden im Hintergrund, mit Schutz vor Screenshots), Erinnerungen als Benachrichtigung (Uhrzeit einstellbar, optional neutraler Text), verschlüsseltes Backup (Passwort, AES-256-GCM) in einen frei gewählten Ordner, Export und Import als JSON oder CSV, Datenbank mit SQLCipher verschlüsselt (Schlüssel im Android Keystore), Beispieldaten im Debug-Build.
- **Noch nicht umgesetzt:** Widgets und weitere Werte (Stimmung, Schlaf, …), siehe „Später“. Fingerabdruck und Gesicht bei der App-Sperre sind nur auf dem Emulator mit PIN getestet.
- **Abweichung vom Entwurf:** In der Periode zeigt die Heute-Seite keinen Knopf „Periode beendet“. Eine Periode ergibt sich aus den erfassten Blutungstagen (ein freier Tag Lücke ist erlaubt).

## 9. Datenschutz und Recht

- Zyklusdaten sind Gesundheitsdaten und besonders sensibel. Bei rein privater Nutzung ohne Weitergabe greifen DSGVO-Pflichten wie Datenschutzerklärung oder Impressum nicht; das Schutzbedürfnis bleibt aber bestehen.
- Die App hat keine Netzwerkberechtigung und installiert nichts selbst. Updates holt Obtainium von den GitHub-Releases. Alle Zyklusdaten bleiben lokal; ein späterer Sync bleibt möglich.
- Die Datenbank ist mit SQLCipher verschlüsselt, Backups sind passwortgeschützt. Exporte (JSON/CSV) sind bewusst unverschlüsselt.
- **Das Repo ist öffentlich.** Deshalb gehören keine echten Daten, Backups, Testexporte oder Screenshots mit Zyklusdaten hinein. Die `.gitignore` schließt Datenbank-, Export- und Backup-Dateien aus. Für Tests und Screenshots nur erfundene Beispieldaten verwenden.
- Falls die App später doch an andere weitergegeben wird, gelten Datenschutzerklärung, Impressum und ein Hinweistext „keine medizinische Beratung, nicht zur Verhütung geeignet".

## 10. Design und Ton

- Ruhige, erdige Farbpalette, je Jahreszeit ein Akzent
- Naturillustrationen statt Symbolik, die Klischees bedient (kein Rosa-Blumen-Look)
- Sprache: warm, sachlich, kein Zwang zur Positivität
- Tarnmodus: neutrales App-Icon und Name optional (z. B. „Kalender")
- Barrierefreiheit von Anfang an (Kontrast, Schriftgrößen, Screenreader)

## 11. Roadmap (grob)

1. **Konzept und Wireframes** – dieses Dokument, Kernscreens siehe [wireframes.html](wireframes.html)
2. **Berechnungslogik** – Zyklusmodell und Vorhersage mit Tests
3. **MVP-App** – Logging, Kalender, Vorhersage, Sperre
4. **Alltagstest** – einige Zyklen selbst nutzen, Vorhersage mit der Realität vergleichen, nachjustieren

## 12. Offene Entscheidungen

Geklärt:
- Kotlin und Compose sind bekannt.
- Einziger Nutzer ist der Entwickler selbst (keine Store-Veröffentlichung, keine Monetarisierung, keine Mehrsprachigkeit).
- Das Zielgerät ist ein Android-Telefon mit Android 14 oder neuer (minSdk 34).
- Das Repo ist öffentlich (Vorsichtsregeln in Abschnitt 9).
- Backup: verschlüsselte Datei in einen frei gewählten Ordner auf dem Gerät.
- Sync bleibt später möglich; der MVP bleibt lokal und nutzt kein Netzwerk.

- Die Jahreszeiten-Metapher ist zentrales Gestaltungsmerkmal (Abschnitt 4).
- Kinderwunsch ist dauerhaft ausgeschlossen, Verhütung kann später dazukommen (Abschnitt 5, „Später").

Offen:
- Keine offenen Konzeptfragen mehr. Nächster Schritt: Wireframes der Kernscreens und Gradle-Projekt mit der Vorhersagelogik.
