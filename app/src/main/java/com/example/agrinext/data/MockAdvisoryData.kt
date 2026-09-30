package com.example.agrinext.data

data class WeatherAlert(
    val title: String,
    val description: String,
    val severity: String,
    val date: String
)

data class SoilTelemetry(
    val nitrogen: Int,      // kg/ha
    val phosphorus: Int,    // kg/ha
    val potassium: Int,     // kg/ha
    val ph: Double,
    val moisturePercent: Int
)

data class SatelliteAlert(
    val ndviScore: Double,
    val status: String,
    val trend: String,
    val satelliteSource: String
)

object MockAdvisoryRepository {
    val sampleSoil = SoilTelemetry(
        nitrogen = 280,
        phosphorus = 18,
        potassium = 160,
        ph = 6.8,
        moisturePercent = 42
    )

    val sampleSatellite = SatelliteAlert(
        ndviScore = 0.68,
        status = "Healthy Vegetation",
        trend = "Improving (+0.04)",
        satelliteSource = "Sentinel-2 L2A (Copernicus)"
    )

    val sampleAlerts = listOf(
        WeatherAlert(
            title = "Unseasonal Rainfall Warning",
            description = "Light to moderate showers (12-18mm) expected in 48 hours. Postpone pesticide sprays.",
            severity = "Warning",
            date = "Today"
        ),
        WeatherAlert(
            title = "High Humidity Alert",
            description = "Relative humidity > 85% elevates risk of powdery mildew on vine canopies.",
            severity = "Advisory",
            date = "Tomorrow"
        )
    )
}