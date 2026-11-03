package sevynidd.bloomseason.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
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
import sevynidd.bloomseason.core.detectPeriods
import sevynidd.bloomseason.data.BloomDatabase
import sevynidd.bloomseason.data.SettingsStore
import sevynidd.bloomseason.ui.theme.SeasonColors
import sevynidd.bloomseason.ui.theme.SeasonPalette
import sevynidd.bloomseason.ui.theme.seasonColorsFor

private enum class Size(@LayoutRes val layout: Int) {
    COMPACT(R.layout.widget_bloom_small),
    MEDIUM(R.layout.widget_bloom),
    LARGE(R.layout.widget_bloom_large),
}

/** Gemeinsame Aktualisierung; die drei Größen sind eigene Widgets in der Auswahl und unterscheiden sich nur im Layout. */
abstract class BaseBloomWidget : AppWidgetProvider() {
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
}

class BloomWidgetSmall : BaseBloomWidget()

/** Das mittlere Widget behält den ursprünglichen Namen, damit bereits platzierte Widgets erhalten bleiben. */
class BloomWidget : BaseBloomWidget() {
    companion object {
        /** Zeichnet alle Widgets neu, etwa nach einer Änderung der Einträge. */
        fun refresh(context: Context) {
            val app = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch { updateWidgets(app) }
        }
    }
}

class BloomWidgetLarge : BaseBloomWidget()

private suspend fun updateWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val targets = listOf(
        BloomWidgetSmall::class.java to Size.COMPACT,
        BloomWidget::class.java to Size.MEDIUM,
        BloomWidgetLarge::class.java to Size.LARGE,
    ).map { (provider, size) -> manager.getAppWidgetIds(ComponentName(context, provider)) to size }
        .filter { (ids, _) -> ids.isNotEmpty() }
    if (targets.isEmpty()) return

    val today = LocalDate.now()
    val days = BloomDatabase.get(context).dao().allDays()
    val periodDays = days.filter { Bleeding.fromLevel(it.bleeding).isPeriodDay }.map { LocalDate.ofEpochDay(it.epochDay) }
    val neutral = SettingsStore(context).settings.first().neutralText
    val content = widgetContent(CycleModel(detectPeriods(periodDays), today), today, neutral)
    val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val colors = seasonColorsFor(dark)
    targets.forEach { (ids, size) -> manager.updateAppWidget(ids, render(context, size, content, colors)) }
}

private fun render(context: Context, size: Size, content: WidgetContent, colors: SeasonColors): RemoteViews {
    val palette = colors.of(content.season ?: Season.SPRING)
    val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    return RemoteViews(context.packageName, size.layout).apply {
        setColorStateList(R.id.widget_root, "setBackgroundTintList", ColorStateList.valueOf(palette.container.toArgb()))
        val (season, big, sub) = when (size) {
            Size.COMPACT -> Triple(content.name, content.number, content.unit)
            else -> Triple(content.title, content.big, content.sub)
        }
        setText(R.id.widget_season, season, palette)
        setText(R.id.widget_big, big, palette)
        setText(R.id.widget_sub, sub, palette)
        if (size == Size.LARGE) {
            setText(R.id.widget_window, content.window, palette)
            val year = content.year
            setViewVisibility(R.id.widget_bar, if (year == null) View.GONE else View.VISIBLE)
            if (year != null) setImageViewBitmap(R.id.widget_bar, yearBar(year, colors, palette))
        }
        setOnClickPendingIntent(R.id.widget_root, open)
    }
}

private fun RemoteViews.setText(id: Int, text: String, palette: SeasonPalette) {
    setTextViewText(id, text)
    setTextColor(id, palette.onContainer.toArgb())
    setViewVisibility(id, if (text.isEmpty()) View.GONE else View.VISIBLE)
}

/** Der Zyklus als Band in den vier Jahreszeitenfarben, ein Punkt markiert den heutigen Tag. */
private fun yearBar(year: YearRun, colors: SeasonColors, palette: SeasonPalette): Bitmap {
    val width = 720
    val height = 40
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val bar = RectF(0f, 12f, width.toFloat(), 28f)
    canvas.save()
    canvas.clipPath(Path().apply { addRoundRect(bar, 8f, 8f, Path.Direction.CW) })
    year.spans.forEach { span ->
        paint.color = colors.of(span.season).accent.toArgb()
        val left = (span.firstDay - 1).toFloat() / year.length * width
        val right = span.lastDay.toFloat() / year.length * width
        canvas.drawRect(left, bar.top, right, bar.bottom, paint)
    }
    canvas.restore()
    val x = ((year.day - 0.5f) / year.length * width).coerceIn(20f, width - 20f)
    paint.color = palette.container.toArgb()
    canvas.drawCircle(x, height / 2f, 19f, paint)
    paint.color = palette.onContainer.toArgb()
    canvas.drawCircle(x, height / 2f, 13f, paint)
    return bitmap
}
