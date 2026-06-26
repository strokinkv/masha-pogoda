package masha.pogoda.ui.main

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import masha.pogoda.R
import masha.pogoda.data.mapper.weatherToAdvice
import masha.pogoda.data.prefs.AppPrefs
import masha.pogoda.databinding.ActivityMainBinding
import masha.pogoda.di.ServiceLocator
import masha.pogoda.domain.model.WeatherForecast
import masha.pogoda.domain.model.HourlyForecastWindow
import masha.pogoda.ui.icon.WeatherIconLoader
import masha.pogoda.ui.settings.SettingsActivity
import masha.pogoda.widget.WeatherWidget

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var hourlyAdapter: HourlyAdapter
    private lateinit var dailyAdapter: DailyAdapter
    private lateinit var iconLoader: WeatherIconLoader
    private lateinit var settingsLauncher: ActivityResultLauncher<Intent>

    // cachedAt прогноза, который уже отправлен в виджет — чтобы не перерисовывать
    // его на каждом возврате в приложение (StateFlow повторно отдаёт текущее значение).
    private var lastWidgetCachedAt: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.refresh()
            }
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyTimeOfDayBackground()

        iconLoader = WeatherIconLoader(this)
        hourlyAdapter = HourlyAdapter(iconLoader)
        dailyAdapter = DailyAdapter(iconLoader)
        setupLists()

        viewModel = ViewModelProvider(this, viewModelFactory())[MainViewModel::class.java]

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.retryButton.setOnClickListener { viewModel.refresh() }
        binding.settingsButton.setOnClickListener {
            settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
        }

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
            val granted = grantResults.any { it == PackageManager.PERMISSION_GRANTED }
            // Перезапрашиваем только если разрешение выдали и используется GPS:
            // при отказе или ручном режиме начальный refresh уже показал данные.
            if (granted && AppPrefs(this).locationMode == AppPrefs.LocationMode.GPS) {
                viewModel.refresh()
            }
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
        binding.bannerText.visibility = if (state.cacheBannerTimeMillis == null) View.GONE else View.VISIBLE
        binding.bannerText.text = state.cacheBannerTimeMillis?.let {
            getString(R.string.cache_banner, it.formatTime())
        }.orEmpty()
        bindForecast(state.forecast)
        animateContent()
        maybeUpdateWidget(state)
    }

    private fun maybeUpdateWidget(state: MainUiState.Content) {
        // Только для свежих сетевых данных и только если это новый прогноз
        // (а не повторная отдача того же значения StateFlow при возврате/повороте).
        if (!state.isFresh || state.forecast.cachedAt == lastWidgetCachedAt) return
        lastWidgetCachedAt = state.forecast.cachedAt
        // Чтение кэша + рендер SVG в bitmap — вне UI-потока.
        lifecycleScope.launch(Dispatchers.Default) {
            WeatherWidget.updateAll(applicationContext)
        }
    }

    private fun bindForecast(forecast: WeatherForecast) {
        val current = forecast.current
        binding.cityText.text = forecast.city
        binding.updatedText.text = getString(R.string.updated_at, forecast.cachedAt.formatTime())
        binding.currentTempText.text = "${current.temperature}°"
        binding.feelsLikeText.text = getString(R.string.feels_like, current.feelsLike)
        binding.descriptionText.text = current.description
        binding.detailsText.text = getString(
            R.string.current_details,
            current.humidity,
            current.windSpeed,
            current.windDirection,
            current.pressure
        )
        val now = HourlyForecastWindow.nowInZone(forecast.timezone)
        val nearTermPrecip = HourlyForecastWindow.nextHours(forecast.hourly, now, limit = 1)
            .firstOrNull()?.precipProb
            ?: forecast.daily.firstOrNull()?.precipProb
            ?: 0
        binding.adviceText.text = weatherToAdvice(
            tempC = current.temperature,
            code = current.code,
            precipProb = nearTermPrecip,
            windMs = current.windSpeed
        )
        binding.currentIcon.contentDescription = current.description
        iconLoader.load(binding.currentIcon, current.iconCode)
        animateCurrentIcon()
        hourlyAdapter.submitList(HourlyForecastWindow.nextHours(forecast.hourly, now, limit = 24))
        dailyAdapter.submitList(forecast.daily)
    }

    private fun showEmpty(state: MainUiState.Empty) {
        binding.mainContent.visibility = View.GONE
        binding.emptyState.visibility = View.VISIBLE
        val message = getString(R.string.error_weather_unavailable)
        binding.emptyText.text = message
        binding.retryButton.visibility = if (state.canRetry) View.VISIBLE else View.GONE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun viewModelFactory(): ViewModelProvider.Factory {
        val prefs = AppPrefs(this)
        val repository = ServiceLocator.weatherRepository(this)
        val locationProvider = ServiceLocator.locationProvider(this)

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
        TIME_FORMATTER.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))

    private fun applyTimeOfDayBackground() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val background = when (hour) {
            in 5..10 -> R.drawable.bg_gradient_morning
            in 11..16 -> R.drawable.bg_gradient_day
            in 17..21 -> R.drawable.bg_gradient_evening
            else -> R.drawable.bg_gradient_night
        }
        binding.rootContainer.setBackgroundResource(background)
    }

    private fun animateContent() {
        binding.mainContent.alpha = 0f
        binding.mainContent.animate()
            .alpha(1f)
            .setDuration(200L)
            .start()
    }

    private fun animateCurrentIcon() {
        binding.currentIcon.animate()
            .scaleX(1.04f)
            .scaleY(1.04f)
            .setDuration(450L)
            .withEndAction {
                binding.currentIcon.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(450L)
                    .start()
            }
            .start()
    }

    private companion object {
        const val LOCATION_REQUEST_CODE = 100
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale("ru"))
    }
}
