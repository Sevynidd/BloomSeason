package sevynidd.bloomseason.data.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val TIMEOUT_MILLIS = 10_000
private const val HTTP_OK = 200

/** Ein veröffentlichtes Release mit installierbarer APK. */
data class GitHubRelease(
    val tagName: String,
    val notes: String,
    val apkUrl: String,
    val apkFileName: String,
)

/** Ergebnis der Update-Prüfung. */
sealed interface UpdateCheck {
    data class Available(val release: GitHubRelease) : UpdateCheck
    data object UpToDate : UpdateCheck
    data object Failed : UpdateCheck
}

/** Fragt das neueste Release von [owner]/[repo] ab (nur öffentliche Repos ohne Anmeldung). */
class GitHubReleaseRepository(private val owner: String, private val repo: String) {

    suspend fun check(currentVersion: String): UpdateCheck = withContext(Dispatchers.IO) {
        try {
            val release = fetchLatest() ?: return@withContext UpdateCheck.Failed
            if (isNewer(release.tagName, currentVersion)) UpdateCheck.Available(release) else UpdateCheck.UpToDate
        } catch (_: IOException) {
            UpdateCheck.Failed
        } catch (_: JSONException) {
            UpdateCheck.Failed
        }
    }

    private fun fetchLatest(): GitHubRelease? {
        val connection = URL("https://api.github.com/repos/$owner/$repo/releases/latest")
            .openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MILLIS
        connection.readTimeout = TIMEOUT_MILLIS
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        return try {
            if (connection.responseCode != HTTP_OK) return null
            parse(JSONObject(connection.inputStream.bufferedReader().readText()))
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(json: JSONObject): GitHubRelease? {
        val assets = json.getJSONArray("assets")
        val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
            .firstOrNull { it.getString("name").endsWith(".apk") } ?: return null
        return GitHubRelease(
            tagName = json.getString("tag_name"),
            notes = json.optString("body"),
            apkUrl = apk.getString("browser_download_url"),
            apkFileName = apk.getString("name"),
        )
    }
}

/** Vergleicht Versionen wie „v0.3.1“ und „0.2“ Zahl für Zahl. */
internal fun isNewer(tag: String, current: String): Boolean {
    fun parts(v: String) = v.trim().removePrefix("v").split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
    val a = parts(tag)
    val b = parts(current)
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}
