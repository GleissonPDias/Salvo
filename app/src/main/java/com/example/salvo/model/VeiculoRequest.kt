package com.example.salvo.model

import com.google.gson.annotations.SerializedName

data class VeiculoRequest(
    val id: Int? = null,

    @SerializedName("provider_id")
    val providerId: Int? = null,

    val name: String,
    val plate: String,
    val status: String? = "Disponível",
    val brand: String? = null,

    @SerializedName("vehicle_type")
    val vehicleType: String? = null,

    @SerializedName("maintenance_date")
    val maintenanceDate: String? = null,

    @SerializedName("vehicle_photo")
    val vehiclePhoto: String? = null
)