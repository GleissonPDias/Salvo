package com.example.salvo


import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PerfilRepository {

    fun alternarStatusOnline(providerId: Int, isOnline: Boolean, onResultado: (Boolean) -> Unit) {
        val dados = mapOf(
            "provider_id" to providerId.toString(),
            "is_online" to isOnline.toString()
        )

        RetrofitClient.apiService.alterarStatusOnline(dados).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                onResultado(response.isSuccessful)
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                onResultado(false)
            }
        })
    }
}