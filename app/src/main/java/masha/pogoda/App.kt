package masha.pogoda

import android.app.Application
import masha.pogoda.di.ServiceLocator
import masha.pogoda.work.WeatherSyncScheduler

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        WeatherSyncScheduler.schedule(this)
    }
}
