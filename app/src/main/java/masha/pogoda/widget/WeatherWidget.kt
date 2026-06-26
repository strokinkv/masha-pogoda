package masha.pogoda.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.view.View
import android.widget.RemoteViews
import java.io.File
import masha.pogoda.R
import masha.pogoda.data.cache.WeatherCacheManager
import masha.pogoda.data.prefs.AppPrefs
import masha.pogoda.ui.icon.WeatherIconLoader
import masha.pogoda.ui.main.MainActivity

class WeatherWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            appWidgetManager.updateAppWidget(widgetId, buildViews(context))
        }
    }

    companion object {
        private val entryLabelIds = intArrayOf(
            R.id.widgetDay1Date,
            R.id.widgetDay2Date,
            R.id.widgetDay3Date,
            R.id.widgetDay4Date,
            R.id.widgetDay5Date
        )
        private val entryIconIds = intArrayOf(
            R.id.widgetDay1Icon,
            R.id.widgetDay2Icon,
            R.id.widgetDay3Icon,
            R.id.widgetDay4Icon,
            R.id.widgetDay5Icon
        )
        private val entryTempIds = intArrayOf(
            R.id.widgetDay1Temp,
            R.id.widgetDay2Temp,
            R.id.widgetDay3Temp,
            R.id.widgetDay4Temp,
            R.id.widgetDay5Temp
        )

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WeatherWidget::class.java))
            ids.forEach { widgetId ->
                manager.updateAppWidget(widgetId, buildViews(context))
            }
        }

        private fun buildViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_weather_4x2)
            views.setOnClickPendingIntent(R.id.widgetRoot, launchIntent(context))
            applyTheme(context, views)

            val forecast = WeatherCacheManager(File(context.filesDir, "weather_cache")).load()
            if (forecast == null) {
                views.setTextViewText(R.id.widgetCity, context.getString(R.string.widget_empty_title))
                views.setTextViewText(R.id.widgetTemp, "--°")
                views.setTextViewText(R.id.widgetDetails, context.getString(R.string.widget_empty_details))
                views.setTextViewText(R.id.widgetUpdated, "")
                return views
            }

            val iconLoader = WeatherIconLoader(context)
            val model = WeatherWidgetModel.from(forecast, AppPrefs(context).widgetHourlyForecast)
            views.setTextViewText(R.id.widgetCity, model.city)
            views.setTextViewText(R.id.widgetTemp, model.temperature)
            views.setTextViewText(
                R.id.widgetDetails,
                context.getString(R.string.widget_details, model.feelsLike, model.humidity, model.windSpeed)
            )
            views.setTextViewText(R.id.widgetUpdated, context.getString(R.string.widget_updated, model.updatedAt))
            views.setImageViewBitmap(R.id.widgetCurrentIcon, iconLoader.renderBitmap(model.iconCode, 112))

            entryLabelIds.indices.forEach { index ->
                val entry = model.entries.getOrNull(index)
                val visibility = if (entry == null) View.INVISIBLE else View.VISIBLE
                views.setViewVisibility(entryLabelIds[index], visibility)
                views.setViewVisibility(entryIconIds[index], visibility)
                views.setViewVisibility(entryTempIds[index], visibility)
                if (entry != null) {
                    views.setTextViewText(entryLabelIds[index], entry.label)
                    views.setTextViewText(entryTempIds[index], entry.temperature)
                    views.setImageViewBitmap(entryIconIds[index], iconLoader.renderBitmap(entry.iconCode, 56))
                }
            }

            return views
        }

        private fun applyTheme(context: Context, views: RemoteViews) {
            val night = context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            val background = if (night) R.drawable.bg_widget_dark else R.drawable.bg_widget_light
            val textColor = if (night) 0xFFFFFFFF.toInt() else 0xFF2B2B2B.toInt()
            val mutedColor = if (night) 0xFFE3E8F0.toInt() else 0xFF5B6470.toInt()

            views.setInt(R.id.widgetRoot, "setBackgroundResource", background)
            listOf(
                R.id.widgetCity,
                R.id.widgetTemp,
                R.id.widgetDay1Date,
                R.id.widgetDay1Temp,
                R.id.widgetDay2Date,
                R.id.widgetDay2Temp,
                R.id.widgetDay3Date,
                R.id.widgetDay3Temp,
                R.id.widgetDay4Date,
                R.id.widgetDay4Temp,
                R.id.widgetDay5Date,
                R.id.widgetDay5Temp
            ).forEach { views.setTextColor(it, textColor) }
            views.setTextColor(R.id.widgetDetails, mutedColor)
            views.setTextColor(R.id.widgetUpdated, mutedColor)
        }

        private fun launchIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
