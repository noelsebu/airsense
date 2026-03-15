package com.airsense.dto

import com.airsense.domain.*
import java.time.Instant
import java.util.UUID

// ── Location DTOs ──

data class LocationDto(
    val id: UUID,
    val name: String,
    val address: String?,
    val floors: Int,
    val buildingType: BuildingType,
    val ventilationType: VentilationType,
    val deviceCount: Int,
    val overallRating: AirQualityRating
)

data class LocationFormDto(
    var name: String = "",
    var address: String = "",
    var floors: Int = 1,
    var buildingType: BuildingType = BuildingType.OTHER,
    var ventilationType: VentilationType = VentilationType.UNKNOWN
)

// ── Device DTOs ──

data class DeviceDto(
    val id: UUID,
    val serialNumber: String,
    val deviceType: DeviceType,
    val roomName: String?,
    val floor: Int,
    val batteryLevel: Int,
    val lastSeenAt: Instant,
    val supportedSensors: Set<SensorType>,
    val latestSample: SampleDto?
)

data class DeviceFormDto(
    var serialNumber: String = "",
    var deviceType: DeviceType = DeviceType.VIEW_PLUS,
    var locationId: UUID? = null,
    var roomName: String = "",
    var floor: Int = 1
)

// ── Sample DTOs ──

data class SampleDto(
    val id: UUID,
    val recordedAt: Instant,
    val readings: List<SensorReadingDto>,
    val overallRating: AirQualityRating
)

data class SensorReadingDto(
    val sensorType: SensorType,
    val value: Double,
    val rating: AirQualityRating,
    val unit: String
)

data class SampleAveragesDto(
    val deviceId: UUID,
    val period: String,
    val radon: Double?,
    val co2: Double?,
    val voc: Double?,
    val pm25: Double?,
    val temperature: Double?,
    val humidity: Double?,
    val pressure: Double?
)

// ── Alert DTOs ──

data class AlertRuleDto(
    val id: UUID,
    val deviceId: UUID,
    val deviceSerial: String,
    val sensorType: SensorType,
    val threshold: Double,
    val enabled: Boolean
)

data class AlertRuleFormDto(
    var deviceId: UUID? = null,
    var sensorType: SensorType = SensorType.CO2,
    var threshold: Double = 1000.0,
    var enabled: Boolean = true
)

data class AlertDto(
    val id: UUID,
    val deviceSerial: String,
    val roomName: String?,
    val sensorType: SensorType,
    val triggeredValue: Double,
    val threshold: Double,
    val acknowledged: Boolean,
    val triggeredAt: Instant
)

// ── Dashboard DTOs ──

data class DashboardSummary(
    val totalLocations: Int,
    val totalDevices: Int,
    val unacknowledgedAlerts: Long,
    val locations: List<LocationDto>,
    val recentAlerts: List<AlertDto>
)

// ── Mapping extensions ──

fun SensorSample.toDto(): SampleDto {
    val readings = device.supportedSensors.mapNotNull { sensor ->
        val value = getValue(sensor)
        val rating = getRating(sensor)
        if (value != null && rating != null) {
            SensorReadingDto(sensor, value, rating, sensor.unit)
        } else null
    }
    return SampleDto(
        id = id!!,
        recordedAt = recordedAt,
        readings = readings,
        overallRating = overallRating
    )
}

fun Device.toDto(latestSample: SensorSample?): DeviceDto = DeviceDto(
    id = id!!,
    serialNumber = serialNumber,
    deviceType = deviceType,
    roomName = roomName,
    floor = floor,
    batteryLevel = batteryLevel,
    lastSeenAt = lastSeenAt,
    supportedSensors = supportedSensors,
    latestSample = latestSample?.toDto()
)

fun Alert.toDto(): AlertDto = AlertDto(
    id = id!!,
    deviceSerial = alertRule.device.serialNumber,
    roomName = alertRule.device.roomName,
    sensorType = alertRule.sensorType,
    triggeredValue = triggeredValue,
    threshold = threshold,
    acknowledged = acknowledged,
    triggeredAt = triggeredAt
)
