package com.airsense.simulator

import com.airsense.domain.*
import com.airsense.service.DeviceService
import com.airsense.service.SensorSampleService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import kotlin.math.sin
import kotlin.random.Random
import java.util.Random as JavaRandom

/**
 * Generates realistic sensor data that mimics real-world indoor air quality patterns:
 * - CO2 follows occupancy curves (rises during working hours, drops at night; weekday vs weekend)
 * - Radon fluctuates with simulated barometric pressure changes (low pressure → more soil seepage)
 * - Temperature has a day/night sinusoidal cycle
 * - Humidity inversely correlates with temperature
 * - PM2.5 has random spikes (cooking, traffic) layered on occupancy baseline
 * - VOC follows occupancy with some lag
 * - Pressure drifts as a slow weather front simulation, driving radon behaviour
 */
@Component
@ConditionalOnProperty(name = ["airsense.simulator.enabled"], havingValue = "true")
class SensorDataSimulator(
    private val deviceService: DeviceService,
    private val sampleService: SensorSampleService
) {
    private val log = LoggerFactory.getLogger(SensorDataSimulator::class.java)

    private val deviceState = mutableMapOf<String, DeviceSimState>()
    private val rng = JavaRandom()

    // Shared weather state — all devices in the same "region" see the same pressure front
    private var weatherPressure = 1013.0
    private var weatherTrend = 0.0          // hPa per tick — positive = rising, negative = falling
    private var ticksSinceTrendChange = 0
    private var trendDuration = randomTrendDuration()

    data class DeviceSimState(
        var radon: Double = 50.0,
        var co2: Double = 420.0,
        var voc: Double = 100.0,
        var pm25: Double = 5.0,
        var temperature: Double = 21.0,
        var humidity: Double = 45.0,
        var pressure: Double = 1013.0,
        var tickCount: Long = 0
    )

    @Scheduled(fixedDelayString = "\${airsense.simulator.interval-ms}")
    fun generateSamples() {
        val now = Instant.now()
        val zoned = now.atZone(ZoneId.systemDefault())
        val hour = zoned.hour
        val minuteFraction = hour + zoned.minute / 60.0
        val dayOfWeek = zoned.dayOfWeek
        val isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY

        evolveWeather()

        val devices = deviceService.findAll()
        for (device in devices) {
            val state = deviceState.getOrPut(device.serialNumber) { DeviceSimState() }
            evolveState(state, minuteFraction, isWeekend, device.roomName ?: "")

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
            state.tickCount++

            // Battery drain: ~0.1% per hour (realistic for months-long battery life)
            if (state.tickCount % 360 == 0L) {
                device.batteryLevel = (device.batteryLevel - 1).coerceAtLeast(5)
            }
        }
        log.debug("Generated samples for {} devices (pressure={} hPa)", devices.size, "%.1f".format(weatherPressure))
    }

    /**
     * Simulates a slow-moving weather front.
     * Pressure drifts up or down for a random duration, then reverses or stabilises,
     * bounded within a realistic 990–1040 hPa range.
     */
    private fun evolveWeather() {
        ticksSinceTrendChange++
        if (ticksSinceTrendChange >= trendDuration) {
            // Pick a new trend: slight bias toward returning to 1013 hPa
            val returnBias = (1013.0 - weatherPressure) * 0.01
            weatherTrend = returnBias + rng.nextGaussian() * 0.3
            trendDuration = randomTrendDuration()
            ticksSinceTrendChange = 0
        }
        weatherPressure += weatherTrend + rng.nextGaussian() * 0.1
        weatherPressure = weatherPressure.coerceIn(990.0, 1040.0)
    }

    private fun evolveState(state: DeviceSimState, hourFrac: Double, isWeekend: Boolean, roomName: String) {
        val hour = hourFrac.toInt()
        val occupancy = computeOccupancy(hour, isWeekend, roomName)

        val isBasement = roomName.lowercase().contains("basement")
        val isServerRoom = roomName.lowercase().contains("server")
        val isMeetingRoom = roomName.lowercase().contains("meeting")
        val isKitchen = roomName.lowercase().contains("kitchen")

        // --- CO2: 400 ppm base + occupancy contribution ---
        val co2Target = 400.0 + (occupancy * 800.0) +
                (if (isMeetingRoom) occupancy * 200.0 else 0.0) +
                rng.nextGaussian() * 15.0
        state.co2 += (co2Target - state.co2) * 0.15

        // --- Radon: driven by pressure drops (low pressure → higher seepage) ---
        val radonBase = if (isBasement) 120.0 else 50.0
        // Pressure below 1013 increases radon; above 1013 suppresses it
        val pressureEffect = (1013.0 - weatherPressure) * 0.8
        // Falling pressure trend amplifies radon further
        val trendEffect = if (weatherTrend < 0) -weatherTrend * 15.0 else 0.0
        val radonTarget = radonBase + pressureEffect + trendEffect
        state.radon += (radonTarget - state.radon) * 0.05 + rng.nextGaussian() * 3.0
        // Occasional ventilation-driven spike (e.g., opening a basement door)
        if (Random.nextDouble() < 0.015) {
            state.radon += Random.nextDouble(15.0, 50.0)
        }

        // --- Temperature: sinusoidal day/night cycle ---
        // Peak at ~15:00 (hourFrac=15), trough at ~05:00
        val tempBase = if (isServerRoom) 28.0 else 21.0 + sin((hourFrac - 5.0) * Math.PI / 12.0) * 2.5
        state.temperature += (tempBase - state.temperature) * 0.1 + rng.nextGaussian() * 0.15

        // --- Humidity: inversely correlated with temperature ---
        val humTarget = 55.0 - (state.temperature - 21.0) * 3.0 + rng.nextGaussian() * 1.5
        state.humidity += (humTarget - state.humidity) * 0.1

        // --- VOC: occupancy with lag + kitchen spikes ---
        val vocBase = 80.0 + occupancy * 300.0 +
                (if (isKitchen && hour in 11..13) 200.0 else 0.0) +
                (if (isKitchen && hour in 17..19) 250.0 else 0.0)
        val vocTarget = vocBase + rng.nextGaussian() * 25.0
        state.voc += (vocTarget - state.voc) * 0.08

        // --- PM2.5: occupancy baseline + cooking/traffic spikes ---
        val pmBase = 5.0 + occupancy * 8.0 +
                (if (isKitchen && hour in 11..13) 15.0 else 0.0) +
                (if (isKitchen && hour in 17..19) 20.0 else 0.0)
        val pmTarget = pmBase + rng.nextGaussian() * 1.5
        state.pm25 += (pmTarget - state.pm25) * 0.1
        if (Random.nextDouble() < 0.03) {
            state.pm25 += Random.nextDouble(8.0, 25.0)
        }

        // --- Pressure: tracks shared weather with per-device noise ---
        state.pressure += (weatherPressure - state.pressure) * 0.3 + rng.nextGaussian() * 0.1
    }

    /**
     * Occupancy factor [0..1] based on time of day, weekday/weekend, and room type.
     */
    private fun computeOccupancy(hour: Int, isWeekend: Boolean, roomName: String): Double {
        val isMeetingRoom = roomName.lowercase().contains("meeting")

        if (isWeekend) {
            // Weekends: low activity, slight bump midday (residential patterns)
            return when (hour) {
                in 9..11 -> 0.2 + Random.nextDouble(0.0, 0.15)
                in 12..16 -> 0.25 + Random.nextDouble(0.0, 0.15)
                in 17..21 -> 0.3 + Random.nextDouble(0.0, 0.15)
                else -> 0.05 + Random.nextDouble(0.0, 0.05)
            }
        }

        // Weekday office patterns
        val base = when (hour) {
            in 7..7 -> 0.15 + Random.nextDouble(0.0, 0.1)   // early arrivals
            in 8..11 -> 0.7 + Random.nextDouble(0.0, 0.25)   // morning peak
            in 12..13 -> 0.35 + Random.nextDouble(0.0, 0.15) // lunch dip
            in 14..17 -> 0.6 + Random.nextDouble(0.0, 0.3)   // afternoon
            in 18..19 -> 0.2 + Random.nextDouble(0.0, 0.15)  // stragglers
            in 20..21 -> 0.1 + Random.nextDouble(0.0, 0.1)   // cleaners
            else -> 0.05 + Random.nextDouble(0.0, 0.05)       // night
        }

        // Meeting rooms have sharper peaks — occupied or empty
        return if (isMeetingRoom) {
            if (hour in 9..16 && Random.nextDouble() < 0.6) base * 1.3 else base * 0.3
        } else {
            base
        }
    }

    private fun randomTrendDuration(): Int = Random.nextInt(30, 120) // ticks (~5–20 min at 10s interval)
}
