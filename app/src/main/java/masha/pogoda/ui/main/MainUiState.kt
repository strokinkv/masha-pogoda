package masha.pogoda.ui.main

import masha.pogoda.domain.model.WeatherForecast

sealed class MainUiState {
    data object Loading : MainUiState()

    data class Content(
        val forecast: WeatherForecast,
        val cacheBannerTimeMillis: Long?,
        val isFresh: Boolean = false
    ) : MainUiState()

    data class Empty(
        val canRetry: Boolean = true
    ) : MainUiState()
}
