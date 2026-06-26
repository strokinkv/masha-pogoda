package masha.pogoda.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File
import masha.pogoda.data.cache.WeatherCacheManager
import masha.pogoda.data.prefs.AppPrefs
import masha.pogoda.data.repository.WeatherRepository
import masha.pogoda.data.repository.WeatherResult
import masha.pogoda.di.ServiceLocator

class WeatherSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val prefs = AppPrefs(applicationContext)
        ServiceLocator.yandexKeyProvider = { prefs.yandexKey }

        val repository = WeatherRepository(
            openMeteoApi = ServiceLocator.openMeteoApi,
            yandexApi = ServiceLocator.yandexApi,
            cache = WeatherCacheManager(File(applicationContext.filesDir, "weather_cache")),
            yandexKeyProvider = { prefs.yandexKey }
        )

        return when (repository.refresh(prefs.lat, prefs.lon, prefs.city)) {
            is WeatherResult.Success -> Result.success()
            is WeatherResult.Error -> Result.retry()
        }
    }
}
