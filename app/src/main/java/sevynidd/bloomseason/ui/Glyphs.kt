package sevynidd.bloomseason.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import sevynidd.bloomseason.core.Season

private fun lineGlyph(name: String, vararg paths: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        for (data in paths) {
            addPath(
                pathData = PathParser().parsePathString(data).toNodes(),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

private val WinterGlyph = lineGlyph(
    "Winter",
    "M12 3v18M4.2 7.5l15.6 9M4.2 16.5l15.6-9M9.6 4.6 12 7l2.4-2.4M9.6 19.4 12 17l2.4 2.4" +
        "M4.9 10.6l3.3-.9-.9-3.3M19.1 13.4l-3.3.9.9 3.3M4.9 13.4l3.3.9-.9 3.3M19.1 10.6l-3.3-.9.9-3.3",
)

private val SpringGlyph = lineGlyph(
    "Spring",
    "M12 21v-9M12 13C8 13 5 10.5 5 6.5c4 0 7 2.5 7 6.5zM12 10.5c0-3.6 2.6-6 6.5-6 0 3.6-2.6 6-6.5 6zM7 21h10",
)

private val SummerGlyph = lineGlyph(
    "Summer",
    "M8 13a4 4 0 1 0 8 0a4 4 0 1 0 -8 0",
    "M12 4.5v2M5.8 7l1.4 1.4M18.2 7l-1.4 1.4M3.5 13h2M18.5 13h2M3 19.5h18",
)

private val AutumnGlyph = lineGlyph(
    "Autumn",
    "M5 19C5 10.5 10.5 5 19 5c0 8.5-5.5 14-14 14zM5 19l9-9M3 21l2-2M9.5 14.5h3.5M12 12V8.8",
)

val Season.glyph: ImageVector
    get() = when (this) {
        Season.WINTER -> WinterGlyph
        Season.SPRING -> SpringGlyph
        Season.SUMMER -> SummerGlyph
        Season.AUTUMN -> AutumnGlyph
    }

/** Umriss eines Tropfens im 24-dp-Raster, für die Blutungsstärke. */
const val DROP_PATH = "M12 3.5c3.2 4 6 7.3 6 10.5a6 6 0 0 1-12 0c0-3.2 2.8-6.5 6-10.5z"

val Season.label: String
    get() = when (this) {
        Season.WINTER -> "Winter"
        Season.SPRING -> "Frühling"
        Season.SUMMER -> "Sommer"
        Season.AUTUMN -> "Herbst"
    }

val Season.phaseName: String
    get() = when (this) {
        Season.WINTER -> "Menstruation"
        Season.SPRING -> "Follikelphase"
        Season.SUMMER -> "Eisprung, geschätzt"
        Season.AUTUMN -> "Lutealphase"
    }

val Season.words: String
    get() = when (this) {
        Season.WINTER -> "Rückzug · Ruhe · Erholung"
        Season.SPRING -> "Aufbau · Energie kommt zurück"
        Season.SUMMER -> "Höhepunkt · Offenheit"
        Season.AUTUMN -> "Abschluss · Sensibilität · Vorbereitung"
    }
