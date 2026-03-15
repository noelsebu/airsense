package com.airsense.repository

import com.airsense.domain.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface AccountRepository : JpaRepository<Account, UUID>

@Repository
interface LocationRepository : JpaRepository<Location, UUID> {
    fun findByAccountId(accountId: UUID): List<Location>
}

@Repository
interface DeviceRepository : JpaRepository<Device, UUID> {
    fun findByLocationId(locationId: UUID): List<Device>
    fun findBySerialNumber(serialNumber: String): Device?
}

@Repository
interface SensorSampleRepository : JpaRepository<SensorSample, UUID> {

    @Query("SELECT s FROM SensorSample s WHERE s.device.id = :deviceId ORDER BY s.recordedAt DESC LIMIT 1")
    fun findLatestByDeviceId(@Param("deviceId") deviceId: UUID): SensorSample?

    @Query(
        """SELECT s FROM SensorSample s 
           WHERE s.device.id = :deviceId 
             AND s.recordedAt BETWEEN :from AND :to 
           ORDER BY s.recordedAt ASC"""
    )
    fun findByDeviceIdAndTimeRange(
        @Param("deviceId") deviceId: UUID,
        @Param("from") from: Instant,
        @Param("to") to: Instant
    ): List<SensorSample>

    @Query(
        """SELECT s FROM SensorSample s 
           WHERE s.device.location.id = :locationId 
           ORDER BY s.recordedAt DESC"""
    )
    fun findLatestByLocationId(@Param("locationId") locationId: UUID): List<SensorSample>

    @Query(
        """SELECT AVG(s.radon), AVG(s.co2), AVG(s.voc), AVG(s.pm25), 
                  AVG(s.temperature), AVG(s.humidity), AVG(s.pressure)
           FROM SensorSample s 
           WHERE s.device.id = :deviceId 
             AND s.recordedAt >= :since"""
    )
    fun findAveragesByDeviceIdSince(
        @Param("deviceId") deviceId: UUID,
        @Param("since") since: Instant
    ): Array<Any?>
}

@Repository
interface AlertRuleRepository : JpaRepository<AlertRule, UUID> {
    fun findByDeviceId(deviceId: UUID): List<AlertRule>
    fun findByDeviceIdAndEnabledTrue(deviceId: UUID): List<AlertRule>
}

@Repository
interface AlertRepository : JpaRepository<Alert, UUID> {

    @Query(
        """SELECT a FROM Alert a 
           WHERE a.alertRule.device.id = :deviceId 
           ORDER BY a.triggeredAt DESC"""
    )
    fun findByDeviceId(@Param("deviceId") deviceId: UUID): List<Alert>

    @Query(
        """SELECT a FROM Alert a 
           WHERE a.alertRule.device.location.id = :locationId 
             AND a.acknowledged = false 
           ORDER BY a.triggeredAt DESC"""
    )
    fun findUnacknowledgedByLocationId(@Param("locationId") locationId: UUID): List<Alert>

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.acknowledged = false")
    fun countUnacknowledged(): Long
}
