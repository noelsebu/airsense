package com.airsense.service

import com.airsense.domain.*
import com.airsense.dto.*
import com.airsense.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class DeviceService(
    private val deviceRepository: DeviceRepository,
    private val locationRepository: LocationRepository,
    private val sampleRepository: SensorSampleRepository
) {

    fun findByLocationId(locationId: UUID): List<DeviceDto> =
        deviceRepository.findByLocationId(locationId).map { device ->
            val latest = sampleRepository.findLatestByDeviceId(device.id!!)
            device.toDto(latest)
        }

    fun findById(id: UUID): Device =
        deviceRepository.findById(id)
            .orElseThrow { NoSuchElementException("Device not found: $id") }

    fun findByIdAsDto(id: UUID): DeviceDto {
        val device = findById(id)
        val latest = sampleRepository.findLatestByDeviceId(id)
        return device.toDto(latest)
    }

    @Transactional
    fun create(form: DeviceFormDto): Device {
        val location = locationRepository.findById(form.locationId!!)
            .orElseThrow { NoSuchElementException("Location not found: ${form.locationId}") }

        val device = Device(
            serialNumber = form.serialNumber,
            deviceType = form.deviceType,
            location = location,
            roomName = form.roomName.ifBlank { null },
            floor = form.floor
        )
        return deviceRepository.save(device)
    }

    @Transactional
    fun delete(id: UUID) = deviceRepository.deleteById(id)

    fun findAll(): List<Device> = deviceRepository.findAll()
}
