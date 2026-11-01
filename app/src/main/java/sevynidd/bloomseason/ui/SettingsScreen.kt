package sevynidd.bloomseason.ui

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.VisibilityOff
import android.net.Uri
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.launch
import sevynidd.bloomseason.data.update.ApkDownloadStatus
import sevynidd.bloomseason.data.update.ApkInstaller
import sevynidd.bloomseason.data.update.GitHubRelease
import sevynidd.bloomseason.data.update.GitHubReleaseRepository
import sevynidd.bloomseason.data.update.UpdateCheck
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import sevynidd.bloomseason.data.AppSettings
import sevynidd.bloomseason.data.backup.BackupRepository
import sevynidd.bloomseason.ui.theme.Tabular

@Composable
fun SettingsScreen(
    settings: AppSettings,
    backup: BackupRepository,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onAppLockChange: (Boolean) -> Unit,
    onExternal: (Boolean) -> Unit,
    onMessage: (String) -> Unit,
    onLoadSample: () -> Unit,
    onClearAll: () -> Unit,
) {
    val debuggable = (LocalContext.current.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AppBar("Einstellungen")

        SectionLabel("Erinnerungen")
        SettingsGroup {
            SettingRow(Icons.Outlined.Notifications, "Periode erwartet", "2 Tage vor dem Zeitfenster") {
                Switch(settings.remindExpected, { v -> onChange { it.copy(remindExpected = v) } })
            }
            SettingRow(Icons.Outlined.Schedule, "Täglich eintragen", "${settings.dailyTime} Uhr") {
                Switch(settings.remindDaily, { v -> onChange { it.copy(remindDaily = v) } })
            }
            SettingRow(Icons.Outlined.VisibilityOff, "Neutraler Text", "„Zeit für deinen Eintrag“ statt „Periode“") {
                Switch(settings.neutralText, { v -> onChange { it.copy(neutralText = v) } })
            }
        }

        SectionLabel("Sicherheit", Modifier.padding(top = 6.dp))
        SettingsGroup {
            SettingRow(Icons.Outlined.Lock, "App-Sperre", "Biometrie oder PIN") {
                Switch(settings.appLock, onAppLockChange)
            }
        }
        Text(
            "Erinnerungen werden gespeichert, aber noch nicht ausgelöst. Die App-Sperre nutzt die Bildschirmsperre deines Geräts und greift beim Öffnen und nach 15 Sekunden im Hintergrund.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        SectionLabel("Daten", Modifier.padding(top = 6.dp))
        SettingsGroup {
            BackupRows(settings, backup, onMessage, onExternal)
        }

        SectionLabel("App", Modifier.padding(top = 6.dp))
        SettingsGroup { UpdateRow(onMessage) }

        if (debuggable) {
            SectionLabel("Debug", Modifier.padding(top = 6.dp))
            SettingsGroup {
                SettingRow(Icons.Outlined.Science, "Beispieldaten laden", "ersetzt alle Einträge durch erfundene Daten", onLoadSample) { Chevron() }
                SettingRow(Icons.Outlined.DeleteOutline, "Alle Daten löschen", "nur in Debug-Builds sichtbar", onClearAll) { Chevron() }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.PhoneAndroid, contentDescription = null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Alle Daten bleiben auf diesem Gerät. Das Netzwerk dient nur der Update-Suche.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val RELEASE_OWNER = "Sevynidd"
private const val RELEASE_REPO = "BloomSeason"

@Composable
private fun UpdateRow(onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val installer = remember { ApkInstaller(context.applicationContext) }
    val repository = remember { GitHubReleaseRepository(RELEASE_OWNER, RELEASE_REPO) }
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0" }
    var busy by remember { mutableStateOf(false) }
    var available by remember { mutableStateOf<GitHubRelease?>(null) }
    var pendingApk by remember { mutableStateOf<Uri?>(null) }
    var progress by remember { mutableStateOf<String?>(null) }

    // Nach der Erlaubnis „Unbekannte Apps installieren“ geht es direkt weiter.
    LifecycleResumeEffect(pendingApk) {
        val apk = pendingApk
        if (apk != null && installer.canInstall()) {
            pendingApk = null
            installer.install(apk)
        }
        onPauseOrDispose {}
    }

    fun install(apk: Uri) {
        if (installer.canInstall()) installer.install(apk) else {
            pendingApk = apk
            onMessage("Erlaube BloomSeason, Apps zu installieren.")
            installer.openInstallPermission()
        }
    }

    fun download(release: GitHubRelease) {
        busy = true
        scope.launch {
            installer.observe(installer.startDownload(release)).collect { status ->
                when (status) {
                    is ApkDownloadStatus.InProgress -> progress =
                        if (status.total > 0) "Lädt … ${status.downloaded * 100 / status.total} %" else "Lädt …"
                    is ApkDownloadStatus.Succeeded -> { busy = false; progress = null; install(status.uri) }
                    ApkDownloadStatus.Failed -> { busy = false; progress = null; onMessage("Download fehlgeschlagen.") }
                }
            }
        }
    }

    val release = available
    SettingRow(
        Icons.Outlined.SystemUpdate,
        if (release != null) "Version ${release.tagName.removePrefix("v")} installieren" else "Nach Updates suchen",
        progress ?: "Installiert: Version $version",
        onClick = if (busy) null else {
            {
                if (release != null) download(release) else {
                    busy = true
                    scope.launch {
                        when (val result = repository.check(version)) {
                            is UpdateCheck.Available -> { available = result.release; onMessage("Neue Version ${result.release.tagName.removePrefix("v")} verfügbar.") }
                            UpdateCheck.UpToDate -> onMessage("Du hast die neueste Version.")
                            UpdateCheck.Failed -> onMessage("Update-Prüfung nicht möglich.")
                        }
                        busy = false
                    }
                }
            }
        },
    ) {
        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Chevron()
    }
}

@Composable
internal fun SettingsGroup(content: @Composable () -> Unit) {
    BloomCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)) { content() }
}

@Composable
internal fun Chevron() {
    Icon(Icons.Outlined.ChevronRight, contentDescription = null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit,
) {
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(icon, contentDescription = null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall.merge(Tabular), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            trailing()
        }
    }
}
