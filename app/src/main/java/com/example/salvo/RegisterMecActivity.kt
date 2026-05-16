package com.example.salvo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.salvo.model.AuthResponse
import com.example.salvo.model.RegisterRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterMecActivity : AppCompatActivity() {

    private lateinit var emailField: EditText
    private lateinit var passField: EditText
    private lateinit var confirmPassField: EditText
    private lateinit var razaoField: EditText
    private lateinit var cnpjField: EditText
    private lateinit var phoneField: EditText
    private lateinit var registerButton: Button

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            validarEEnviar()
        } else {
            mostrarSnackbar("A permissão de GPS é obrigatória para o cadastro.", Color.YELLOW)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_mec)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mainLayout = findViewById<android.view.View>(R.id.main)
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        emailField = findViewById(R.id.reg_email)
        passField = findViewById(R.id.reg_password)
        confirmPassField = findViewById(R.id.reg_confirm_password)
        razaoField = findViewById(R.id.reg_razao)
        cnpjField = findViewById(R.id.reg_cnpj)
        phoneField = findViewById(R.id.reg_telephone)
        registerButton = findViewById(R.id.btn_register)

        registerButton.setOnClickListener {
            verificarPermissaoERegistrar()
        }
    }

    private fun verificarPermissaoERegistrar() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            validarEEnviar()
        } else {
            requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private fun validarEEnviar() {
        val razao = razaoField.text.toString().trim()
        val cnpj = cnpjField.text.toString().trim()
        val phone = phoneField.text.toString().trim()
        val email = emailField.text.toString().trim()
        val pass = passField.text.toString()
        val confirmPass = confirmPassField.text.toString()

        if (razao.isEmpty() || cnpj.isEmpty() || phone.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            mostrarSnackbar("Preencha todos os campos", Color.RED)
            return
        }

        if (pass != confirmPass) {
            mostrarSnackbar("As senhas não coincidem", Color.RED)
            return
        }

        pegarLocalizacaoECadastrar(razao, email, cnpj, phone, pass)
    }

    @SuppressLint("MissingPermission")
    private fun pegarLocalizacaoECadastrar(nome: String, email: String, cpf: String, telefone: String, senha: String) {
        mostrarSnackbar("Buscando sua localização exata...", Color.BLUE)

        // Tenta a última localização conhecida (mais rápido)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                enviarParaAPI(nome, email, cpf, telefone, senha, location.latitude, location.longitude)
            } else {
                // Se falhar, força uma busca em tempo real (mais preciso/robusto)
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { newLocation ->
                        if (newLocation != null) {
                            enviarParaAPI(nome, email, cpf, telefone, senha, newLocation.latitude, newLocation.longitude)
                        } else {
                            mostrarSnackbar("Erro ao obter GPS. Verifique se está ligado.", Color.RED)
                        }
                    }
            }
        }
    }

    private fun enviarParaAPI(nome: String, email: String, cpf: String, telefone: String, senha: String, lat: Double, lon: Double) {
        val role = intent.getStringExtra("role") ?: "provider"

        val request = RegisterRequest(
            nome = nome,
            email = email,
            cpf = cpf,
            telefone = telefone,
            password = senha,
            role = role,
            latitude = lat,
            longitude = lon
        )

        RetrofitClient.apiService.cadastrar(request).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful && response.body()?.sucesso == true) {
                    // CASO DE SUCESSO (200, 201...)
                    mostrarSnackbar("Oficina cadastrada com sucesso!", Color.GREEN)
                    Handler(Looper.getMainLooper()).postDelayed({
                        startActivity(Intent(this@RegisterMecActivity, LoginActivity::class.java))
                        finish()
                    }, 2000)
                } else {
                    // CASO DE ERRO (400, 500...)
                    try {
                        // 1. Pegamos o JSON bruto do erro
                        val errorJson = response.errorBody()?.string()

                        // 2. Convertemos esse JSON para o nosso objeto AuthResponse
                        val authResponse = com.google.gson.Gson().fromJson(errorJson, AuthResponse::class.java)

                        // 3. Pegamos a mensagem real (ex: "Erro ao cadastrar: Duplicate entry...")
                        val mensagemReal = authResponse?.message ?: "Erro desconhecido no servidor"

                        Log.e("ERRO_API", "Mensagem Real: $mensagemReal")
                        mostrarSnackbar(mensagemReal, Color.RED)

                    } catch (e: Exception) {
                        mostrarSnackbar("Falha na comunicação com o servidor.", Color.RED)
                    }
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                Log.e("ERRO_API", "Falha total: ${t.message}")
                mostrarSnackbar("Sem conexão: ${t.message}", Color.RED)
            }
        })
    }

    private fun mostrarSnackbar(mensagem: String, corTexto: Int) {
        val rootView = findViewById<android.view.View>(android.R.id.content)
        val snack = Snackbar.make(rootView, mensagem, Snackbar.LENGTH_LONG)
        snack.setTextColor(corTexto)
        snack.setBackgroundTint(Color.BLACK)
        snack.show()
    }
}