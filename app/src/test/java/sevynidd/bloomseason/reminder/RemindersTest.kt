package sevynidd.bloomseason.reminder

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import sevynidd.bloomseason.core.CycleModel
import sevynidd.bloomseason.core.Period
import sevynidd.bloomseason.data.AppSettings

class RemindersTest {
    private val first = LocalDate.of(2026, 1, 5)
    private val periods = (0 until 5).map { Period(first.plusDays(it * 28L), first.plusDays(it * 28L + 4)) }
    private val on = AppSettings(remindExpected = true, remindDaily = true)

    private fun model(today: LocalDate) = CycleModel(periods, today)

    @Test
    fun expectedReminderComesTwoDaysBeforeTheWindow() {
        val earliest = model(periods.last().end.plusDays(5)).prediction!!.earliest
        val day = earliest.minusDays(2)
        assertEquals(ReminderKind.EXPECTED, dueReminder(on, model(day), day, hasEntryToday = true))
        val other = day.minusDays(1)
        assertEquals(null, dueReminder(on, model(other), other, hasEntryToday = true))
    }

    @Test
    fun dailyReminderOnlyWithoutEntry() {
        val day = periods.last().end.plusDays(3)
        assertEquals(ReminderKind.DAILY, dueReminder(on, model(day), day, hasEntryToday = false))
        assertEquals(null, dueReminder(on, model(day), day, hasEntryToday = true))
    }

    @Test
    fun switchedOffRemindersStaySilent() {
        val day = periods.last().end.plusDays(3)
        assertEquals(null, dueReminder(AppSettings(remindExpected = false, remindDaily = false), model(day), day, false))
    }

    @Test
    fun invalidTimeFallsBackToEvening() {
        assertEquals(LocalTime.of(21, 0), parseTime("kaputt"))
        assertNotNull(parseTime("07:30"))
        assertEquals(LocalTime.of(7, 30), parseTime("07:30"))
    }
}
