package com.example.salvo.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("SalvoSessao", Context.MODE_PRIVATE)

    // 🔥 Guarda todos os dados necessários
    fun salvarSessao(userId: Int, role: String, nome: String) {
        val editor = prefs.edit()
        editor.putInt("USER_ID", userId)
        editor.putString("USER_ROLE", role)
        editor.putString("USER_NOME", nome)
        editor.apply()
    }

    fun buscarUserId(): Int {
        return prefs.getInt("USER_ID", -1)
    }

    fun buscarUserRole(): String {
        return prefs.getString("USER_ROLE", "customer") ?: "customer"
    }

    fun buscarUserNome(): String {
        return prefs.getString("USER_NOME", "Usuário") ?: "Usuário"
    }

    fun limparSessao() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}