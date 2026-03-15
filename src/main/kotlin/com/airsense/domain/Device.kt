package com.airsense.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "devices")
class Device(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false, unique = true)
    val serialNumber: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val deviceType: DeviceType,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    var location: Location,

    var roomName: String? = null,

    var floor: Int = 1,

    var firmwareVersion: String = "1.0.0",

    var batteryLevel: Int = 100,

    var lastSeenAt: Instant = Instant.now(),

    @Column(nullable = false)
    val registeredAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "device", cascade = [CascadeType.ALL], orphanRemoval = true)
    val samples: MutableList<SensorSample> = mutableListOf(),

    @OneToMany(mappedBy = "device", cascade = [CascadeType.ALL], orphanRemoval = true)
    val alertRules: MutableList<AlertRule> = mutableListOf()
) {
    /**
     * Which sensors this device supports, based on its type.
     */
    val supportedSensors: Set<SensorType>
        get() = deviceType.sensors
}
