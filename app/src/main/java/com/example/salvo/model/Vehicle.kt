package com.example.salvo.model

import com.google.gson.annotations.SerializedName

data class Vehicle(
    val id: Int,

    @SerializedName("provider_id")
    val providerId: Int,

    val name: String,         // Usado como Modelo (ex: FIAT STRADA)
    val plate: String,
    val status: String,

    @SerializedName("is_active")
    val isActive: Boolean,    // Convertido para Boolean/camelCase (GSON faz a conversão do TINYINT do MySQL automaticamente)

    val brand: String?,       // NOVO: Marca (ex: Fiat)

    @SerializedName("vehicle_type")
    val vehicleType: String?, // NOVO: Tipo (ex: Guincho, Moto)

    @SerializedName("maintenance_date")
    val maintenanceDate: String?, // NOVO: Data (ex: 15/06/2026)

    @SerializedName("vehicle_photo")
    val vehiclePhoto: String? // Base64
)
