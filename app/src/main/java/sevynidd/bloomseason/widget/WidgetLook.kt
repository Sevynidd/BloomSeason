package sevynidd.bloomseason.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.PathParser
import sevynidd.bloomseason.core.Season

/** Farben eines Widgets nach Jahreszeit und Helligkeit, aus dem Entwurf in docs/widgets/seasons. */
internal class WidgetLook(
    val background: Int,
    val text: Int,
    /** Hauptfarbe des Motivs, bei den Herbstblättern dazu zwei weitere. */
    val motif: IntArray,
    val dot: Int,
)

/** Die vier Farben des Jahreslaufs: Winter, Frühling, Sommer, Herbst. */
internal fun barColors(dark: Boolean): IntArray =
    if (dark) intArrayOf(0xFF93BAEB.toInt(), 0xFF7BCB68.toInt(), 0xFFEBBC51.toInt(), 0xFFEE9770.toInt())
    else intArrayOf(0xFF6885AA.toInt(), 0xFF57B95E.toInt(), 0xFFCFA42C.toInt(), 0xFFCC744F.toInt())

internal fun widgetLook(season: Season, dark: Boolean, large: Boolean): WidgetLook {
    fun c(rgb: Long) = (0xFF000000 or rgb).toInt()
    return if (dark) when (season) {
        Season.SPRING -> WidgetLook(c(0x334D3B), c(0xCAEBC0), intArrayOf(c(0x95C988)), c(0xBCD8B3))
        Season.SUMMER -> WidgetLook(c(if (large) 0x7C6024 else 0x6C581F), c(0xEDDBB1), intArrayOf(c(0xF0BB3F)), c(0xEBD198))
        Season.AUTUMN -> WidgetLook(c(0x523B25), c(0xEBCDBF), intArrayOf(c(0xD79C62), c(0xC6804B), c(0xDFBB97)), c(0xE9BCA8))
        Season.WINTER -> WidgetLook(c(0x1C2F4C), c(0xD6DFEC), intArrayOf(c(0x83A7D3)), c(0xBBCAE0))
    } else when (season) {
        Season.SPRING -> WidgetLook(c(0xD2EBBE), c(0x154A11), intArrayOf(c(0x2A9031)), c(0x30842A))
        Season.SUMMER -> WidgetLook(c(0xF6E5AF), c(0x835E00), intArrayOf(c(0xCDA32E)), c(0x8F7124))
        Season.AUTUMN -> WidgetLook(c(0xF5D6B0), c(0x81501B), intArrayOf(c(0xD6A473), c(0xD39252), c(0xA16B40)), c(0xAC4D2A))
        Season.WINTER -> WidgetLook(c(0xE0E8ED), c(0x4E7AAD), intArrayOf(c(0x749BCC)), c(0x1E4E85))
    }
}

/** Ein Teil des Motivs. `fill` und `stroke` sind Indizes in [WidgetLook.motif], 9 steht für die Hintergrundfarbe. */
private class Shape(val path: String, val fill: Int?, val stroke: Int?, val width: Float, val round: Boolean = false)

/** Motive auf einem Raster von 64 Einheiten, im Entwurf nach dem Faktor der Widgetgröße skaliert. */
private fun shapesOf(season: Season): List<Shape> = when (season) {
    Season.SPRING -> listOf(
        Shape("M 32.00 30.00 L 32.00 60.00", null, 0, 3f, true),
        Shape("M 19.00 41.00 C 19.00 46.00 23.00 52.00 32.00 52.00 C 32.00 45.00 27.00 41.00 19.00 41.00 Z", 0, null, 0f),
        Shape("M 45.00 35.00 C 45.00 40.00 41.00 46.00 32.00 46.00 C 32.00 39.00 37.00 35.00 45.00 35.00 Z", 0, null, 0f),
        Shape("M 32.00 21.00 C 28.69 21.00 26.00 16.52 26.00 11.00 C 26.00 5.48 28.69 1.00 32.00 1.00 C 35.31 1.00 38.00 5.48 38.00 11.00 C 38.00 16.52 35.31 21.00 32.00 21.00", 0, null, 0f),
        Shape("M 32.95 21.69 C 31.93 18.54 35.35 14.60 40.61 12.89 C 45.86 11.19 50.95 12.36 51.97 15.51 C 53.00 18.66 49.57 22.60 44.32 24.31 C 39.06 26.01 33.97 24.84 32.95 21.69", 0, null, 0f),
        Shape("M 32.59 22.81 C 35.27 20.86 40.07 22.90 43.32 27.37 C 46.57 31.84 47.02 37.04 44.34 38.99 C 41.66 40.94 36.86 38.89 33.61 34.43 C 30.37 29.96 29.91 24.76 32.59 22.81", 0, null, 0f),
        Shape("M 31.41 22.81 C 34.09 24.76 33.63 29.96 30.39 34.43 C 27.14 38.89 22.34 40.94 19.66 38.99 C 16.98 37.04 17.43 31.84 20.68 27.37 C 23.93 22.90 28.73 20.86 31.41 22.81", 0, null, 0f),
        Shape("M 31.05 21.69 C 30.02 24.84 24.94 26.01 19.68 24.31 C 14.43 22.60 11.00 18.66 12.03 15.51 C 13.05 12.36 18.14 11.19 23.39 12.89 C 28.64 14.60 32.07 18.54 31.05 21.69", 0, null, 0f),
        Shape("M 36.50 22.00 C 36.50 24.48 34.48 26.50 32.00 26.50 C 29.52 26.50 27.50 24.48 27.50 22.00 C 27.50 19.52 29.52 17.50 32.00 17.50 C 34.48 17.50 36.50 19.52 36.50 22.00", 9, null, 0f),
    )
    Season.SUMMER -> listOf(
        Shape("M 43.00 32.00 C 43.00 38.07 38.07 43.00 32.00 43.00 C 25.92 43.00 21.00 38.07 21.00 32.00 C 21.00 25.92 25.92 21.00 32.00 21.00 C 38.07 21.00 43.00 25.92 43.00 32.00", 0, null, 0f),
        Shape("M 32.00 9.00 L 32.00 15.00", null, 0, 3f, true),
        Shape("M 48.26 15.74 L 44.02 19.98", null, 0, 3f, true),
        Shape("M 55.00 32.00 L 49.00 32.00", null, 0, 3f, true),
        Shape("M 48.26 48.26 L 44.02 44.02", null, 0, 3f, true),
        Shape("M 32.00 55.00 L 32.00 49.00", null, 0, 3f, true),
        Shape("M 15.74 48.26 L 19.98 44.02", null, 0, 3f, true),
        Shape("M 9.00 32.00 L 15.00 32.00", null, 0, 3f, true),
        Shape("M 15.74 15.74 L 19.98 19.98", null, 0, 3f, true),
    )
    Season.AUTUMN -> listOf(
        Shape("M 34.33 51.43 C 42.88 35.55 31.61 14.36 13.67 12.58 C 5.12 28.45 16.39 49.64 34.33 51.43 Z", 0, null, 0f),
        Shape("M 17.43 19.64 L 38.08 58.49", null, 9, 1.6f, true),
        Shape("M 35.52 36.14 C 48.10 37.08 58.44 23.85 54.48 11.86 C 41.90 10.92 31.55 24.16 35.52 36.14 Z", 1, null, 0f),
        Shape("M 51.03 16.28 L 32.07 40.55", null, 9, 1.12f, true),
        Shape("M 30.31 44.87 C 33.49 54.26 46.24 57.68 53.69 51.13 C 50.51 41.74 37.76 38.32 30.31 44.87 Z", 2, null, 0f),
        Shape("M 49.44 49.99 L 26.06 43.73", null, 9, 0.88f, true),
    )
    Season.WINTER -> listOf(
        Shape("M 27.00 33.00 L 27.00 14.00 M 27.00 22.00 L 22.00 17.00 M 27.00 22.00 L 32.00 17.00", null, 0, 2.6f, true),
        Shape("M 27.00 33.00 L 43.45 23.50 M 36.53 27.50 L 38.36 20.67 M 36.53 27.50 L 43.36 29.33", null, 0, 2.6f, true),
        Shape("M 27.00 33.00 L 43.45 42.50 M 36.53 38.50 L 43.36 36.67 M 36.53 38.50 L 38.36 45.33", null, 0, 2.6f, true),
        Shape("M 27.00 33.00 L 27.00 52.00 M 27.00 44.00 L 32.00 49.00 M 27.00 44.00 L 22.00 49.00", null, 0, 2.6f, true),
        Shape("M 27.00 33.00 L 10.55 42.50 M 17.47 38.50 L 15.64 45.33 M 17.47 38.50 L 10.64 36.67", null, 0, 2.6f, true),
        Shape("M 27.00 33.00 L 10.55 23.50 M 17.47 27.50 L 10.64 29.33 M 17.47 27.50 L 15.64 20.67", null, 0, 2.6f, true),
        Shape("M 50.00 14.00 L 50.00 6.02 M 50.00 9.38 L 47.90 7.28 M 50.00 9.38 L 52.10 7.28", null, 0, 1.09f, true),
        Shape("M 50.00 14.00 L 56.91 10.01 M 54.00 11.69 L 54.77 8.82 M 54.00 11.69 L 56.87 12.46", null, 0, 1.09f, true),
        Shape("M 50.00 14.00 L 56.91 17.99 M 54.00 16.31 L 56.87 15.54 M 54.00 16.31 L 54.77 19.18", null, 0, 1.09f, true),
        Shape("M 50.00 14.00 L 50.00 21.98 M 50.00 18.62 L 52.10 20.72 M 50.00 18.62 L 47.90 20.72", null, 0, 1.09f, true),
        Shape("M 50.00 14.00 L 43.09 17.99 M 46.00 16.31 L 45.23 19.18 M 46.00 16.31 L 43.13 15.54", null, 0, 1.09f, true),
        Shape("M 50.00 14.00 L 43.09 10.01 M 46.00 11.69 L 43.13 12.46 M 46.00 11.69 L 45.23 8.82", null, 0, 1.09f, true),
        Shape("M 51.20 50.00 C 51.20 51.21 50.21 52.20 49.00 52.20 C 47.79 52.20 46.80 51.21 46.80 50.00 C 46.80 48.78 47.79 47.80 49.00 47.80 C 50.21 47.80 51.20 48.78 51.20 50.00", 0, null, 0f),
        Shape("M 14.80 54.00 C 14.80 54.99 13.99 55.80 13.00 55.80 C 12.01 55.80 11.20 54.99 11.20 54.00 C 11.20 53.01 12.01 52.20 13.00 52.20 C 13.99 52.20 14.80 53.01 14.80 54.00", 0, null, 0f),
    )}

/** Rand um das Raster, damit nichts abgeschnitten wird: das Bild umfasst die Einheiten −4 bis 68. */
private const val MARGIN_UNITS = 4f
private const val FRAME_UNITS = 64f + 2 * MARGIN_UNITS

/** Zeichnet das Motiv der Jahreszeit; [pixelsPerUnit] bestimmt die Auflösung. */
internal fun seasonArt(season: Season, look: WidgetLook, pixelsPerUnit: Float): Bitmap {
    val side = (FRAME_UNITS * pixelsPerUnit).toInt()
    val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.scale(pixelsPerUnit, pixelsPerUnit)
    canvas.translate(MARGIN_UNITS, MARGIN_UNITS)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    fun colorOf(index: Int) = if (index == 9) look.background else look.motif[index]
    shapesOf(season).forEach { shape ->
        val path = PathParser.createPathFromPathData(shape.path)
        shape.fill?.let {
            paint.style = Paint.Style.FILL
            paint.color = colorOf(it)
            canvas.drawPath(path, paint)
        }
        shape.stroke?.let {
            paint.style = Paint.Style.STROKE
            paint.color = colorOf(it)
            paint.strokeWidth = shape.width
            paint.strokeCap = if (shape.round) Paint.Cap.ROUND else Paint.Cap.BUTT
            canvas.drawPath(path, paint)
        }
    }
    return bitmap
}
