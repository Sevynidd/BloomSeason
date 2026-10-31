package sevynidd.bloomseason

import java.time.LocalDate
import sevynidd.bloomseason.core.Bleeding
import sevynidd.bloomseason.data.DayEntryEntity
import sevynidd.bloomseason.data.PainEntryEntity

/** Erfundene Daten für die Designvorschau: sechs abgeschlossene Zyklen, heute ist Zyklustag 9. */
object SampleData {
    private val cycleLengths = listOf(27, 31, 28, 29, 26, 28)
    private val flowPattern = listOf(Bleeding.MEDIUM, Bleeding.HEAVY, Bleeding.MEDIUM, Bleeding.LIGHT, Bleeding.LIGHT, Bleeding.SPOTTING)

    fun generate(today: LocalDate): Pair<List<DayEntryEntity>, List<PainEntryEntity>> {
        var start = today.minusDays(8)
        val starts = mutableListOf(start)
        for (length in cycleLengths.reversed()) {
            start = start.minusDays(length.toLong())
            starts += start
        }
        val days = mutableListOf<DayEntryEntity>()
        val pain = mutableListOf<PainEntryEntity>()
        for ((index, cycleStart) in starts.withIndex()) {
            for (offset in flowPattern.indices) {
                val date = cycleStart.plusDays(offset.toLong())
                if (date.isAfter(today)) continue
                days += DayEntryEntity(date.toEpochDay(), flowPattern[offset].level)
            }
            for ((offset, intensity) in listOf(0 to 6, 1 to 7, 2 to 5)) {
                val date = cycleStart.plusDays(offset.toLong())
                if (!date.isAfter(today)) pain += PainEntryEntity(date.toEpochDay(), "Bauch", intensity)
            }
            if (index % 2 == 0) {
                val date = cycleStart.plusDays(20)
                if (!date.isAfter(today)) pain += PainEntryEntity(date.toEpochDay(), "Kopf", 3)
            }
        }
        return days to pain
    }
}
