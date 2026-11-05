package com.qibla.compass.model

data class City(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double
)

object KaabaConstants {
    const val KAABA_LATITUDE = 21.422487
    const val KAABA_LONGITUDE = 39.826206
}

object PredefinedCities {
    val list = listOf(
        // Türkiye
        City("İstanbul", "Türkiye", 41.0082, 28.9784),
        City("Ankara", "Türkiye", 39.9334, 32.8597),
        City("İzmir", "Türkiye", 38.4192, 27.1287),
        City("Bursa", "Türkiye", 40.1885, 29.0610),
        City("Antalya", "Türkiye", 36.8969, 30.7133),
        City("Konya", "Türkiye", 37.8714, 32.4846),
        City("Gaziantep", "Türkiye", 37.0662, 37.3833),
        City("Trabzon", "Türkiye", 41.0027, 39.7168),
        City("Diyarbakır", "Türkiye", 37.9144, 40.2306),
        City("Erzurum", "Türkiye", 39.9043, 41.2679),

        // Almanya & Avrupa
        City("Berlin", "Almanya", 52.5200, 13.4050),
        City("Frankfurt", "Almanya", 50.1109, 8.6821),
        City("Münih", "Almanya", 48.1351, 11.5820),
        City("Köln", "Almanya", 50.9375, 6.9603),
        City("Londra", "İngiltere", 51.5074, -0.1278),
        City("Paris", "Fransa", 48.8566, 2.3522),
        City("Viyana", "Avusturya", 48.2082, 16.3738),
        City("Amsterdam", "Hollanda", 52.3676, 4.9041),

        // Orta Doğu & Körfez
        City("Mekke", "Suudi Arabistan", 21.4225, 39.8262),
        City("Medine", "Suudi Arabistan", 24.5247, 39.5692),
        City("Riyad", "Suudi Arabistan", 24.7136, 46.6753),
        City("Dubai", "BAE", 25.2048, 55.2708),
        City("Doha", "Katar", 25.2854, 51.5310),
        City("Kuveyt", "Kuveyt", 29.3759, 47.9774),
        City("Kahire", "Mısır", 30.0444, 31.2357),
        City("Kudüs", "Filistin", 31.7683, 35.2137),

        // Diğer
        City("Bakü", "Azerbaycan", 40.4093, 49.8671),
        City("Taşkent", "Özbekistan", 41.2995, 69.2401),
        City("Cakarta", "Endonezya", -6.2088, 106.8456),
        City("Kuala Lumpur", "Malezya", 3.1390, 101.6869),
        City("New York", "ABD", 40.7128, -74.0060),
        City("Chicago", "ABD", 41.8781, -87.6298)
    )
}
