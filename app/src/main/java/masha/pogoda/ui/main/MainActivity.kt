package masha.pogoda.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationServices
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import masha.pogoda.data.cache.WeatherCacheManager
import masha.pogoda.data.location.LocationProvider
import masha.pogoda.data.mapper.weatherToAdvice
import masha.pogoda.data.prefs.AppPrefs
import masha.pogoda.data.repository.WeatherRepository
import masha.pogoda.databinding.ActivityMainBinding
import masha.pogoda.di.ServiceLocator
import masha.pogoda.domain.model.WeatherForecast
import masha.pogoda.ui.icon.WeatherIconLoader

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var hourlyAdapter: HourlyAdapter
    private lateinit var dailyAdapter: DailyAdapter
    private lateinit var iconLoader: WeatherIconLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        iconLoader = WeatherIconLoader(this)
        hourlyAdapter = HourlyAdapter(iconLoader)
        dailyAdapter = DailyAdapter(iconLoader)
        setupLists()

        viewModel = ViewModelProvider(this, viewModelFactory())[MainViewModel::class.java]

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.retryButton.setOnClickListener { viewModel.refresh() }

        requestLocationPermissionIfNeeded()
        observeState()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST_CODE) {
            viewModel.refresh()
        }
    }

    private fun setupLists() {
        binding.hourlyList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.hourlyList.adapter = hourlyAdapter
        binding.dailyList.layoutManager = LinearLayoutManager(this)
        binding.dailyList.adapter = dailyAdapter
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.swipeRefresh.isRefreshing = state is MainUiState.Loading
                    when (state) {
                        MainUiState.Loading -> Unit
                        is MainUiState.Content -> showContent(state)
                        is MainUiState.Empty -> showEmpty(state)
                    }
                }
            }
        }
    }

    private fun showContent(state: MainUiState.Content) {
        binding.mainContent.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE
        binding.bannerText.visibility = if (state.banner == null) View.GONE else View.VISIBLE
        binding.bannerText.text = state.banner.orEmpty()
        bindForecast(state.forecast)
    }

    private fun bindForecast(forecast: WeatherForecast) {
        val current = forecast.current
        binding.cityText.text = forecast.city
        binding.updatedText.text = "Обновлено ${forecast.cachedAt.formatTime()}"
        binding.currentTempText.text = "${current.temperature}°"
        binding.feelsLikeText.text = "Ощущается как ${current.feelsLike}°"
        binding.descriptionText.text = current.description
        binding.detailsText.text =
            "Влажность ${current.humidity}%   Ветер ${current.windSpeed} м/с ${current.windDirection}   Давление ${current.pressure}"
        binding.adviceText.text = weatherToAdvice(
            tempC = current.temperature,
            code = current.code,
            precipProb = forecast.daily.firstOrNull()?.precipProb ?: 0,
            windMs = current.windSpeed
        )
        binding.currentIcon.contentDescription = current.description
        iconLoader.load(binding.currentIcon, current.iconCode)
        hourlyAdapter.submitList(forecast.hourly)
        dailyAdapter.submitList(forecast.daily)
    }

    private fun showEmpty(state: MainUiState.Empty) {
        binding.mainContent.visibility = View.GONE
        binding.emptyState.visibility = View.VISIBLE
        binding.emptyText.text = state.message
        binding.retryButton.visibility = if (state.canRetry) View.VISIBLE else View.GONE
        Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
    }

    private fun viewModelFactory(): ViewModelProvider.Factory {
        val prefs = AppPrefs(this)
        ServiceLocator.yandexKeyProvider = { prefs.yandexKey }
        val repository = WeatherRepository(
            openMeteoApi = ServiceLocator.openMeteoApi,
            yandexApi = ServiceLocator.yandexApi,
            cache = WeatherCacheManager(File(filesDir, "weather_cache")),
            yandexKeyProvider = { prefs.yandexKey }
        )
        val locationProvider = LocationProvider(
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this),
            nominatimApi = ServiceLocator.nominatimApi,
            prefs = prefs
        )

        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(repository, locationProvider, prefs) as T
            }
        }
    }

    private fun requestLocationPermissionIfNeeded() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_REQUEST_CODE
            )
        }
    }

    private fun Long.formatTime(): String =
        SimpleDateFormat("HH:mm", Locale("ru")).format(Date(this))

    private companion object {
        const val LOCATION_REQUEST_CODE = 100
    }
}

