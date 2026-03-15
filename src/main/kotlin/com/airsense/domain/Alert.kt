package com.airsense.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "alert_rules")
class AlertRule(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    val device: Device,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val sensorType: SensorType,

    @Column(nullable = false)
    var threshold: Double,

    var enabled: Boolean = true,

    val createdAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "alertRule", cascade = [CascadeType.ALL], orphanRemoval = true)
    val alerts: MutableList<Alert> = mutableListOf()
)

@Entity
@Table(name = "alerts")
class Alert(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_rule_id", nullable = false)
    val alertRule: AlertRule,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_id", nullable = false)
    val sample: SensorSample,

    @Column(nullable = false)
    val triggeredValue: Double,

    @Column(nullable = false)
    val threshold: Double,

    var acknowledged: Boolean = false,

    @Column(nullable = false)
    val triggeredAt: Instant = Instant.now()
)
