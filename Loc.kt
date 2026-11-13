package com.example.weatherwidget

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import java.util.Locale

data class Place(val lat: Double, val lon: Double, val name: String)

object Loc {
    // ha nincs engedély / helyadat, ez az alapértelmezett hely
    private val DEFAULT = Place(47.4979, 19.0402, "Budapest")

    fun resolve(ctx: Context): Place {
        val fresh = lastKnown(ctx)
        if (fresh != null) {
            val place = Place(fresh.latitude, fresh.longitude, cityName(ctx, fresh))
            save(ctx, place)
            return place
        }
        return load(ctx) ?: DEFAULT
    }

    @SuppressLint("MissingPermission")
    fun lastKnown(ctx: Context): Location? {
        val ok = ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!ok) return null
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.getProviders(true).mapNotNull {
            try { lm.getLastKnownLocation(it) } catch (e: Exception) { null }
        }.maxByOrNull { it.time }
    }

    fun cityName(ctx: Context, l: Location): String = try {
        @Suppress("DEPRECATION")
        val a = Geocoder(ctx, Locale("hu")).getFromLocation(l.latitude, l.longitude, 1)?.firstOrNull()
        a?.locality ?: a?.subAdminArea ?: "Itt vagy"
    } catch (e: Exception) { "Itt vagy" }

    fun save(ctx: Context, p: Place) {
        ctx.getSharedPreferences("loc", Context.MODE_PRIVATE).edit()
            .putString("lat", p.lat.toString()).putString("lon", p.lon.toString())
            .putString("name", p.name).apply()
    }

    private fun load(ctx: Context): Place? {
        val sp = ctx.getSharedPreferences("loc", Context.MODE_PRIVATE)
        val lat = sp.getString("lat", null)?.toDoubleOrNull() ?: return null
        val lon = sp.getString("lon", null)?.toDoubleOrNull() ?: return null
        return Place(lat, lon, sp.getString("name", "Itt vagy") ?: "Itt vagy")
    }
}
