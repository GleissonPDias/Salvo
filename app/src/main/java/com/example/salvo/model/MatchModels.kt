package com.example.salvo.model

import com.google.gson.annotations.SerializedName

data class PedidoSocorroRequest(
    @SerializedName("customerId") val customerId: Int,
    @SerializedName("clienteNome") val clienteNome: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("serviceType") val serviceType: String,
    @SerializedName("vehicleId") val vehicleId: Int,
    @SerializedName("description") val description: String
)

data class PedidoSocorroResponse(
    @SerializedName("sucesso") val sucesso: Boolean,
    @SerializedName("mensagem") val mensagem: String,
    @SerializedName("requestId") val requestId: Int?,
    @SerializedName("mecanicosNotificados") val mecanicosNotificados: Int
)