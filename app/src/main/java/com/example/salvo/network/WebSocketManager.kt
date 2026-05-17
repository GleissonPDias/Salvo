package com.example.salvo.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.*
import org.json.JSONObject

class WebSocketManager(
    private val providerId: Int,
    private val onChamadoRecebido: (JSONObject) -> Unit
) {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val TAG = "SALVO_WEBSOCKET"

    fun conectar() {
        val url = "wss://apisalvologin.onrender.com/radar-provider/$providerId"
        Log.d(TAG, "Tentando conectar ao Socket no endereço: $url")

        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "🟢 CONECTADO COM SUCESSO! Oficina $providerId está ouvindo o servidor.")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.w(TAG, "🚨 CHEGOU MENSAGEM DO SERVIDOR: $text")
                try {
                    val jsonObject = JSONObject(text)
                    mainHandler.post {
                        onChamadoRecebido(jsonObject)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao processar JSON recebido: ${e.message}")
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.e(TAG, "🔴 Socket Fechado. Motivo: $reason (Código: $code)")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "💥 FALHA CRÍTICA NO SOCKET: ${t.message}")
                // Tentativa de reconexão automática em 5 segundos
                mainHandler.postDelayed({
                    Log.d(TAG, "Tentando reconectar automaticamente...")
                    conectar()
                }, 5000)
            }
        })
    }

    fun desconectar() {
        Log.w(TAG, "Desconectando manualmente do Socket...")
        webSocket?.close(1000, "Mecânico ficou Offline")
        webSocket = null
    }
}