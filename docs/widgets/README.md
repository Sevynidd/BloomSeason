# Widget-Entwürfe

SVG-Vorlagen der drei Widgets zum Import in FigCraft oder ein anderes Figma-ähnliches Tool. Texte sind echter Text, jede Ebene hat eine ID (`background`, `title`, `headline`, `year-bar` …). Maße sind dp, im SVG 1 dp = 1 px.

| Datei | Inhalt |
|---|---|
| `widgets-overview.svg` | Alle drei Größen, hell und dunkel, dazu das kompakte Widget in allen vier Jahreszeiten |
| `widget-compact-{light,dark}.svg` | Kompakt, 2×2, 160 × 160 |
| `widget-medium-{light,dark}.svg` | Standard, 3×2, 250 × 160 |
| `widget-large-{light,dark}.svg` | Groß mit Jahreslauf, 4×3, 340 × 250 |

Alle Entwürfe zeigen den Frühling mit Beispieltexten. Die Jahreszeit ändert nur die Farben.

## Maße und Abstände

| Widget | Größe | Innenabstand | Ausrichtung |
|---|---|---|---|
| Kompakt | 160 × 160 | 12 | mittig, vertikal zentriert |
| Standard | 250 × 160 | 16 | links, vertikal zentriert |
| Groß | 340 × 250 | 20 | links, vertikal zentriert |

Eckenradius 28 (Systemvorgabe für Widgets). Die tatsächliche Größe richtet sich nach dem Launcher, die Widgets lassen sich in der Größe ziehen. Der Inhalt bleibt vertikal zentriert.

## Texte

| Ebene | Kompakt | Standard | Groß |
|---|---|---|---|
| Jahreszeit / Titel | 13, normal | 14, normal | 15, normal |
| Große Zahl / Zeile | 30, fett | 24, fett | 32, fett |
| Unterzeile | 12, normal | 13, normal | 14, normal |
| Zeitfenster | – | – | 13, normal |

Schrift: Roboto (Systemschrift). Schriftgrößen in sp. Textfarbe ist immer `onContainer`, Hintergrund `container`.

## Farben

| Jahreszeit | Hell: accent / container / onContainer | Dunkel: accent / container / onContainer |
|---|---|---|
| Winter | `3B5F8C` / `D8E4F3` / `142D4A` | `A9C4E6` / `23324A` / `D6E3F7` |
| Frühling | `3D6A3A` / `D6EACD` / `11290F` | `A6D49B` / `24392A` / `CDEBC4` |
| Sommer | `7A5B0F` / `F4E3B8` / `2E2100` | `E8C677` / `3E3119` / `F6E2B4` |
| Herbst | `9B4A28` / `F0DDCC` / `3A1406` | `EBA484` / `47291C` / `F8D5C5` |

Der Jahreslauf im großen Widget nutzt die vier `accent`-Farben des jeweiligen Themas, der Punkt für heute ist `onContainer` mit einem Ring in `container`.

## Jahreslauf (nur groß)

Band 300 × 16 mit Radius 8, 20 vom linken Rand. Die Abschnitte teilen den Zyklus nach Tagen: Winter, Frühling, Sommer, Herbst. Im Entwurf für einen 28-Tage-Zyklus mit 5 Tagen Periode sind die Breiten 53,6 / 85,7 / 32,1 / 128,6. Der Punkt (Radius 6, Ring 9) sitzt auf dem heutigen Tag.

## Zustände und Texte

| Zustand | Kompakt | Standard und Groß |
|---|---|---|
| Vor der Periode | „18–26“ / „Tage bis zur Periode“ | „in 18–26 Tagen“ / „nächste Periode“ |
| Genau ein Tag | „1“ / „Tag bis zur Periode“ | „in 1 Tag“ |
| Jetzt erwartet | „jetzt“ / „erwartet“ | „jetzt erwartet“ |
| Überfällig | „+3“ / „überfällig“ | „seit 3 Tagen überfällig“ |
| Periode läuft | „Tag 3“ / „Periode läuft“ | „Tag 3“ / „Periode läuft“, groß zusätzlich „voraussichtliches Ende in 2–4 Tagen“ |
| Neutraler Text | „Tag 7“, keine Unterzeile | „Tag 7“, keine Unterzeile |
| Noch keine Daten | „–“ / „keine Daten“ | „Noch keine Daten“ |

Im großen Widget steht unter dem Titel im Frühling bis Herbst „Jahreszeit · Tag N“, in der Periode nur die Jahreszeit. Das Zeitfenster („voraussichtlich 21.–29. November“) gibt es nur vor der Periode.

## Zurück an die App

Du kannst mir die geänderten Werte als Liste schicken (Farben, Größen, Abstände, Schriftgrößen, Texte) oder die exportierten SVG-Dateien. Umsetzbar sind Farben, Abstände, Schriftgrößen und Texte, außerdem eine andere Anordnung der Zeilen. Eigene Schriften und Verläufe brauchen mehr Aufwand.
