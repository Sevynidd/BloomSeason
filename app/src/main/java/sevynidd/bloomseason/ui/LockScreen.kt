package sevynidd.bloomseason.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import sevynidd.bloomseason.core.Season
import sevynidd.bloomseason.security.AppLock

@Composable
fun LockScreen(
    onUnlocked: () -> Unit,
    onDisableLock: () -> Unit,
    onAuthenticating: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val available = remember { AppLock.isAvailable(context) }
    var prompting by remember { mutableStateOf(false) }

    fun prompt() {
        if (prompting || !available) return
        prompting = true
        onAuthenticating(true)
        AppLock.authenticate(context, "BloomSeason entsperren") { success ->
            prompting = false
            onAuthenticating(false)
            if (success) onUnlocked()
        }
    }

    // Die Abfrage erscheint beim Öffnen und bei jeder Rückkehr in die App von selbst.
    LifecycleResumeEffect(Unit) {
        prompt()
        onPauseOrDispose { }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Season.SPRING.glyph, contentDescription = null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(20.dp))
            Text("BloomSeason ist gesperrt", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            if (available) {
                Text(
                    "Entsperre mit Fingerabdruck, Gesicht oder der Bildschirmsperre.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = ::prompt, modifier = Modifier.height(56.dp)) {
                    Icon(Icons.Outlined.LockOpen, contentDescription = null, Modifier.size(22.dp))
                    Text("Entsperren", modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Text(
                    "Auf diesem Gerät ist keine Bildschirmsperre mehr eingerichtet, daher kann die App nicht entsperrt werden.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onClick = onDisableLock, modifier = Modifier.height(48.dp)) { Text("App-Sperre ausschalten") }
            }
        }
    }
}
