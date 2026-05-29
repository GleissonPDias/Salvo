package com.example.salvo.model

import com.google.gson.annotations.SerializedName

data class Vehicle(
    val id: Int,
    @SerializedName("provider_id") val providerId: Int,
    val name: String,         // Usado como Modelo (ex: FIAT STRADA)
    val plate: String,
    val status: String,
    val is_active: Int,
    val brand: String?,       // NOVO: Marca (ex: Fiat)
    val vehicle_type: String?, // NOVO: Tipo (ex: Guincho, Moto)
    val maintenance_date: String?, // NOVO: Data (ex: 15/06/2026)
    @SerializedName("vehicle_photo") val vehiclePhoto: String? // Base64
)
