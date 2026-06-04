package com.example.salvo

import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.salvo.model.GenericResponse
import com.example.salvo.model.ResetPasswordRequest
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

// IMPORTAÇÕES EXATAS DO RETROFIT PARA EVITAR ERROS
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RedefinirSenhaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_redefinir_senha)

        // Botão de voltar para a tela de Login
        findViewById<TextView>(R.id.tv_voltar_login).setOnClickListener {
            finish()
        }

        val etEmail = findViewById<TextInputEditText>(R.id.et_email_redefinir)
        val btnEnviar = findViewById<MaterialButton>(R.id.btn_enviar_link)

        btnEnviar.setOnClickListener {
            val email = etEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Por favor, digite seu e-mail.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Digite um e-mail válido.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnEnviar.isEnabled = false
            btnEnviar.text = "ENVIANDO..."

            enviarPedidoParaBackend(email, btnEnviar)
        }
    }

    private fun enviarPedidoParaBackend(email: String, btnEnviar: MaterialButton) {
        val pedido = ResetPasswordRequest(email = email)

        RetrofitClient.apiService.solicitarRedefinicaoSenha(pedido).enqueue(object : Callback<GenericResponse> {

            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                btnEnviar.isEnabled = true
                btnEnviar.text = "ENVIAR LINK"

                val corpoDaResposta = response.body()

                if (response.isSuccessful && corpoDaResposta != null && corpoDaResposta.sucesso) {
                    Toast.makeText(this@RedefinirSenhaActivity, corpoDaResposta.message, Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    val msgErro = corpoDaResposta?.message ?: "E-mail não encontrado no sistema."
                    Toast.makeText(this@RedefinirSenhaActivity, msgErro, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                btnEnviar.isEnabled = true
                btnEnviar.text = "ENVIAR LINK"
                Toast.makeText(this@RedefinirSenhaActivity, "Falha na conexão. Tente novamente.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}