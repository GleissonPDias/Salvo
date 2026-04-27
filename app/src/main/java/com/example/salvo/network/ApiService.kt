package com.example.salvo.network

import com.example.salvo.model.AuthResponse
import com.example.salvo.model.LoginRequest
import okhttp3.Request
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("login")
    fun logar(@Body request: LoginRequest): Call<AuthResponse>
}