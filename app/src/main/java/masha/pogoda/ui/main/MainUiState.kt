package masha.pogoda.ui.main

import masha.pogoda.domain.model.WeatherForecast

sealed class MainUiState {
    data object Loading : MainUiState()

    data class Content(
        val forecast: WeatherForecast,
        val banner: String?
    ) : MainUiState()

    data class Empty(
        val message: String,
        val canRetry: Boolean = true
    ) : MainUiState()
}

