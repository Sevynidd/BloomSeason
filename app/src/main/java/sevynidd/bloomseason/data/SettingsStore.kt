package sevynidd.bloomseason.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val remindExpected: Boolean = true,
    val remindDaily: Boolean = true,
    val dailyTime: String = "21:00",
    val neutralText: Boolean = false,
    val appLock: Boolean = false,
    /** Vom Nutzer gewählter Ordner (SAF-Tree-URI) für Sicherungen, leer solange keiner gewählt ist. */
    val backupFolder: String = "",
    val lastBackup: String = "",
    val autoBackup: Boolean = false,
)

class SettingsStore(private val context: Context) {
    private object Keys {
        val remindExpected = booleanPreferencesKey("remind_expected")
        val remindDaily = booleanPreferencesKey("remind_daily")
        val dailyTime = stringPreferencesKey("daily_time")
        val neutralText = booleanPreferencesKey("neutral_text")
        val appLock = booleanPreferencesKey("app_lock")
        val backupFolder = stringPreferencesKey("backup_folder")
        val lastBackup = stringPreferencesKey("last_backup")
        val autoBackup = booleanPreferencesKey("auto_backup")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[Keys.remindExpected] = next.remindExpected
            prefs[Keys.remindDaily] = next.remindDaily
            prefs[Keys.dailyTime] = next.dailyTime
            prefs[Keys.neutralText] = next.neutralText
            prefs[Keys.appLock] = next.appLock
            prefs[Keys.backupFolder] = next.backupFolder
            prefs[Keys.lastBackup] = next.lastBackup
            prefs[Keys.autoBackup] = next.autoBackup
        }
    }

    private fun Preferences.toSettings() = AppSettings(
        remindExpected = this[Keys.remindExpected] ?: true,
        remindDaily = this[Keys.remindDaily] ?: true,
        dailyTime = this[Keys.dailyTime] ?: "21:00",
        neutralText = this[Keys.neutralText] ?: false,
        appLock = this[Keys.appLock] ?: false,
        backupFolder = this[Keys.backupFolder] ?: "",
        lastBackup = this[Keys.lastBackup] ?: "",
        autoBackup = this[Keys.autoBackup] ?: false,
    )
}
