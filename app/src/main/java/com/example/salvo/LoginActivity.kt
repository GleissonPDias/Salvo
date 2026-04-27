package com.example.salvo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.salvo.model.AuthResponse
import com.example.salvo.model.LoginRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var edit_email: EditText
    private lateinit var edit_password: EditText
    private lateinit var bt_entrar: Button
    private lateinit var bt_cadastro: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Inicializando os componentes do XML
        edit_email = findViewById(R.id.edit_email)
        edit_password = findViewById(R.id.edit_password)
        bt_entrar = findViewById(R.id.btn_login)
        bt_cadastro = findViewById(R.id.text_cadastro)

        bt_entrar.setOnClickListener {
            executarLogin()
        }

        bt_cadastro.setOnClickListener {
            val intent = Intent(this@LoginActivity, RegisterChooseActivity::class.java)
            startActivity(intent)
        }
    }

    private fun executarLogin() {
        val email = edit_email.text.toString().trim()
        val password = edit_password.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Usando o nosso Retrofit Central!
        // Não precisamos mais construir o Retrofit aqui.
        RetrofitClient.apiService.login(LoginRequest(email, password))
            .enqueue(object : Callback<AuthResponse> {
                override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                    if (response.isSuccessful && response.body()?.sucesso == true) {

                        // 2. Pegando os dados que vieram do banco
                        val nomeRecebido = response.body()?.nome ?: "Usuário"
                        val idRecebido = response.body()?.userId ?: -1 // <-- IMPORTANTE: Pegando o ID!

                        Toast.makeText(this@LoginActivity, "Raio-x: id $idRecebido", Toast.LENGTH_LONG).show()

                        // 3. Preparando a viagem para a próxima tela
                        val intent = Intent(this@LoginActivity, MainScreenActivity::class.java)

                        // 4. Colocando na mochila!
                        intent.putExtra("NOME_USUARIO", nomeRecebido)
                        intent.putExtra("USER_ID", idRecebido) // <-- Passando o ID pra frente!

                        // Limpa a pilha para não voltar pro login
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Usuário ou senha inválidos", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "Erro de conexão: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}