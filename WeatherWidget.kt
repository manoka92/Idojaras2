package com.example.weatherwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.json.JSONObject
import java.net.URL
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.concurrent.thread

class WeatherWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        thread {
            val place = Loc.resolve(context)
            val json = try {
                val url = "https://api.open-meteo.com/v1/forecast?latitude=${place.lat}&longitude=${place.lon}" +
                    "&current=temperature_2m,weather_code" +
                    "&hourly=temperature_2m,weather_code&forecast_hours=6" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
                    "&forecast_days=4&timezone=auto"
                JSONObject(URL(url).readText())
            } catch (e: Exception) { null }
            ids.forEach { mgr.updateAppWidget(it, build(context, json, ids, place.name)) }
            pending.finish()
        }
    }

    private fun build(ctx: Context, j: JSONObject?, ids: IntArray, city: String): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget)
        v.setTextViewText(R.id.city, city)

        // koppintásra frissít
        val i = Intent(ctx, WeatherWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        v.setOnClickPendingIntent(R.id.root,
            PendingIntent.getBroadcast(ctx, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

        if (j == null) {
            v.setTextViewText(R.id.temp, "–")
            setFrames(ctx, v, 3)
            return v
        }

        val cur = j.getJSONObject("current")
        v.setTextViewText(R.id.temp, "${Math.round(cur.getDouble("temperature_2m"))}°")
        setFrames(ctx, v, cur.getInt("weather_code"))

        val h = j.getJSONObject("hourly")
        val hT = h.getJSONArray("time"); val hTemp = h.getJSONArray("temperature_2m"); val hC = h.getJSONArray("weather_code")
        val hIds = intArrayOf(R.id.h0, R.id.h1, R.id.h2, R.id.h3, R.id.h4)
        for (n in hIds.indices) {
            val k = n + 1
            if (k >= hT.length()) break
            val hour = hT.getString(k).substring(11, 13)
            v.setTextViewText(hIds[n], "$hour:00\n${emoji(hC.getInt(k))}\n${Math.round(hTemp.getDouble(k))}°")
        }

        val d = j.getJSONObject("daily")
        val dT = d.getJSONArray("time"); val dC = d.getJSONArray("weather_code")
        val dMax = d.getJSONArray("temperature_2m_max"); val dMin = d.getJSONArray("temperature_2m_min")
        val dIds = intArrayOf(R.id.d0, R.id.d1, R.id.d2)
        for (n in dIds.indices) {
            val k = n + 1
            if (k >= dT.length()) break
            val day = LocalDate.parse(dT.getString(k)).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("hu"))
            v.setTextViewText(dIds[n], "$day\n${emoji(dC.getInt(k))}\n${Math.round(dMax.getDouble(k))}° / ${Math.round(dMin.getDouble(k))}°")
        }
        return v
    }

    // képkockás animáció: a ViewFlipper automatikusan lapozgatja a kockákat
    private fun setFrames(ctx: Context, v: RemoteViews, code: Int) {
        val prefix = when (code) {
            0, 1 -> "sun"
            in 2..3, 45, 48 -> "cloud"
            in 71..77, 85, 86 -> "snow"
            else -> "rain"
        }
        v.removeAllViews(R.id.flipper)
        for (f in 0..2) {
            val res = ctx.resources.getIdentifier("${prefix}_$f", "drawable", ctx.packageName)
            val frame = RemoteViews(ctx.packageName, R.layout.frame)
            frame.setImageViewResource(R.id.img, res)
            v.addView(R.id.flipper, frame)
        }
    }

    private fun emoji(code: Int) = when (code) {
        0, 1 -> "☀️"
        2 -> "⛅"
        3, 45, 48 -> "☁️"
        in 71..77, 85, 86 -> "❄️"
        95, 96, 99 -> "⛈️"
        else -> "🌧️"
    }
}
