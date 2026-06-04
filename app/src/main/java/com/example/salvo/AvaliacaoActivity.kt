package com.example.salvo

import android.os.Bundle
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.salvo.model.AuthResponse
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AvaliacaoActivity : AppCompatActivity() {

    private var pedidoId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_avaliacao)

        pedidoId = intent.getIntExtra("PEDIDO_ID", -1)

        val ratingBar = findViewById<RatingBar>(R.id.rating_bar)
        val etComentario = findViewById<TextInputEditText>(R.id.et_comentario_avaliacao)
        val btnEnviar = findViewById<MaterialButton>(R.id.btn_enviar_avaliacao)

        btnEnviar.setOnClickListener {
            val nota = ratingBar.rating.toInt()
            val comentario = etComentario.text.toString().trim()

            if (nota == 0) {
                Toast.makeText(this, "Selecione as estrelas para avaliar.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            enviarAvaliacao(nota, comentario)
        }
    }

    private fun enviarAvaliacao(nota: Int, comentario: String) {
        val dados = mapOf(
            "pedidoId" to pedidoId.toString(),
            "nota" to nota.toString(),
            "comentario" to comentario
        )

        RetrofitClient.apiService.enviarAvaliacao(dados).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AvaliacaoActivity, "Obrigado pela avaliação!", Toast.LENGTH_LONG).show()
                    finish() // Volta para a tela inicial
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                Toast.makeText(this@AvaliacaoActivity, "Erro ao enviar avaliação.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}