package com.airsense.service

import com.airsense.domain.*
import com.airsense.dto.*
import com.airsense.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class LocationService(
    private val locationRepository: LocationRepository,
    private val accountRepository: AccountRepository,
    private val deviceRepository: DeviceRepository,
    private val sampleRepository: SensorSampleRepository
) {

    fun findAll(): List<LocationDto> =
        locationRepository.findAll().map { it.toDto() }

    fun findById(id: UUID): Location =
        locationRepository.findById(id)
            .orElseThrow { NoSuchElementException("Location not found: $id") }

    fun findByIdAsDto(id: UUID): LocationDto = findById(id).toDto()

    @Transactional
    fun create(accountId: UUID, form: LocationFormDto): Location {
        val account = accountRepository.findById(accountId)
            .orElseThrow { NoSuchElementException("Account not found: $accountId") }

        val location = Location(
            account = account,
            name = form.name,
            address = form.address.ifBlank { null },
            floors = form.floors,
            buildingType = form.buildingType,
            ventilationType = form.ventilationType
        )
        return locationRepository.save(location)
    }

    @Transactional
    fun update(id: UUID, form: LocationFormDto): Location {
        val location = findById(id)
        location.name = form.name
        location.address = form.address.ifBlank { null }
        location.floors = form.floors
        location.buildingType = form.buildingType
        location.ventilationType = form.ventilationType
        return locationRepository.save(location)
    }

    @Transactional
    fun delete(id: UUID) = locationRepository.deleteById(id)

    private fun Location.toDto(): LocationDto {
        val devices = deviceRepository.findByLocationId(id!!)
        val worstRating = devices.mapNotNull { device ->
            sampleRepository.findLatestByDeviceId(device.id!!)?.overallRating
        }.maxByOrNull { it.ordinal } ?: AirQualityRating.GOOD

        return LocationDto(
            id = id!!,
            name = name,
            address = address,
            floors = floors,
            buildingType = buildingType,
            ventilationType = ventilationType,
            deviceCount = devices.size,
            overallRating = worstRating
        )
    }
}
