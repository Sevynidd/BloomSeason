package sevynidd.bloomseason

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import sevynidd.bloomseason.reminder.ReminderScheduler
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sevynidd.bloomseason.core.Bleeding
import sevynidd.bloomseason.core.CycleModel
import sevynidd.bloomseason.core.DEFAULT_PERIOD_LENGTH
import kotlin.math.roundToInt
import sevynidd.bloomseason.core.PainSample
import sevynidd.bloomseason.core.detectPeriods
import sevynidd.bloomseason.data.AppSettings
import sevynidd.bloomseason.data.BloomDatabase
import sevynidd.bloomseason.data.DayEntryEntity
import sevynidd.bloomseason.data.PainEntryEntity
import sevynidd.bloomseason.data.SettingsStore
import sevynidd.bloomseason.data.backup.BackupRepository

data class PainItem(val location: String, val intensity: Int)

data class AppState(
    val today: LocalDate,
    val bleeding: Map<LocalDate, Bleeding>,
    val pain: Map<LocalDate, List<PainItem>>,
    val model: CycleModel,
    val settings: AppSettings,
    /** false, bis Datenbank und Einstellungen zum ersten Mal geladen sind. */
    val ready: Boolean = true,
) {
    val painSamples: List<PainSample>
        get() = pain.flatMap { (date, items) -> items.map { PainSample(date, it.intensity) } }

    val knownLocations: List<String>
        get() = pain.values.flatten().map { it.location }.distinct()

    companion object {
        fun empty(today: LocalDate) =
            AppState(today, emptyMap(), emptyMap(), CycleModel(emptyList(), today), AppSettings(), ready = false)
    }
}

/** So lange darf die App im Hintergrund sein, bevor sie wieder gesperrt ist. */
private const val LOCK_GRACE_MS = 15_000L

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = BloomDatabase.get(application).dao()
    private val settingsStore = SettingsStore(application)
    val backup = BackupRepository(application, dao, settingsStore::update)
    private val today = MutableStateFlow(LocalDate.now())

    val state: StateFlow<AppState> = combine(
        dao.observeDays(),
        dao.observePain(),
        settingsStore.settings,
        today,
    ) { days, pain, settings, today ->
        val bleeding = days.associate { LocalDate.ofEpochDay(it.epochDay) to Bleeding.fromLevel(it.bleeding) }
        val periodDays = bleeding.filterValues { it.isPeriodDay }.keys
        AppState(
            today = today,
            bleeding = bleeding,
            pain = pain.groupBy({ LocalDate.ofEpochDay(it.epochDay) }, { PainItem(it.location, it.intensity) }),
            model = CycleModel(detectPeriods(periodDays), today),
            settings = settings,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppState.empty(today.value))

    init {
        // Die tägliche Prüfung folgt den Erinnerungs-Einstellungen und wird bei jedem App-Start neu geplant.
        viewModelScope.launch {
            settingsStore.settings
                .map { Triple(it.remindExpected, it.remindDaily, it.dailyTime) }
                .distinctUntilChanged()
                .collect { ReminderScheduler.update(application, settingsStore.settings.first()) }
        }
    }

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked

    private var backgroundSince: Long? = null
    private var authenticating = false

    fun unlock() {
        _unlocked.value = true
    }

    /** Während einer Entsperr-Abfrage kann die Activity kurz pausieren; das zählt nicht als Hintergrund. */
    fun setAuthenticating(value: Boolean) {
        authenticating = value
    }

    fun onBackground() {
        if (_unlocked.value && !authenticating) backgroundSince = SystemClock.elapsedRealtime()
    }

    fun onForeground() {
        val since = backgroundSince ?: return
        backgroundSince = null
        if (SystemClock.elapsedRealtime() - since > LOCK_GRACE_MS) _unlocked.value = false
    }

    fun refreshToday() {
        today.value = LocalDate.now()
    }

    fun saveDay(date: LocalDate, bleeding: Bleeding, pain: Map<String, Int>) {
        val current = state.value
        viewModelScope.launch {
            val epochDay = date.toEpochDay()
            val day = bleeding.takeIf { it != Bleeding.NONE }?.let { DayEntryEntity(epochDay, it.level) }
            dao.saveDay(day, epochDay, pain.map { (location, intensity) -> PainEntryEntity(epochDay, location, intensity) })
            // Beginnt hier rückwirkend eine neue Periode, werden die folgenden Tage in üblicher Länge vorbelegt.
            val newStart = bleeding.isPeriodDay && date.isBefore(current.today) &&
                current.bleeding[date]?.isPeriodDay != true && current.bleeding[date.minusDays(1)]?.isPeriodDay != true
            if (newStart) {
                val length = (current.model.stats.meanPeriod?.roundToInt() ?: DEFAULT_PERIOD_LENGTH).coerceIn(2, 10)
                val prefill = (1 until length).map { date.plusDays(it.toLong()) }
                    .filter { !it.isAfter(current.today) && it !in current.bleeding }
                    .map { DayEntryEntity(it.toEpochDay(), bleeding.level) }
                if (prefill.isNotEmpty()) dao.insertDays(prefill)
            }
        }
    }

    fun startPeriodToday() {
        val date = today.value
        if (state.value.bleeding[date]?.isPeriodDay == true) return
        viewModelScope.launch { dao.upsertDay(DayEntryEntity(date.toEpochDay(), Bleeding.MEDIUM.level)) }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsStore.update(transform) }
    }

    /** Nur Debug-Builds: erfundene Beispieldaten, damit sich das Design ansehen lässt. */
    fun loadSampleData() {
        viewModelScope.launch {
            val (days, pain) = SampleData.generate(today.value)
            dao.replaceAll(days, pain)
        }
    }

    fun clearAllData() {
        viewModelScope.launch { dao.replaceAll(emptyList(), emptyList()) }
    }
}
