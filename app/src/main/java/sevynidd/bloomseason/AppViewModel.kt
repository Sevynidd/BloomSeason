package sevynidd.bloomseason

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sevynidd.bloomseason.core.Bleeding
import sevynidd.bloomseason.core.CycleModel
import sevynidd.bloomseason.core.PainSample
import sevynidd.bloomseason.core.detectPeriods
import sevynidd.bloomseason.data.AppSettings
import sevynidd.bloomseason.data.BloomDatabase
import sevynidd.bloomseason.data.DayEntryEntity
import sevynidd.bloomseason.data.PainEntryEntity
import sevynidd.bloomseason.data.SettingsStore

data class PainItem(val location: String, val intensity: Int)

data class AppState(
    val today: LocalDate,
    val bleeding: Map<LocalDate, Bleeding>,
    val pain: Map<LocalDate, List<PainItem>>,
    val model: CycleModel,
    val settings: AppSettings,
) {
    val painSamples: List<PainSample>
        get() = pain.flatMap { (date, items) -> items.map { PainSample(date, it.intensity) } }

    val knownLocations: List<String>
        get() = pain.values.flatten().map { it.location }.distinct()

    companion object {
        fun empty(today: LocalDate) = AppState(today, emptyMap(), emptyMap(), CycleModel(emptyList(), today), AppSettings())
    }
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = BloomDatabase.create(application).dao()
    private val settingsStore = SettingsStore(application)
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

    fun refreshToday() {
        today.value = LocalDate.now()
    }

    fun saveDay(date: LocalDate, bleeding: Bleeding, pain: Map<String, Int>) {
        viewModelScope.launch {
            val epochDay = date.toEpochDay()
            val day = bleeding.takeIf { it != Bleeding.NONE }?.let { DayEntryEntity(epochDay, it.level) }
            dao.saveDay(day, epochDay, pain.map { (location, intensity) -> PainEntryEntity(epochDay, location, intensity) })
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
