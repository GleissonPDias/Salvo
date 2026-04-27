package com.example.salvo

import com.example.salvo.model.AuthResponse
import com.example.salvo.model.LoginRequest
import com.example.salvo.model.PedidoSocorroRequest
import com.example.salvo.model.PedidoSocorroResponse
import com.example.salvo.model.RegisterRequest
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// 1. A Interface Central com TODAS as rotas
interface ApiService {
    @POST("login")
    fun login(@Body request: LoginRequest): Call<AuthResponse>

    @POST("cadastro")
    fun cadastrar(@Body request: RegisterRequest): Call<AuthResponse>

    // A nossa nova rota do radar adicionada aqui!
    @POST("solicitar-socorro")
    fun solicitarSocorro(@Body request: PedidoSocorroRequest): Call<PedidoSocorroResponse>
}

// 2. O Singleton (O único "telefone" do aplicativo)
object RetrofitClient {
    private const val BASE_URL = "https://apisalvologin.onrender.com/" // A sua URL do Render

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}