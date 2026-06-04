package com.example.salvo.model

import com.google.gson.annotations.SerializedName

data class GenericResponse(
    @SerializedName("sucesso") val sucesso: Boolean,
    @SerializedName("mensagem") val message: String
)

data class ResetPasswordRequest(
    val email: String
)