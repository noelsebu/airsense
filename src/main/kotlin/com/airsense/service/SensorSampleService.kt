package com.airsense.service

import com.airsense.domain.*
import com.airsense.dto.*
import com.airsense.repository.*
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Spring event published when a new sample is ingested.
 * The alert service listens for this to evaluate thresholds.
 */
data class SampleIngestedEvent(val sample: SensorSample)

@Service
@Transactional(readOnly = true)
class SensorSampleService(
    private val sampleRepository: SensorSampleRepository,
    private val deviceRepository: DeviceRepository,
    private val eventPublisher: ApplicationEventPublisher
) {

    /**
     * Ingest a new sensor sample: compute ratings, save, publish event.
     */
    @Transactional
    fun ingest(deviceId: UUID, sample: SensorSample): SensorSample {
        sample.computeRatings()
        val saved = sampleRepository.save(sample)

        // Update device last seen
        val device = saved.device
        device.lastSeenAt = saved.recordedAt
        device.batteryLevel = (device.batteryLevel - 1).coerceAtLeast(0)
        deviceRepository.save(device)

        // Publish event for alert evaluation
        eventPublisher.publishEvent(SampleIngestedEvent(saved))

        return saved
    }

    fun getLatestByDevice(deviceId: UUID): SampleDto? =
        sampleRepository.findLatestByDeviceId(deviceId)?.toDto()

    fun getHistory(deviceId: UUID, hours: Long = 24): List<SampleDto> {
        val from = Instant.now().minus(hours, ChronoUnit.HOURS)
        val to = Instant.now()
        return sampleRepository.findByDeviceIdAndTimeRange(deviceId, from, to)
            .map { it.toDto() }
    }

    fun getAverages(deviceId: UUID, hours: Long): SampleAveragesDto {
        val since = Instant.now().minus(hours, ChronoUnit.HOURS)
        val result = sampleRepository.findAveragesByDeviceIdSince(deviceId, since)

        val period = when (hours) {
            24L -> "24h"
            168L -> "7d"
            720L -> "30d"
            else -> "${hours}h"
        }

        return SampleAveragesDto(
            deviceId = deviceId,
            period = period,
            radon = result[0] as? Double,
            co2 = result[1] as? Double,
            voc = result[2] as? Double,
            pm25 = result[3] as? Double,
            temperature = result[4] as? Double,
            humidity = result[5] as? Double,
            pressure = result[6] as? Double
        )
    }
}
