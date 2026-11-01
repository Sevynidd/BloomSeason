package sevynidd.bloomseason.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import sevynidd.bloomseason.MainActivity
import sevynidd.bloomseason.R
import sevynidd.bloomseason.core.Bleeding
import sevynidd.bloomseason.core.CycleModel
import sevynidd.bloomseason.core.detectPeriods
import sevynidd.bloomseason.data.AppSettings
import sevynidd.bloomseason.data.BloomDatabase
import sevynidd.bloomseason.data.SettingsStore

/** So viele Tage vor dem frühesten Tag des Zeitfensters kommt die Erinnerung. */
private const val EXPECTED_LEAD_DAYS = 2L
private const val WORK_NAME = "reminders"
private const val CHANNEL_ID = "reminders"

enum class ReminderKind { EXPECTED, DAILY }

/** Entscheidet, welche Erinnerung heute fällig ist; die „erwartet“-Erinnerung hat Vorrang. */
fun dueReminder(settings: AppSettings, model: CycleModel, today: LocalDate, hasEntryToday: Boolean): ReminderKind? {
    if (settings.remindExpected) {
        val earliest = model.prediction?.earliest
        if (earliest != null && today == earliest.minusDays(EXPECTED_LEAD_DAYS)) return ReminderKind.EXPECTED
    }
    if (settings.remindDaily && !hasEntryToday) return ReminderKind.DAILY
    return null
}

fun parseTime(text: String): LocalTime = runCatching { LocalTime.parse(text) }.getOrDefault(LocalTime.of(21, 0))

object ReminderScheduler {
    /** Plant eine tägliche Prüfung zur eingestellten Uhrzeit oder löscht sie, wenn keine Erinnerung an ist. */
    fun update(context: Context, settings: AppSettings) {
        val work = WorkManager.getInstance(context)
        if (!settings.remindExpected && !settings.remindDaily) {
            work.cancelUniqueWork(WORK_NAME)
            return
        }
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(parseTime(settings.dailyTime))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        work.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
    }

    fun canNotify(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val settings = SettingsStore(applicationContext).settings.first()
        val today = LocalDate.now()
        val dao = BloomDatabase.get(applicationContext).dao()
        val days = dao.allDays()
        val periodDays = days.filter { Bleeding.fromLevel(it.bleeding).isPeriodDay }.map { LocalDate.ofEpochDay(it.epochDay) }
        val model = CycleModel(detectPeriods(periodDays), today)
        val todayEpoch = today.toEpochDay()
        val hasEntry = days.any { it.epochDay == todayEpoch } || dao.allPain().any { it.epochDay == todayEpoch }

        val kind = dueReminder(settings, model, today, hasEntry) ?: return Result.success()
        if (!ReminderScheduler.canNotify(applicationContext)) return Result.success()
        notify(kind, settings.neutralText)
        return Result.success()
    }

    private fun notify(kind: ReminderKind, neutral: Boolean) {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Erinnerungen", NotificationManager.IMPORTANCE_DEFAULT))
        val text = when {
            neutral -> "Zeit für deinen Eintrag."
            kind == ReminderKind.EXPECTED -> "Die nächste Periode wird in etwa $EXPECTED_LEAD_DAYS Tagen erwartet."
            else -> "Du hast heute noch nichts eingetragen."
        }
        val open = PendingIntent.getActivity(
            applicationContext, 0, Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("BloomSeason")
            .setContentText(text)
            .setContentIntent(open)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .build()
        manager.notify(kind.ordinal, notification)
    }
}
