package com.qibla.compass.util

import com.qibla.compass.model.KaabaConstants
import kotlin.math.*

object QiblaCalculator {

    /**
     * Kullanıcının bulunduğu enlem ve boylamdan Kabe'ye olan gerçek yön açısını (0° - 360°) hesaplar.
     * Küresel trigonometri (Great Circle Forward Azimuth) formülü kullanılır.
     */
    fun calculateQiblaBearing(userLat: Double, userLon: Double): Float {
        val phi1 = Math.toRadians(userLat)
        val phi2 = Math.toRadians(KaabaConstants.KAABA_LATITUDE)
        val deltaLambda = Math.toRadians(KaabaConstants.KAABA_LONGITUDE - userLon)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)

        val bearingRad = atan2(y, x)
        val bearingDeg = Math.toDegrees(bearingRad)

        return ((bearingDeg + 360.0) % 360.0).toFloat()
    }

    /**
     * Kullanıcının Kabe'ye olan mesafesini Haversine formülü ile kilometre cinsinden hesaplar.
     */
    fun calculateDistanceToKaaba(userLat: Double, userLon: Double): Double {
        val earthRadiusKm = 6371.0

        val dLat = Math.toRadians(KaabaConstants.KAABA_LATITUDE - userLat)
        val dLon = Math.toRadians(KaabaConstants.KAABA_LONGITUDE - userLon)

        val lat1 = Math.toRadians(userLat)
        val lat2 = Math.toRadians(KaabaConstants.KAABA_LATITUDE)

        val a = sin(dLat / 2).pow(2) + sin(dLon / 2).pow(2) * cos(lat1) * cos(lat2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusKm * c
    }

    /**
     * Açıyı pusula yönü metnine dönüştürür (Kuzey, Güneydoğu vb.)
     */
    fun getCardinalDirection(angle: Float): String {
        val normalized = ((angle % 360) + 360) % 360
        return when {
            normalized in 337.5..360.0 || normalized in 0.0..22.5 -> "K (Kuzey)"
            normalized in 22.5..67.5 -> "KD (Kuzeydoğu)"
            normalized in 67.5..112.5 -> "D (Doğu)"
            normalized in 112.5..157.5 -> "GD (Güneydoğu)"
            normalized in 157.5..202.5 -> "G (Güney)"
            normalized in 202.5..247.5 -> "GB (Güneybatı)"
            normalized in 247.5..292.5 -> "B (Batı)"
            normalized in 292.5..337.5 -> "KB (Kuzeybatı)"
            else -> "K"
        }
    }
}
