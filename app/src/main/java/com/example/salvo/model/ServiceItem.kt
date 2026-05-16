package com.example.salvo.model

import com.google.gson.annotations.SerializedName

data class ServiceItem(
    val id: Int,
    @SerializedName("provider_id") val providerId: Int,
    @SerializedName("service_type") val serviceType: String,
    @SerializedName("base_price") val basePrice: Double,
    @SerializedName("price_per_km") val pricePerKm: Double,
    @SerializedName("is_active") var isActive: Boolean
)