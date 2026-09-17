package com.phonefortress.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.phonefortress.app.domain.model.SafeZone
import com.phonefortress.app.domain.model.ZoneType

@Entity(tableName = "safe_zones")
data class SafeZoneEntity(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val type: String,
    val enabled: Boolean,
    val createdAt: Long
) {
    fun toDomain() = SafeZone(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        radiusMeters = radiusMeters,
        type = runCatching { ZoneType.valueOf(type) }.getOrDefault(ZoneType.NEUTRAL),
        enabled = enabled,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(z: SafeZone) = SafeZoneEntity(
            id = z.id,
            name = z.name,
            latitude = z.latitude,
            longitude = z.longitude,
            radiusMeters = z.radiusMeters,
            type = z.type.name,
            enabled = z.enabled,
            createdAt = z.createdAt
        )
    }
}
