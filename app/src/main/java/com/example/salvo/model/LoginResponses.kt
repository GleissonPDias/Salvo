package com.example.salvo.model

import com.google.gson.annotations.SerializedName


data class RegisterRequest(
    @SerializedName("nome") val nome: String,
    @SerializedName("email") val email: String,
    @SerializedName("cpf") val cpf: String,
    @SerializedName("telefone") val telefone: String,
    @SerializedName("password") val password: String,
    @SerializedName("role") val role: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double

)

