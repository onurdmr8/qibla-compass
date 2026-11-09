package com.qibla.compass

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
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
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.qibla.compass.model.City
import com.qibla.compass.model.PredefinedCities
import com.qibla.compass.ui.compass.CompassScreen
import com.qibla.compass.util.CompassSensorManager
import com.qibla.compass.util.InterstitialAdManager
import com.qibla.compass.util.QiblaCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var compassSensorManager: CompassSensorManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var interstitialAdManager: InterstitialAdManager

    private var heading by mutableFloatStateOf(0f)
    private var isCalibrated by mutableStateOf(true)
    private var currentCity by mutableStateOf(PredefinedCities.list.first { it.name == "İstanbul" })

    private var showCitySheet by mutableStateOf(false)
    private var isPermissionDeniedReason by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineLocationGranted || coarseLocationGranted) {
            isPermissionDeniedReason = false
            fetchDeviceLocation()
        } else {
            // İzin verilmediğinde kullanıcıya şehir seçtir
            isPermissionDeniedReason = true
            showCitySheet = true
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

        // Uygulama açılışında konum iznini kontrol et veya iste
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
                        showCitySheet = showCitySheet,
                        isPermissionDeniedReason = isPermissionDeniedReason,
                        onShowCitySheetChange = { show ->
                            showCitySheet = show
                            if (!show) {
                                isPermissionDeniedReason = false
                            }
                        },
                        onCitySelected = { selected ->
                            currentCity = selected
                            showCitySheet = false
                            isPermissionDeniedReason = false
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
            isPermissionDeniedReason = false
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
                    resolveCityFromCoordinates(location.latitude, location.longitude)
                } else {
                    // lastLocation null ise tek seferlik güncel konumu almayı dene
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        null
                    ).addOnSuccessListener { currentLocation ->
                        if (currentLocation != null) {
                            resolveCityFromCoordinates(currentLocation.latitude, currentLocation.longitude)
                        } else {
                            // Konum alınamadıysa şehir seçtir
                            isPermissionDeniedReason = false
                            showCitySheet = true
                        }
                    }.addOnFailureListener {
                        isPermissionDeniedReason = false
                        showCitySheet = true
                    }
                }
            }.addOnFailureListener {
                isPermissionDeniedReason = false
                showCitySheet = true
            }
        } catch (e: Exception) {
            // Hata durumunda şehir seçtir
            isPermissionDeniedReason = false
            showCitySheet = true
        }
    }

    /**
     * GPS koordinatlarından Geocoder ile şehir ve ülke adını otomatik bulur.
     * Çevrimdışı veya Geocoder yanıt vermediğinde en yakın şehri eşleştirir.
     */
    private fun resolveCityFromCoordinates(lat: Double, lon: Double) {
        lifecycleScope.launch(Dispatchers.IO) {
            var resolvedCityName: String? = null
            var resolvedCountry: String? = null

            try {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(lat, lon, 1) { addresses ->
                            val address = addresses.firstOrNull()
                            if (address != null) {
                                val (cName, cCountry) = extractCityAndCountry(address)
                                updateCurrentCity(lat, lon, cName, cCountry)
                            } else {
                                updateCurrentCity(lat, lon, null, null)
                            }
                        }
                        return@launch
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(lat, lon, 1)
                        val address = addresses?.firstOrNull()
                        if (address != null) {
                            val (cName, cCountry) = extractCityAndCountry(address)
                            resolvedCityName = cName
                            resolvedCountry = cCountry
                        }
                    }
                }
            } catch (e: Exception) {
                // Ağ yok veya Geocoder zaman aşımı -> fallback
            }

            updateCurrentCity(lat, lon, resolvedCityName, resolvedCountry)
        }
    }

    private fun extractCityAndCountry(address: Address): Pair<String?, String?> {
        val locality = address.locality?.trim()
        val adminArea = address.adminArea?.trim()
        val subAdmin = address.subAdminArea?.trim()

        val cityName = when {
            !locality.isNullOrEmpty() && !adminArea.isNullOrEmpty() && locality != adminArea -> {
                "$locality, $adminArea"
            }
            !adminArea.isNullOrEmpty() -> adminArea
            !locality.isNullOrEmpty() -> locality
            !subAdmin.isNullOrEmpty() -> subAdmin
            else -> address.subLocality ?: address.featureName
        }

        return Pair(cityName, address.countryName)
    }

    private fun updateCurrentCity(
        lat: Double,
        lon: Double,
        detectedCityName: String?,
        detectedCountry: String?
    ) {
        val nearest = QiblaCalculator.findNearestPredefinedCity(lat, lon)
        val finalName = detectedCityName ?: nearest?.name ?: getString(R.string.current_location)
        val finalCountry = detectedCountry ?: nearest?.country ?: "Türkiye"

        lifecycleScope.launch(Dispatchers.Main) {
            currentCity = City(
                name = finalName,
                country = finalCountry,
                latitude = lat,
                longitude = lon
            )
        }
    }
}

