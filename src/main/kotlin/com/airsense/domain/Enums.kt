package com.airsense.domain

/**
 * Physical device types mirroring Airthings product line.
 */
enum class DeviceType(val displayName: String, val sensors: Set<SensorType>) {
    VIEW_PLUS(
        "View Plus",
        setOf(
            SensorType.RADON, SensorType.CO2, SensorType.VOC,
            SensorType.PM25, SensorType.TEMPERATURE, SensorType.HUMIDITY, SensorType.PRESSURE
        )
    ),
    WAVE_PLUS(
        "Wave Plus",
        setOf(
            SensorType.RADON, SensorType.CO2, SensorType.VOC,
            SensorType.TEMPERATURE, SensorType.HUMIDITY, SensorType.PRESSURE
        )
    ),
    WAVE_RADON(
        "Wave Radon",
        setOf(SensorType.RADON, SensorType.TEMPERATURE, SensorType.HUMIDITY)
    ),
    WAVE_MINI(
        "Wave Mini",
        setOf(SensorType.VOC, SensorType.TEMPERATURE, SensorType.HUMIDITY)
    ),
    SPACE_PRO(
        "Space Pro",
        setOf(
            SensorType.RADON, SensorType.CO2, SensorType.VOC,
            SensorType.PM25, SensorType.TEMPERATURE, SensorType.HUMIDITY, SensorType.PRESSURE
        )
    );
}

/**
 * Sensor measurement types with units and health thresholds.
 */
enum class SensorType(
    val displayName: String,
    val unit: String,
    val goodMax: Double,
    val fairMax: Double
) {
    RADON("Radon", "Bq/m³", 100.0, 150.0),
    CO2("CO₂", "ppm", 800.0, 1000.0),
    VOC("VOC", "ppb", 250.0, 2000.0),
    PM25("PM2.5", "µg/m³", 10.0, 25.0),
    TEMPERATURE("Temperature", "°C", 25.0, 28.0),
    HUMIDITY("Humidity", "%", 60.0, 70.0),
    PRESSURE("Pressure", "hPa", 1013.25, 1030.0);

    fun rate(value: Double): AirQualityRating = when {
        value <= goodMax -> AirQualityRating.GOOD
        value <= fairMax -> AirQualityRating.FAIR
        else -> AirQualityRating.POOR
    }
}

/**
 * Air quality rating — color-coded like Airthings' green/yellow/red system.
 */
enum class AirQualityRating(val label: String, val cssClass: String) {
    GOOD("Good", "text-bg-success"),
    FAIR("Fair", "text-bg-warning"),
    POOR("Poor", "text-bg-danger");
}

enum class VentilationType {
    NATURAL, MECHANICAL, HYBRID, UNKNOWN
}

enum class BuildingType {
    RESIDENTIAL, OFFICE, SCHOOL, HOSPITAL, WAREHOUSE, RETAIL, OTHER
}
