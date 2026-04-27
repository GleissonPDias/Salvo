package com.example.salvo

import android.content.Intent
import android.util.Log
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.example.salvo.model.AuthResponse
import com.example.salvo.model.RegisterRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    private lateinit var nameField: EditText
    private lateinit var emailField: EditText
    private lateinit var cpfField: EditText
    private lateinit var phoneField: EditText
    private lateinit var passField: EditText
    private lateinit var confirmPassField: EditText
    private lateinit var registerButton: Button
    private lateinit var loginScreen: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        val mainLayout = findViewById<android.view.View>(R.id.main)
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        // Referências
        nameField = findViewById(R.id.reg_name)
        emailField = findViewById(R.id.reg_email)
        cpfField = findViewById(R.id.reg_cpf)
        phoneField = findViewById(R.id.reg_telephone)
        passField = findViewById(R.id.reg_password)
        confirmPassField = findViewById(R.id.reg_confirm_password)
        registerButton = findViewById(R.id.btn_register)
        loginScreen = findViewById(R.id.text_login)

        val roleEscolhida = intent.getStringExtra("role") ?: "customer"

        val mensagens = arrayOf(
            "Preencha todos os campos",
            "As senhas não coincidem"
        )

        registerButton.setOnClickListener {
            val name_value = nameField.text.toString().trim()
            val email_value = emailField.text.toString().trim()
            val cpf_value = cpfField.text.toString().trim()
            val telephone_value = phoneField.text.toString().trim()
            val pass_value = passField.text.toString()
            val pass_confirm = confirmPassField.text.toString()

            if (name_value.isEmpty() || email_value.isEmpty() || cpf_value.isEmpty() ||
                telephone_value.isEmpty() || pass_value.isEmpty() || pass_confirm.isEmpty()
            ) {
                mostrarSnackbar(mensagens[0], Color.RED)
            } else if (pass_value != pass_confirm) {
                mostrarSnackbar(mensagens[1], Color.RED)
            } else {
                cadastrarNaAPI(name_value, email_value, cpf_value, telephone_value, pass_value, roleEscolhida)
            }
        }

        loginScreen.setOnClickListener {
            finish()
        }
    }

    private fun cadastrarNaAPI(nome: String, email: String, cpf: String, telefone: String, senha: String, role: String) {

        val request = RegisterRequest(
            nome = nome,
            email = email,
            cpf = cpf,
            telefone = telefone,
            password = senha,
            role = role,
            latitude = 0.0,
            longitude = 0.0
        )

        // Usando nosso telefone global! Muito mais simples.
        RetrofitClient.apiService.cadastrar(request).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful && response.body()?.sucesso == true) {
                    mostrarSnackbar("Cadastro realizado com sucesso!", Color.GREEN)

                    // Fecha a tela depois de 2 segundos e volta pro Login
                    Handler(Looper.getMainLooper()).postDelayed({
                        val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }, 2000)
                } else {
                    val erroJsonCru = response.errorBody()?.string() ?: "Sem detalhes"
                    Log.e("ERRO_API", "Motivo da falha no cadastro: $erroJsonCru")
                    mostrarSnackbar("Falha no cadastro. Verifique os logs.", Color.RED)
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                mostrarSnackbar("Sem conexão com o servidor: ${t.message}", Color.RED)
            }
        })
    }

    private fun mostrarSnackbar(mensagem: String, corTexto: Int) {
        val rootView = findViewById<android.view.View>(android.R.id.content)
        val snack = Snackbar.make(rootView, mensagem, Snackbar.LENGTH_SHORT)
        snack.setTextColor(corTexto)
        snack.setBackgroundTint(Color.BLACK)
        snack.show()
    }
}