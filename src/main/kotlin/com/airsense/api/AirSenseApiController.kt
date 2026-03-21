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
        alertService.getAlertsForDevice(deviceId)

    @GetMapping("/devices/{deviceId}/alert-rules")
    fun getAlertRules(@PathVariable deviceId: UUID): List<AlertRuleDto> =
        alertService.getRulesForDevice(deviceId)

    @GetMapping("/locations/{locationId}/alerts")
    fun getLocationAlerts(@PathVariable locationId: UUID): List<AlertDto> =
        alertService.getUnacknowledgedForLocation(locationId)

    @GetMapping("/alerts/count")
    fun countUnacknowledged(): Map<String, Long> =
        mapOf("unacknowledged" to alertService.countUnacknowledged())

    @PostMapping("/alerts/{alertId}/acknowledge")
    fun acknowledgeAlert(@PathVariable alertId: UUID): ResponseEntity<Void> {
        alertService.acknowledgeAlert(alertId)
        return ResponseEntity.ok().build()
    }

    // ── Alert Rules ──

    @PostMapping("/devices/{deviceId}/alert-rules")
    fun createAlertRule(
        @PathVariable deviceId: UUID,
        @RequestBody form: AlertRuleFormDto
    ): ResponseEntity<AlertRuleDto> {
        form.deviceId = deviceId
        val rule = alertService.createRule(form)
        return ResponseEntity.status(201).body(
            AlertRuleDto(
                id = rule.id!!,
                deviceId = deviceId,
                deviceSerial = rule.device.serialNumber,
                sensorType = rule.sensorType,
                threshold = rule.threshold,
                enabled = rule.enabled
            )
        )
    }

    @PostMapping("/alert-rules/{id}/toggle")
    fun toggleAlertRule(@PathVariable id: UUID): ResponseEntity<Void> {
        alertService.toggleRule(id)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/alert-rules/{id}")
    fun deleteAlertRule(@PathVariable id: UUID): ResponseEntity<Void> {
        alertService.deleteRule(id)
        return ResponseEntity.noContent().build()
    }
}
