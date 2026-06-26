package masha.pogoda

import android.app.Application
import masha.pogoda.work.WeatherSyncScheduler

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        WeatherSyncScheduler.schedule(this)
    }
}
