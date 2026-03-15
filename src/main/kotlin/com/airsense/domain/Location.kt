package com.airsense.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "locations")
class Location(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    val account: Account,

    @Column(nullable = false)
    var name: String,

    var address: String? = null,

    var floors: Int = 1,

    @Enumerated(EnumType.STRING)
    var buildingType: BuildingType = BuildingType.OTHER,

    @Enumerated(EnumType.STRING)
    var ventilationType: VentilationType = VentilationType.UNKNOWN,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "location", cascade = [CascadeType.ALL], orphanRemoval = true)
    val devices: MutableList<Device> = mutableListOf()
)
