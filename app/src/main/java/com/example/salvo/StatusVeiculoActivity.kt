package com.example.salvo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.salvo.model.AuthResponse
import com.example.salvo.model.Vehicle
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StatusVeiculoActivity : AppCompatActivity() {

    private var userIdLogado = -1
    private var veiculoId = -1
    private var veiculoCarregado: Vehicle? = null

    // Componentes mapeados do XML
    private lateinit var tvModelo: TextView
    private lateinit var tvMarca: TextView
    private lateinit var tvPlaca: TextView
    private lateinit var tvGeralVeiculo: TextView
    private lateinit var tvGeralPlaca: TextView
    private lateinit var tvStatusOperacional: TextView
    private lateinit var tvDataManutencao: TextView
    private lateinit var tvStatusManutencao: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_status_veiculo)

        userIdLogado = intent.getIntExtra("USER_ID", -1)
        veiculoId = intent.getIntExtra("VEICULO_ID", -1)

        if (userIdLogado == -1 || veiculoId == -1) {
            Toast.makeText(this, "Erro ao identificar veículo", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        inicializarComponentes()
        configurarSistemaEWindowInsets()
        configurarBotaoVoltar()
        configurarBottomNavigation()
        setupClickListenersDeGestao()

        carregarDadosDoVeiculo()
    }

    private fun inicializarComponentes() {
        // IDs 100% sincronizados com o XML
        tvModelo = findViewById(R.id.tv_get_modelo)
        tvMarca = findViewById(R.id.tv_detalhe_marca) ?: tvModelo
        tvPlaca = findViewById(R.id.tv_detalhe_placa)
        tvGeralVeiculo = findViewById(R.id.tv_geral_veiculo) ?: tvModelo
        tvGeralPlaca = findViewById(R.id.tv_geral_placa) ?: tvPlaca
        tvStatusOperacional = findViewById(R.id.tv_detalhe_status_operacional)
        tvDataManutencao = findViewById(R.id.tv_detalhe_data_manutencao) ?: tvModelo
        tvStatusManutencao = findViewById(R.id.tv_detalhe_status_manutencao) ?: tvModelo
    }

    private fun carregarDadosDoVeiculo() {
        RetrofitClient.apiService.obterVeiculos(userIdLogado).enqueue(object : Callback<List<Vehicle>> {
            override fun onResponse(call: Call<List<Vehicle>>, response: Response<List<Vehicle>>) {
                if (response.isSuccessful) {
                    val lista = response.body() ?: emptyList()
                    veiculoCarregado = lista.find { it.id == veiculoId }
                    veiculoCarregado?.let { preencherDadosNaTela(it) }
                }
            }
            override fun onFailure(call: Call<List<Vehicle>>, t: Throwable) {}
        })
    }

    private fun preencherDadosNaTela(veiculo: Vehicle) {
        tvModelo.text = veiculo.name.uppercase()
        tvMarca.text = veiculo.brand ?: "Não Informada"
        tvPlaca.text = veiculo.plate.uppercase()

        tvGeralVeiculo.text = veiculo.vehicle_type ?: "Não Definido"
        tvGeralPlaca.text = veiculo.plate.uppercase()

        tvStatusOperacional.text = veiculo.status
        when (veiculo.status.lowercase()) {
            "disponível" -> tvStatusOperacional.setTextColor(Color.parseColor("#10B981"))
            "em atendimento" -> tvStatusOperacional.setTextColor(Color.parseColor("#F59E0B"))
            else -> tvStatusOperacional.setTextColor(Color.parseColor("#EF4444"))
        }

        tvDataManutencao.text = veiculo.maintenance_date ?: "---"
        tvStatusManutencao.text = if (veiculo.maintenance_date.isNullOrEmpty()) "Sem data" else "Agendada"
        tvStatusManutencao.setTextColor(Color.parseColor("#F59E0B"))
    }

    private fun setupClickListenersDeGestao() {
        findViewById<MaterialCardView>(R.id.card_status_operacional_click)?.setOnClickListener {
            veiculoCarregado?.let { veiculo ->
                val opcoes = arrayOf("Disponível", "Em atendimento", "Em manutenção")
                AlertDialog.Builder(this, R.style.Theme_Salvo)
                    .setTitle("Alterar Status Operacional")
                    .setItems(opcoes) { _, which ->
                        val novoStatus = opcoes[which]
                        val dados = mapOf("provider_id" to userIdLogado.toString(), "status" to novoStatus)
                        RetrofitClient.apiService.atualizarStatusVeiculo(veiculo.id, dados).enqueue(object : Callback<AuthResponse> {
                            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                                if (response.isSuccessful) carregarDadosDoVeiculo()
                            }
                            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {}
                        })
                    }.show()
            }
        }

        findViewById<MaterialCardView>(R.id.card_manutencao_click)?.setOnClickListener {
            veiculoCarregado?.let { veiculo ->
                val input = TextInputEditText(this).apply {
                    hint = "DD/MM/AAAA"
                    setText(veiculo.maintenance_date ?: "")
                }
                AlertDialog.Builder(this, R.style.Theme_Salvo)
                    .setTitle("Atualizar Data de Manutenção")
                    .setView(input)
                    .setPositiveButton("Salvar") { _, _ ->
                        val novaData = input.text.toString().trim()

                        // AQUI ESTÁ A CORREÇÃO DE PRECEDÊNCIA (Uso dos parênteses)
                        val dados = mapOf(
                            "provider_id" to userIdLogado.toString(),
                            "name" to veiculo.name,
                            "plate" to veiculo.plate,
                            "brand" to (veiculo.brand ?: ""),
                            "vehicle_type" to (veiculo.vehicle_type ?: ""),
                            "maintenance_date" to novaData
                        )

                        RetrofitClient.apiService.atualizarVeiculoCompleto(veiculo.id, dados).enqueue(object : Callback<AuthResponse> {
                            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                                if (response.isSuccessful) carregarDadosDoVeiculo()
                            }
                            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {}
                        })
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
    }

    private fun configurarSistemaEWindowInsets() {
        val mainLayout = findViewById<android.view.View>(android.R.id.content)
        mainLayout?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }
    }

    private fun configurarBotaoVoltar() {
        findViewById<ImageView>(R.id.btn_voltar)?.setOnClickListener { finish() }
    }

    private fun configurarBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.selectedItemId = R.id.nav_frota
        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_radar -> {
                    startActivity(Intent(this, HomePrestadorActivity::class.java).apply { putExtra("USER_ID", userIdLogado) })
                    finish()
                    true
                }
                R.id.nav_servicos -> {
                    startActivity(Intent(this, CardapioServicosActivity::class.java).apply { putExtra("USER_ID", userIdLogado) })
                    finish()
                    true
                }
                R.id.nav_frota -> true
                else -> false
            }
        }
    }
}