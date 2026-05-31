package com.example.salvo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.salvo.model.AuthResponse
import com.example.salvo.model.LoginRequest
import com.example.salvo.utils.SessionManager
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

        val sessionManager = SessionManager(this)
        val idGuardado = sessionManager.buscarUserId()

        if (idGuardado != -1) {
            val roleGuardada = sessionManager.buscarUserRole()
            val nomeGuardado = sessionManager.buscarUserNome()

            val intent = if (roleGuardada.equals("provider", ignoreCase = true)) {
                Intent(this, HomePrestadorActivity::class.java)
            } else {
                Intent(this, MainScreenActivity::class.java)
            }

            intent.putExtra("USER_ID", idGuardado)
            intent.putExtra("NOME_USUARIO", nomeGuardado)
            startActivity(intent)
            finish()
            return // Impede o restante da tela de Login de carregar
        }

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

        RetrofitClient.apiService.login(LoginRequest(email, password))
            .enqueue(object : Callback<AuthResponse> {
                override fun onResponse(
                    call: Call<AuthResponse>,
                    response: Response<AuthResponse>
                ) {
                    if (response.isSuccessful && response.body()?.sucesso == true) {

                        // 1. Pegando os dados vindos do banco
                        val nomeRecebido = response.body()?.nome ?: "Usuário"
                        val idRecebido = response.body()?.userId ?: -1
                        val roleRecebida = response.body()?.role ?: "customer" // Assume customer se vier nulo

                        Log.d("DEBUG_SALVO", "Login OK! Nome: $nomeRecebido, ID: $idRecebido, ROLE: $roleRecebida")

                        // 🔥 2. SALVANDO A SESSÃO PARA NÃO PRECISAR LOGAR AMANHÃ
                        val sessionManager = SessionManager(this@LoginActivity)
                        sessionManager.salvarSessao(idRecebido, roleRecebida, nomeRecebido)

                        Toast.makeText(
                            this@LoginActivity,
                            "Bem-vindo(a), $nomeRecebido!",
                            Toast.LENGTH_SHORT
                        ).show()

                        // 3. Direcionamento exato com base no seu banco de dados
                        val intent = if (roleRecebida.equals("provider", ignoreCase = true)) {
                            // É prestador -> Vai para o Radar/Dashboard
                            Intent(this@LoginActivity, HomePrestadorActivity::class.java)
                        } else {
                            // É cliente (customer) -> Vai para a tela de Pedir Socorro
                            Intent(this@LoginActivity, MainScreenActivity::class.java)
                        }

                        // 4. Coloca os dados na mochila (ambas as telas precisam do ID e Nome)
                        intent.putExtra("NOME_USUARIO", nomeRecebido)
                        intent.putExtra("USER_ID", idRecebido)

                        // 5. Trava o botão de "voltar" do celular para não retornar à tela de login
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                        startActivity(intent)
                        finish()

                    } else {
                        val errorMsg = response.body()?.message ?: "Usuário ou senha inválidos"
                        Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Erro de conexão: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}