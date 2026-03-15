package com.airsense.simulator

import com.airsense.domain.*
import com.airsense.service.DeviceService
import com.airsense.service.SensorSampleService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneId
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates realistic sensor data that mimics real-world indoor air quality patterns:
 * - CO2 follows occupancy curves (rises during working hours, drops at night)
 * - Radon fluctuates with "weather pressure" changes
 * - Temperature has a day/night cycle
 * - Humidity inversely correlates with temperature
 * - PM2.5 has random spikes (cooking, traffic)
 * - VOC follows occupancy with some lag
 */
@Component
@ConditionalOnProperty(name = ["airsense.simulator.enabled"], havingValue = "true")
class SensorDataSimulator(
    private val deviceService: DeviceService,
    private val sampleService: SensorSampleService
) {
    private val log = LoggerFactory.getLogger(SensorDataSimulator::class.java)

    // Track per-device state for smooth transitions
    private val deviceState = mutableMapOf<String, DeviceSimState>()

    data class DeviceSimState(
        var radon: Double = 50.0,
        var co2: Double = 420.0,
        var voc: Double = 100.0,
        var pm25: Double = 5.0,
        var temperature: Double = 21.0,
        var humidity: Double = 45.0,
        var pressure: Double = 1013.0
    )

    @Scheduled(fixedDelayString = "\${airsense.simulator.interval-ms}")
    fun generateSamples() {
        val now = Instant.now()
        val hour = now.atZone(ZoneId.systemDefault()).hour
        val devices = deviceService.findAll()

        for (device in devices) {
            val state = deviceState.getOrPut(device.serialNumber) { DeviceSimState() }
            evolveState(state, hour, device.roomName ?: "")

            val sample = SensorSample(
                device = device,
                recordedAt = now,
                radon = if (SensorType.RADON in device.supportedSensors)
                    state.radon.coerceAtLeast(0.0) else null,
                co2 = if (SensorType.CO2 in device.supportedSensors)
                    state.co2.coerceAtLeast(400.0) else null,
                voc = if (SensorType.VOC in device.supportedSensors)
                    state.voc.coerceAtLeast(0.0) else null,
                pm25 = if (SensorType.PM25 in device.supportedSensors)
                    state.pm25.coerceAtLeast(0.0) else null,
                temperature = if (SensorType.TEMPERATURE in device.supportedSensors)
                    state.temperature else null,
                humidity = if (SensorType.HUMIDITY in device.supportedSensors)
                    state.humidity.coerceIn(0.0, 100.0) else null,
                pressure = if (SensorType.PRESSURE in device.supportedSensors)
                    state.pressure else null
            )

            sampleService.ingest(device.id!!, sample)
        }
        log.debug("Generated samples for {} devices", devices.size)
    }

    private fun evolveState(state: DeviceSimState, hour: Int, roomName: String) {
        // Occupancy factor: higher during work hours (8-18)
        val occupancy = when (hour) {
            in 8..11 -> 0.7 + Random.nextDouble(0.0, 0.3)
            in 12..13 -> 0.4 + Random.nextDouble(0.0, 0.2) // lunch
            in 14..17 -> 0.6 + Random.nextDouble(0.0, 0.3)
            in 18..21 -> 0.2 + Random.nextDouble(0.0, 0.2)
            else -> 0.05 + Random.nextDouble(0.0, 0.1) // night
        }

        // Room-specific modifiers
        val isBasement = roomName.lowercase().contains("basement")
        val isServerRoom = roomName.lowercase().contains("server")
        val isMeetingRoom = roomName.lowercase().contains("meeting")

        // CO2: 400 ppm baseline + occupancy contribution, with smoothing
        val co2Target = 400.0 + (occupancy * 800.0) +
                (if (isMeetingRoom) 200.0 else 0.0) +
                Random.nextGaussian() * 20.0
        state.co2 += (co2Target - state.co2) * 0.15

        // Radon: slow drift with occasional spikes, basements are higher
        val radonBase = if (isBasement) 120.0 else 50.0
        state.radon += Random.nextGaussian() * 5.0
        state.radon += (radonBase - state.radon) * 0.05
        if (Random.nextDouble() < 0.02) { // 2% chance of spike
            state.radon += Random.nextDouble(20.0, 60.0)
        }

        // Temperature: day/night cycle, server rooms run hot
        val tempBase = if (isServerRoom) 28.0 else 21.0 + sin(hour * Math.PI / 12.0) * 2.0
        state.temperature += (tempBase - state.temperature) * 0.1 + Random.nextGaussian() * 0.2

        // Humidity: inversely related to temperature
        val humTarget = 55.0 - (state.temperature - 21.0) * 3.0 + Random.nextGaussian() * 2.0
        state.humidity += (humTarget - state.humidity) * 0.1

        // VOC: follows occupancy with lag
        val vocTarget = 80.0 + occupancy * 300.0 + Random.nextGaussian() * 30.0
        state.voc += (vocTarget - state.voc) * 0.08

        // PM2.5: baseline with occasional cooking/traffic spikes
        val pmTarget = 5.0 + occupancy * 8.0 + Random.nextGaussian() * 2.0
        state.pm25 += (pmTarget - state.pm25) * 0.1
        if (Random.nextDouble() < 0.03) {
            state.pm25 += Random.nextDouble(10.0, 30.0)
        }

        // Pressure: slow drift around 1013 hPa
        state.pressure += Random.nextGaussian() * 0.3
        state.pressure += (1013.0 - state.pressure) * 0.02
    }
}
