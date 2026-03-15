package com.airsense.api

import com.airsense.dto.*
import com.airsense.service.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class AirSenseApiController(
    private val locationService: LocationService,
    private val deviceService: DeviceService,
    private val sampleService: SensorSampleService,
    private val alertService: AlertService
) {

    // ── Locations ──

    @GetMapping("/locations")
    fun listLocations(): List<LocationDto> = locationService.findAll()

    @GetMapping("/locations/{id}")
    fun getLocation(@PathVariable id: UUID): LocationDto = locationService.findByIdAsDto(id)

    // ── Devices ──

    @GetMapping("/locations/{locationId}/devices")
    fun listDevices(@PathVariable locationId: UUID): List<DeviceDto> =
        deviceService.findByLocationId(locationId)

    @GetMapping("/devices/{id}")
    fun getDevice(@PathVariable id: UUID): DeviceDto = deviceService.findByIdAsDto(id)

    // ── Samples ──

    @GetMapping("/devices/{deviceId}/samples/latest")
    fun getLatestSample(@PathVariable deviceId: UUID): ResponseEntity<SampleDto> {
        val sample = sampleService.getLatestByDevice(deviceId)
        return if (sample != null) ResponseEntity.ok(sample) else ResponseEntity.noContent().build()
    }

    @GetMapping("/devices/{deviceId}/samples/history")
    fun getSampleHistory(
        @PathVariable deviceId: UUID,
        @RequestParam(defaultValue = "24") hours: Long
    ): List<SampleDto> = sampleService.getHistory(deviceId, hours)

    @GetMapping("/devices/{deviceId}/samples/averages")
    fun getSampleAverages(
        @PathVariable deviceId: UUID,
        @RequestParam(defaultValue = "24") hours: Long
    ): SampleAveragesDto = sampleService.getAverages(deviceId, hours)

    // ── Alerts ──

    @GetMapping("/devices/{deviceId}/alerts")
    fun getDeviceAlerts(@PathVariable deviceId: UUID): List<AlertDto> =
        alertService.getAlertsByDevice(deviceId)

    @GetMapping("/devices/{deviceId}/alert-rules")
    fun getAlertRules(@PathVariable deviceId: UUID): List<AlertRuleDto> =
        alertService.getRulesByDevice(deviceId)

    @PostMapping("/alerts/{alertId}/acknowledge")
    fun acknowledgeAlert(@PathVariable alertId: UUID): ResponseEntity<Void> {
        alertService.acknowledge(alertId)
        return ResponseEntity.ok().build()
    }
}
