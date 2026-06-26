package masha.pogoda.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import masha.pogoda.data.location.LocationProvider
import masha.pogoda.data.prefs.AppPrefs
import masha.pogoda.data.prefs.AppPrefs.LocationMode
import masha.pogoda.data.repository.WeatherRepository
import masha.pogoda.data.repository.WeatherResult

class MainViewModel(
    private val repository: WeatherRepository,
    private val locationProvider: LocationProvider,
    private val prefs: AppPrefs
) : ViewModel() {
    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            // Cache-first: сразу показываем сохранённый прогноз (если есть),
            // иначе спиннер. Затем тихо обновляем из сети.
            val cached = repository.cached()
            _uiState.value = cached?.toUiState() ?: MainUiState.Loading

            val (lat, lon) = resolveLocation()
            val result = repository.refresh(lat, lon, prefs.city)
            _uiState.value = result.toUiState()
        }
    }

    private suspend fun resolveLocation(): Pair<Double, Double> {
        if (prefs.locationMode == LocationMode.GPS) {
            locationProvider.current()?.let { current ->
                prefs.lat = current.first
                prefs.lon = current.second
                return current
            }
        }
        return prefs.lat to prefs.lon
    }

    private fun WeatherResult.toUiState(): MainUiState = when (this) {
        is WeatherResult.Success -> MainUiState.Content(
            forecast = forecast,
            // Баннер только для устаревших данных; свежий кэш и сеть показываем без него.
            cacheBannerTimeMillis = forecast.cachedAt.takeIf { stale },
            // isFresh = пришло из сети (не из кэша) → можно обновить виджет.
            isFresh = !fromCache
        )

        is WeatherResult.Error -> cached?.let { forecast ->
            MainUiState.Content(
                forecast = forecast,
                cacheBannerTimeMillis = forecast.cachedAt,
                isFresh = false
            )
        } ?: MainUiState.Empty()
    }
}
