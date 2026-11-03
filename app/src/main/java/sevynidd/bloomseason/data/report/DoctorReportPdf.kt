package sevynidd.bloomseason.data.report

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.OutputStream
import java.time.format.DateTimeFormatter
import java.util.Locale
import sevynidd.bloomseason.core.Bleeding

private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val MARGIN = 40f
private const val ROW_HEIGHT = 20f
private const val FOOTER = "Selbst erfasste Daten aus der App BloomSeason. Keine Diagnose."

private val LongDate = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN)
private val ShortDate = DateTimeFormatter.ofPattern("dd.MM.yy", Locale.GERMAN)

private class Column(val title: String, val width: Float, val rightAligned: Boolean = false)

private val Columns = listOf(
    Column("Beginn", 62f),
    Column("Zyklus", 50f, true),
    Column("Periode", 52f, true),
    Column("Blutung", 60f),
    Column("Ø Schmerz", 62f, true),
    Column("Max.", 36f, true),
    Column("Schmerzorte", 193f),
)

/** Schreibt den Bericht als PDF in A4: Zusammenfassung oben, darunter eine Zeile je Zyklus, bei Bedarf auf mehreren Seiten. */
object DoctorReportPdf {
    fun write(report: DoctorReport, out: OutputStream) {
        val document = PdfDocument()
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f }
        val bold = Paint(body).apply { typeface = Typeface.DEFAULT_BOLD }
        val title = Paint(bold).apply { textSize = 20f }
        val muted = Paint(body).apply { color = 0xFF555555.toInt(); textSize = 9f }
        val rule = Paint().apply { color = 0xFFBBBBBB.toInt(); strokeWidth = 0.7f }

        var pageNumber = 0
        lateinit var page: PdfDocument.Page
        var y = 0f
        fun startPage() {
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            y = MARGIN
        }
        fun finishPage() {
            page.canvas.drawText(FOOTER, MARGIN, PAGE_HEIGHT - 24f, muted)
            val number = "Seite $pageNumber"
            page.canvas.drawText(number, PAGE_WIDTH - MARGIN - muted.measureText(number), PAGE_HEIGHT - 24f, muted)
            document.finishPage(page)
        }
        fun tableHeader() {
            var x = MARGIN
            Columns.forEach { column ->
                val text = column.title
                page.canvas.drawText(text, if (column.rightAligned) x + column.width - bold.measureText(text) else x, y + 13f, bold)
                x += column.width + 6f
            }
            y += ROW_HEIGHT
            page.canvas.drawLine(MARGIN, y - 4f, PAGE_WIDTH - MARGIN, y - 4f, rule)
        }

        startPage()
        page.canvas.drawText("Zyklusbericht", MARGIN, y + 20f, title)
        y += 34f
        page.canvas.drawText(
            "Zeitraum ${report.from.format(LongDate)} bis ${report.to.format(LongDate)}",
            MARGIN, y + 10f, body,
        )
        y += 28f

        val summary = report.summary
        val lines = listOf(
            "Erfasste Zyklen" to "${summary.cycles}",
            "Zykluslänge im Mittel" to (summary.meanCycle?.let { "${decimal(it)} Tage" + (summary.sdCycle?.let { sd -> " (± ${decimal(sd)})" } ?: "") } ?: "–"),
            "Kürzester / längster Zyklus" to if (summary.shortestCycle != null) "${summary.shortestCycle} / ${summary.longestCycle} Tage" else "–",
            "Periodendauer im Mittel" to (summary.meanPeriod?.let { "${decimal(it)} Tage" } ?: "–"),
            "Zyklen mit erfassten Schmerzen" to "${summary.cyclesWithPain}",
        )
        lines.forEach { (label, value) ->
            page.canvas.drawText(label, MARGIN, y + 10f, body)
            page.canvas.drawText(value, MARGIN + 190f, y + 10f, bold)
            y += 16f
        }
        y += 14f

        if (report.cycles.isEmpty()) {
            page.canvas.drawText("Im gewählten Zeitraum sind keine Perioden erfasst.", MARGIN, y + 10f, body)
        } else {
            tableHeader()
            report.cycles.forEachIndexed { row, cycle ->
                if (y + ROW_HEIGHT > PAGE_HEIGHT - 48f) {
                    finishPage()
                    startPage()
                    tableHeader()
                }
                val cells = listOf(
                    cycle.start.format(ShortDate),
                    cycle.cycleLength?.toString() ?: if (row == report.cycles.lastIndex) "läuft" else "–",
                    "${cycle.periodLength}",
                    bleedingName(cycle.strongestBleeding),
                    cycle.painAverage?.let(::decimal) ?: "–",
                    cycle.painMax?.toString() ?: "–",
                    cycle.painLocations.joinToString(", ").ifEmpty { "–" },
                )
                var x = MARGIN
                Columns.forEachIndexed { index, column ->
                    val text = fit(cells[index], column.width, body)
                    page.canvas.drawText(text, if (column.rightAligned) x + column.width - body.measureText(text) else x, y + 13f, body)
                    x += column.width + 6f
                }
                y += ROW_HEIGHT
            }
            y += 10f
            page.canvas.drawText("Zyklus = Tage von einem Periodenbeginn zum nächsten, Schmerz = Stärke von 0 bis 10 (Mittel der täglich stärksten Angabe).", MARGIN, y + 8f, muted)
        }
        finishPage()
        document.writeTo(out)
        document.close()
    }

    private fun bleedingName(bleeding: Bleeding) = when (bleeding) {
        Bleeding.LIGHT -> "leicht"
        Bleeding.MEDIUM -> "mittel"
        Bleeding.HEAVY -> "stark"
        Bleeding.SPOTTING -> "schmier"
        Bleeding.NONE -> "–"
    }

    /** Kürzt den Text mit Auslassungspunkten, wenn er nicht in die Spalte passt. */
    private fun fit(text: String, width: Float, paint: Paint): String {
        if (paint.measureText(text) <= width) return text
        val count = paint.breakText(text, true, width - paint.measureText("…"), null)
        return text.take(count) + "…"
    }
}

internal fun decimal(value: Double): String = "%.1f".format(Locale.GERMAN, value)

