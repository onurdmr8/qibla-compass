package com.qibla.compass

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.qibla.compass.model.City
import com.qibla.compass.model.PredefinedCities
import com.qibla.compass.ui.compass.CompassScreen
import com.qibla.compass.util.CompassSensorManager
import com.qibla.compass.util.InterstitialAdManager
import com.qibla.compass.util.QiblaCalculator

class MainActivity : ComponentActivity() {

    private lateinit var compassSensorManager: CompassSensorManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var interstitialAdManager: InterstitialAdManager

    private var heading by mutableFloatStateOf(0f)
    private var isCalibrated by mutableStateOf(true)
    private var currentCity by mutableStateOf(PredefinedCities.list.first()) // Varsayılan İstanbul

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            fetchDeviceLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Google AdMob SDK'sını başlat
        MobileAds.initialize(this) {}
        interstitialAdManager = InterstitialAdManager(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        compassSensorManager = CompassSensorManager(
            context = this,
            onHeadingChanged = { newHeading ->
                heading = newHeading
                val qiblaBearing = QiblaCalculator.calculateQiblaBearing(
                    currentCity.latitude,
                    currentCity.longitude
                )
                compassSensorManager.checkAlignmentVibration(newHeading, qiblaBearing)
            },
            onAccuracyChanged = { calibrated ->
                isCalibrated = calibrated
            }
        )

        checkAndRequestLocationPermission()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CompassScreen(
                        heading = heading,
                        isCalibrated = isCalibrated,
                        currentCity = currentCity,
                        onCitySelected = { selected ->
                            currentCity = selected
                            interstitialAdManager.showIfReady(this@MainActivity)
                        },
                        onRequestGps = {
                            checkAndRequestLocationPermission()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        compassSensorManager.start()
    }

    override fun onPause() {
        super.onPause()
        compassSensorManager.stop()
    }

    private fun checkAndRequestLocationPermission() {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineLocation == PackageManager.PERMISSION_GRANTED || coarseLocation == PackageManager.PERMISSION_GRANTED) {
            fetchDeviceLocation()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchDeviceLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentCity = City(
                        name = "Mevcut Konum",
                        country = "GPS",
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
}
