package com.example.salvo.model

import com.google.gson.annotations.SerializedName

data class Vehicle(
    val id: Int,
    @SerializedName("provider_id") val providerId: Int,
    val name: String,
    val plate: String,
    val status: String,
    @SerializedName("vehicle_photo") val vehiclePhoto: String? // Base64
)
