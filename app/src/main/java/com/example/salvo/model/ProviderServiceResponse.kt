package com.example.salvo.model

import com.google.gson.annotations.SerializedName

data class ProviderServiceResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("service_type") val serviceType: String,
    @SerializedName("base_price") val basePrice: Double,
    @SerializedName("price_per_km") val pricePerKm: Double,
    @SerializedName("is_active") val isActive: Boolean
)