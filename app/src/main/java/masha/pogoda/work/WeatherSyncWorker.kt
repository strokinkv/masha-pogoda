package masha.pogoda.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import masha.pogoda.data.prefs.AppPrefs
import masha.pogoda.data.repository.WeatherResult
import masha.pogoda.di.ServiceLocator
import masha.pogoda.widget.WeatherWidget

class WeatherSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val prefs = AppPrefs(applicationContext)
        val repository = ServiceLocator.weatherRepository(applicationContext)

        return when (repository.refresh(prefs.lat, prefs.lon, prefs.city)) {
            is WeatherResult.Success -> {
                WeatherWidget.updateAll(applicationContext)
                Result.success()
            }
            is WeatherResult.Error -> Result.retry()
        }
    }
}
