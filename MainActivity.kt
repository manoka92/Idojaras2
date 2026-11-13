package com.example.weatherwidget

import android.Manifest
import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.concurrent.thread

// Ez az app csak az engedélyek kérésére és a hely mentésére kell, utána bezárul.
class MainActivity : ComponentActivity() {

    private val bgLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { updateAndFinish() }

    private val fgLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        val granted = res.values.any { it }
        if (granted && Build.VERSION.SDK_INT >= 29 &&
            checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "A háttérfrissítéshez válaszd: „Mindig engedélyezett”", Toast.LENGTH_LONG).show()
            bgLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else updateAndFinish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fgLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
    }

    @SuppressLint("MissingPermission")
    private fun updateAndFinish() {
        if (Loc.lastKnown(this) == null &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // nincs tárolt hely: kérünk egy frisset
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val provider = lm.getProviders(true).firstOrNull()
            if (provider != null) {
                @Suppress("DEPRECATION")
                lm.requestSingleUpdate(provider, object : LocationListener {
                    override fun onLocationChanged(l: Location) { refreshWidgets() }
                    override fun onProviderEnabled(p: String) {}
                    override fun onProviderDisabled(p: String) {}
                }, mainLooper)
                return
            }
        }
        refreshWidgets()
    }

    private fun refreshWidgets() {
        val mgr = AppWidgetManager.getInstance(this)
        val ids = mgr.getAppWidgetIds(ComponentName(this, WeatherWidget::class.java))
        sendBroadcast(Intent(this, WeatherWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        })
        finish()
    }
}
