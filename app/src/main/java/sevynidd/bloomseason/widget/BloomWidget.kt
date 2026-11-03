package sevynidd.bloomseason.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import sevynidd.bloomseason.MainActivity
import sevynidd.bloomseason.R
import sevynidd.bloomseason.core.Bleeding
import sevynidd.bloomseason.core.CycleModel
import sevynidd.bloomseason.core.Season
import sevynidd.bloomseason.core.daysBetween
import sevynidd.bloomseason.core.detectPeriods
import sevynidd.bloomseason.data.BloomDatabase
import sevynidd.bloomseason.data.SettingsStore
import sevynidd.bloomseason.ui.label
import sevynidd.bloomseason.ui.theme.seasonColorsFor

/** Drei Textzeilen des Widgets; `season` bestimmt die Farben, null heißt noch keine Daten. */
data class WidgetContent(val season: Season?, val title: String, val big: String, val sub: String)

/** Mit neutralem Text steht kein Wort zur Periode auf dem Startbildschirm. */
fun widgetContent(model: CycleModel, today: LocalDate, neutral: Boolean): WidgetContent {
    val prediction = model.prediction
    val phase = model.phaseOn(today)
    if (prediction == null || phase == null) return WidgetContent(null, "BloomSeason", "Noch keine Daten", "")
    val title = phase.season.label
    if (neutral) return WidgetContent(phase.season, title, "Tag ${phase.cycleDay}", "")
    if (phase.season == Season.WINTER) return WidgetContent(phase.season, title, "Tag ${phase.cycleDay}", "Periode läuft")
    val from = daysBetween(today, prediction.earliest)
    val to = daysBetween(today, prediction.latest)
    val big = when {
        to < 0 -> "seit ${-to} ${days(-to)} überfällig"
        from <= 0 -> "jetzt erwartet"
        from == to -> "in $from ${days(from)}"
        else -> "in $from–$to Tagen"
    }
    return WidgetContent(phase.season, "$title · Tag ${phase.cycleDay}", big, "nächste Periode")
}

private fun days(count: Int) = if (count == 1) "Tag" else "Tagen"

class BloomWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        async { updateWidgets(context) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_DATE_CHANGED) async { updateWidgets(context) } else super.onReceive(context, intent)
    }

    private fun async(block: suspend () -> Unit) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                block()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** Zeichnet alle Widgets neu, etwa nach einer Änderung der Einträge. */
        fun refresh(context: Context) {
            val app = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch { updateWidgets(app) }
        }
    }
}

private suspend fun updateWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(ComponentName(context, BloomWidget::class.java))
    if (ids.isEmpty()) return
    val today = LocalDate.now()
    val days = BloomDatabase.get(context).dao().allDays()
    val periodDays = days.filter { Bleeding.fromLevel(it.bleeding).isPeriodDay }.map { LocalDate.ofEpochDay(it.epochDay) }
    val neutral = SettingsStore(context).settings.first().neutralText
    val content = widgetContent(CycleModel(detectPeriods(periodDays), today), today, neutral)

    val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val palette = seasonColorsFor(dark).of(content.season ?: Season.SPRING)
    val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    val views = RemoteViews(context.packageName, R.layout.widget_bloom).apply {
        setColorStateList(R.id.widget_root, "setBackgroundTintList", ColorStateList.valueOf(palette.container.toArgb()))
        setTextViewText(R.id.widget_season, content.title)
        setTextViewText(R.id.widget_big, content.big)
        setTextViewText(R.id.widget_sub, content.sub)
        setViewVisibility(R.id.widget_sub, if (content.sub.isEmpty()) View.GONE else View.VISIBLE)
        listOf(R.id.widget_season, R.id.widget_big, R.id.widget_sub).forEach { setTextColor(it, palette.onContainer.toArgb()) }
        setOnClickPendingIntent(R.id.widget_root, open)
    }
    manager.updateAppWidget(ids, views)
}
