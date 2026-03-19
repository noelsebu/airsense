package com.airsense.service

import com.airsense.domain.*
import com.airsense.dto.*
import com.airsense.repository.*
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AlertService(
    private val alertRuleRepository: AlertRuleRepository,
    private val alertRepository: AlertRepository,
    private val deviceRepository: DeviceRepository
) {
    private val log = LoggerFactory.getLogger(AlertService::class.java)

    @EventListener
    @Transactional
    fun onSampleIngested(event: SampleIngestedEvent) {
        val sample = event.sample
        val rules = alertRuleRepository.findByDeviceIdAndEnabledTrue(sample.device.id!!)

        for (rule in rules) {
            val value = sample.getValue(rule.sensorType) ?: continue
            if (value > rule.threshold) {
                val alert = Alert(
                    alertRule = rule,
                    sample = sample,
                    triggeredValue = value,
                    threshold = rule.threshold
                )
                alertRepository.save(alert)
                log.warn(
                    "ALERT: {} on device {} = {} {} (threshold: {})",
                    rule.sensorType.displayName,
                    sample.device.serialNumber,
                    String.format("%.1f", value),
                    rule.sensorType.unit,
                    String.format("%.1f", rule.threshold)
                )
            }
        }
    }

    fun getRulesForDevice(deviceId: UUID): List<AlertRuleDto> =
        alertRuleRepository.findByDeviceId(deviceId).map { it.toDto() }

    fun getAlertsForDevice(deviceId: UUID): List<AlertDto> =
        alertRepository.findByDeviceId(deviceId).map { it.toDto() }

    fun getUnacknowledgedForLocation(locationId: UUID): List<AlertDto> =
        alertRepository.findUnacknowledgedByLocationId(locationId).map { it.toDto() }

    fun countUnacknowledged(): Long = alertRepository.countUnacknowledged()

    fun getRecentAlerts(limit: Int = 10): List<AlertDto> =
        alertRepository.findAll()
            .sortedByDescending { it.triggeredAt }
            .take(limit)
            .map { it.toDto() }

    @Transactional
    fun createRule(form: AlertRuleFormDto): AlertRule {
        val device = deviceRepository.findById(form.deviceId!!)
            .orElseThrow { NoSuchElementException("Device not found: ${form.deviceId}") }
        val rule = AlertRule(
            device = device,
            sensorType = form.sensorType,
            threshold = form.threshold,
            enabled = form.enabled
        )
        return alertRuleRepository.save(rule)
    }

    @Transactional
    fun toggleRule(id: UUID) {
        val rule = alertRuleRepository.findById(id)
            .orElseThrow { NoSuchElementException("Alert rule not found: $id") }
        rule.enabled = !rule.enabled
        alertRuleRepository.save(rule)
    }

    @Transactional
    fun deleteRule(id: UUID) = alertRuleRepository.deleteById(id)

    @Transactional
    fun acknowledgeAlert(id: UUID) {
        val alert = alertRepository.findById(id)
            .orElseThrow { NoSuchElementException("Alert not found: $id") }
        alert.acknowledged = true
        alertRepository.save(alert)
    }

    private fun AlertRule.toDto() = AlertRuleDto(
        id = id!!,
        deviceId = device.id!!,
        deviceSerial = device.serialNumber,
        sensorType = sensorType,
        threshold = threshold,
        enabled = enabled
    )
}
