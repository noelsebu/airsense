package com.airsense.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "sensor_samples",
    indexes = [
        Index(name = "idx_sample_device_recorded", columnList = "device_id, recorded_at"),
        Index(name = "idx_sample_recorded", columnList = "recorded_at")
    ]
)
class SensorSample(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    val device: Device,

    @Column(nullable = false)
    val recordedAt: Instant,

    // Sensor values — nullable because not all devices have all sensors
    var radon: Double? = null,
    var co2: Double? = null,
    var voc: Double? = null,
    var pm25: Double? = null,
    var temperature: Double? = null,
    var humidity: Double? = null,
    var pressure: Double? = null,

    // Computed ratings
    @Enumerated(EnumType.STRING)
    var radonRating: AirQualityRating? = null,
    @Enumerated(EnumType.STRING)
    var co2Rating: AirQualityRating? = null,
    @Enumerated(EnumType.STRING)
    var vocRating: AirQualityRating? = null,
    @Enumerated(EnumType.STRING)
    var pm25Rating: AirQualityRating? = null,
    @Enumerated(EnumType.STRING)
    var temperatureRating: AirQualityRating? = null,
    @Enumerated(EnumType.STRING)
    var humidityRating: AirQualityRating? = null,
    @Enumerated(EnumType.STRING)
    var pressureRating: AirQualityRating? = null
) {
    /**
     * Compute and store ratings for all non-null sensor values.
     */
    fun computeRatings() {
        radon?.let { radonRating = SensorType.RADON.rate(it) }
        co2?.let { co2Rating = SensorType.CO2.rate(it) }
        voc?.let { vocRating = SensorType.VOC.rate(it) }
        pm25?.let { pm25Rating = SensorType.PM25.rate(it) }
        temperature?.let { temperatureRating = SensorType.TEMPERATURE.rate(it) }
        humidity?.let { humidityRating = SensorType.HUMIDITY.rate(it) }
        pressure?.let { pressureRating = SensorType.PRESSURE.rate(it) }
    }

    /**
     * Overall worst rating across all sensors.
     */
    val overallRating: AirQualityRating
        get() = listOfNotNull(
            radonRating, co2Rating, vocRating,
            pm25Rating, temperatureRating, humidityRating
        ).maxByOrNull { it.ordinal } ?: AirQualityRating.GOOD

    /**
     * Get value for a specific sensor type.
     */
    fun getValue(sensorType: SensorType): Double? = when (sensorType) {
        SensorType.RADON -> radon
        SensorType.CO2 -> co2
        SensorType.VOC -> voc
        SensorType.PM25 -> pm25
        SensorType.TEMPERATURE -> temperature
        SensorType.HUMIDITY -> humidity
        SensorType.PRESSURE -> pressure
    }

    fun getRating(sensorType: SensorType): AirQualityRating? = when (sensorType) {
        SensorType.RADON -> radonRating
        SensorType.CO2 -> co2Rating
        SensorType.VOC -> vocRating
        SensorType.PM25 -> pm25Rating
        SensorType.TEMPERATURE -> temperatureRating
        SensorType.HUMIDITY -> humidityRating
        SensorType.PRESSURE -> pressureRating
    }
}
