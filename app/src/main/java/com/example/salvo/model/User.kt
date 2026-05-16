package com.example.salvo.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String
)

data class AuthResponse(
    val sucesso: Boolean,
    val message: String,
    val userId: Int? = null,
    val nome: String? = null,
    val role: String? = null,
    val token: String? = null
)