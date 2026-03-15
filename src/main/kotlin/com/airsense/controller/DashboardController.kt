package com.airsense.controller

import com.airsense.domain.*
import com.airsense.dto.*
import com.airsense.service.*
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import java.util.UUID

private val DEMO_ACCOUNT_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001")

@Controller
class DashboardController(
    private val locationService: LocationService,
    private val deviceService: DeviceService,
    private val sampleService: SensorSampleService,
    private val alertService: AlertService
) {

    @GetMapping("/")
    fun dashboard(model: Model): String {
        val locations = locationService.findAll()
        model.addAttribute("summary", DashboardSummary(
            totalLocations = locations.size,
            totalDevices = locations.sumOf { it.deviceCount },
            unacknowledgedAlerts = alertService.countUnacknowledged(),
            locations = locations,
            recentAlerts = alertService.getRecentAlerts(5)
        ))
        return "dashboard/index"
    }

    // ── Locations ──

    @GetMapping("/locations/{id}")
    fun locationDetail(@PathVariable id: UUID, model: Model): String {
        model.addAttribute("location", locationService.findByIdAsDto(id))
        model.addAttribute("devices", deviceService.findByLocationId(id))
        model.addAttribute("alerts", alertService.getUnacknowledgedByLocation(id))
        return "location/detail"
    }

    @GetMapping("/locations/new")
    fun newLocationForm(model: Model): String {
        model.addAttribute("form", LocationFormDto())
        model.addAttribute("buildingTypes", BuildingType.entries)
        model.addAttribute("ventilationTypes", VentilationType.entries)
        return "location/form"
    }

    @PostMapping("/locations")
    fun createLocation(@ModelAttribute form: LocationFormDto): String {
        locationService.create(DEMO_ACCOUNT_ID, form)
        return "redirect:/"
    }

    @GetMapping("/locations/{id}/edit")
    fun editLocationForm(@PathVariable id: UUID, model: Model): String {
        val location = locationService.findById(id)
        model.addAttribute("locationId", id)
        model.addAttribute("form", LocationFormDto(
            name = location.name,
            address = location.address ?: "",
            floors = location.floors,
            buildingType = location.buildingType,
            ventilationType = location.ventilationType
        ))
        model.addAttribute("buildingTypes", BuildingType.entries)
        model.addAttribute("ventilationTypes", VentilationType.entries)
        return "location/form"
    }

    @PostMapping("/locations/{id}")
    fun updateLocation(@PathVariable id: UUID, @ModelAttribute form: LocationFormDto): String {
        locationService.update(id, form)
        return "redirect:/locations/$id"
    }

    @PostMapping("/locations/{id}/delete")
    fun deleteLocation(@PathVariable id: UUID): String {
        locationService.delete(id)
        return "redirect:/"
    }

    // ── Devices ──

    @GetMapping("/devices/{id}")
    fun deviceDetail(@PathVariable id: UUID, model: Model): String {
        model.addAttribute("device", deviceService.findByIdAsDto(id))
        model.addAttribute("history", sampleService.getHistory(id, 24))
        model.addAttribute("avg24h", sampleService.getAverages(id, 24))
        model.addAttribute("avg7d", sampleService.getAverages(id, 168))
        model.addAttribute("alerts", alertService.getAlertsByDevice(id))
        model.addAttribute("alertRules", alertService.getRulesByDevice(id))
        model.addAttribute("sensorTypes", deviceService.findById(id).supportedSensors)
        return "device/detail"
    }

    @GetMapping("/devices/new")
    fun newDeviceForm(@RequestParam locationId: UUID, model: Model): String {
        model.addAttribute("form", DeviceFormDto(locationId = locationId))
        model.addAttribute("deviceTypes", DeviceType.entries)
        model.addAttribute("locationId", locationId)
        return "device/form"
    }

    @PostMapping("/devices")
    fun createDevice(@ModelAttribute form: DeviceFormDto): String {
        val device = deviceService.create(form)
        return "redirect:/locations/${form.locationId}"
    }

    @PostMapping("/devices/{id}/delete")
    fun deleteDevice(@PathVariable id: UUID): String {
        val device = deviceService.findById(id)
        val locationId = device.location.id
        deviceService.delete(id)
        return "redirect:/locations/$locationId"
    }

    // ── Alerts ──

    @GetMapping("/alerts")
    fun alertsPage(model: Model): String {
        model.addAttribute("alerts", alertService.getRecentAlerts(50))
        return "alert/list"
    }

    @PostMapping("/alerts/{id}/acknowledge")
    fun acknowledgeAlert(@PathVariable id: UUID, @RequestHeader(value = "Referer", required = false) referer: String?): String {
        alertService.acknowledge(id)
        return "redirect:${referer ?: "/alerts"}"
    }

    @PostMapping("/devices/{deviceId}/alert-rules")
    fun createAlertRule(@PathVariable deviceId: UUID, @ModelAttribute form: AlertRuleFormDto): String {
        val device = deviceService.findById(deviceId)
        alertService.createRule(form, device)
        return "redirect:/devices/$deviceId"
    }

    @PostMapping("/alert-rules/{id}/toggle")
    fun toggleAlertRule(@PathVariable id: UUID, @RequestHeader(value = "Referer", required = false) referer: String?): String {
        alertService.toggleRule(id)
        return "redirect:${referer ?: "/alerts"}"
    }

    @PostMapping("/alert-rules/{id}/delete")
    fun deleteAlertRule(@PathVariable id: UUID, @RequestHeader(value = "Referer", required = false) referer: String?): String {
        alertService.deleteRule(id)
        return "redirect:${referer ?: "/alerts"}"
    }
}
