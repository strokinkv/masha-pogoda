package masha.pogoda.ui.settings

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import masha.pogoda.BuildConfig
import masha.pogoda.data.location.LocationProvider
import masha.pogoda.data.prefs.AppPrefs
import masha.pogoda.data.prefs.AppPrefs.LocationMode
import masha.pogoda.databinding.ActivitySettingsBinding
import masha.pogoda.di.ServiceLocator

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: AppPrefs
    private lateinit var locationProvider: LocationProvider
    private var unlocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = AppPrefs(this)
        locationProvider = LocationProvider(
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this),
            nominatimApi = ServiceLocator.nominatimApi,
            prefs = prefs
        )

        bindInitialValues()
        bindControls()
        showParentLock()
    }

    private fun bindInitialValues() {
        binding.yandexKeyInput.setText(prefs.userYandexKey.orEmpty())
        binding.cityInput.setText(prefs.city)
        if (prefs.locationMode == LocationMode.GPS) {
            binding.gpsModeButton.isChecked = true
        } else {
            binding.manualModeButton.isChecked = true
        }
        updateManualControls()

        binding.aboutText.text = getString(
            masha.pogoda.R.string.settings_about,
            BuildConfig.VERSION_NAME
        )
    }

    private fun bindControls() {
        binding.modeGroup.setOnCheckedChangeListener { _, _ -> updateManualControls() }
        binding.searchCityButton.setOnClickListener { geocodeCity() }
        binding.saveButton.setOnClickListener { saveSettings() }
        binding.cancelButton.setOnClickListener { finish() }
    }

    private fun showParentLock() {
        val answerInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            imeOptions = EditorInfo.IME_ACTION_DONE
        }

        AlertDialog.Builder(this)
            .setTitle(getString(masha.pogoda.R.string.parent_lock_title))
            .setMessage(getString(masha.pogoda.R.string.parent_lock_question))
            .setView(answerInput)
            .setCancelable(false)
            .setPositiveButton(masha.pogoda.R.string.parent_lock_ok) { _, _ ->
                if (answerInput.text.toString().trim() == "12") {
                    unlocked = true
                } else {
                    Toast.makeText(this, masha.pogoda.R.string.parent_lock_wrong, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> finish() }
            .show()
    }

    private fun updateManualControls() {
        val manual = selectedLocationMode() == LocationMode.MANUAL
        binding.cityInput.isEnabled = manual
        binding.searchCityButton.isEnabled = manual
        binding.cityHint.alpha = if (manual) 1f else 0.5f
    }

    private fun selectedLocationMode(): LocationMode =
        if (binding.manualModeButton.isChecked) LocationMode.MANUAL else LocationMode.GPS

    private fun saveSettings() {
        if (!unlocked) return

        val current = currentSnapshot()
        val result = SettingsForm.normalize(
            current = current,
            cityInput = binding.cityInput.text?.toString().orEmpty(),
            locationMode = selectedLocationMode(),
            yandexKeyInput = binding.yandexKeyInput.text?.toString().orEmpty()
        )

        if (!result.changed) {
            finish()
            return
        }

        lifecycleScope.launch {
            val next = result.snapshot
            if (next.locationMode == LocationMode.MANUAL && next.city != current.city) {
                binding.progress.visibility = View.VISIBLE
                binding.saveButton.isEnabled = false
                val point = locationProvider.geocode(next.city)
                binding.progress.visibility = View.GONE
                binding.saveButton.isEnabled = true
                if (point == null) {
                    Toast.makeText(
                        this@SettingsActivity,
                        masha.pogoda.R.string.city_not_found,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
            }

            prefs.city = next.city
            prefs.locationMode = next.locationMode
            prefs.userYandexKey = next.userYandexKey
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun geocodeCity() {
        val city = binding.cityInput.text?.toString().orEmpty().trim()
        if (city.isBlank()) return

        lifecycleScope.launch {
            binding.progress.visibility = View.VISIBLE
            binding.searchCityButton.isEnabled = false
            val point = locationProvider.geocode(city)
            binding.progress.visibility = View.GONE
            binding.searchCityButton.isEnabled = true
            val message = if (point == null) {
                masha.pogoda.R.string.city_not_found
            } else {
                prefs.locationMode = LocationMode.MANUAL
                binding.manualModeButton.isChecked = true
                masha.pogoda.R.string.city_found
            }
            Toast.makeText(this@SettingsActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun currentSnapshot(): SettingsSnapshot =
        SettingsSnapshot(
            city = prefs.city,
            locationMode = prefs.locationMode,
            userYandexKey = prefs.userYandexKey
        )
}
