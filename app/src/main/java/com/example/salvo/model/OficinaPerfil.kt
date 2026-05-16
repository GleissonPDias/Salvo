package com.example.salvo.model

import com.google.gson.annotations.SerializedName

data class OficinaPerfil(
    val id: Int,
    @SerializedName("user_name") var nome: String,
    @SerializedName("user_cnpj") var cnpj: String?,
    @SerializedName("user_address") var endereco: String?,
    @SerializedName("user_banner") var urlBanner: String?
)