package com.example.salvo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
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
import com.example.salvo.model.VeiculoRequest
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
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

        tvGeralVeiculo.text = veiculo.vehicleType ?: "Não Definido"
        tvGeralPlaca.text = veiculo.plate.uppercase()

        tvStatusOperacional.text = veiculo.status
        when (veiculo.status.lowercase()) {
            "disponível" -> tvStatusOperacional.setTextColor(Color.parseColor("#10B981"))
            "em atendimento" -> tvStatusOperacional.setTextColor(Color.parseColor("#F59E0B"))
            else -> tvStatusOperacional.setTextColor(Color.parseColor("#EF4444"))
        }

        tvDataManutencao.text = veiculo.maintenanceDate ?: "---"
        tvStatusManutencao.text = if (veiculo.maintenanceDate.isNullOrEmpty()) "Sem data" else "Agendada"
        tvStatusManutencao.setTextColor(Color.parseColor("#F59E0B"))
    }

    private fun setupClickListenersDeGestao() {
        // --- 1. ABRIR NOVO POP-UP DE STATUS OPERACIONAL ---
        findViewById<MaterialCardView>(R.id.card_status_operacional_click)?.setOnClickListener {
            veiculoCarregado?.let { veiculo ->
                abrirBottomSheetStatus(veiculo)
            }
        }

        // --- 2. ABRIR DIALOG DE MANUTENÇÃO ---
        findViewById<MaterialCardView>(R.id.card_manutencao_click)?.setOnClickListener {
            veiculoCarregado?.let { veiculo ->
                val input = TextInputEditText(this).apply {
                    hint = "DD/MM/AAAA"
                    setText(veiculo.maintenanceDate ?: "")
                }
                AlertDialog.Builder(this, R.style.Theme_Salvo)
                    .setTitle("Atualizar Data de Manutenção")
                    .setView(input)
                    .setPositiveButton("Salvar") { _, _ ->
                        val novaData = input.text.toString().trim()
                        val dados = VeiculoRequest(
                            id = veiculo.id,
                            providerId = userIdLogado,
                            name = veiculo.name,
                            plate = veiculo.plate,
                            brand = veiculo.brand,
                            vehicleType = veiculo.vehicleType,
                            maintenanceDate = novaData,
                            status = veiculo.status,
                            vehiclePhoto = veiculo.vehiclePhoto
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

    // --- NOVA FUNÇÃO: BOTTOM SHEET DE STATUS ---
    private fun abrirBottomSheetStatus(veiculo: Vehicle) {
        val dialog = BottomSheetDialog(this, R.style.Theme_Salvo)
        val view = layoutInflater.inflate(R.layout.layout_dialog_status_veiculo, null)
        dialog.setContentView(view)

        val btnDisponivel = view.findViewById<MaterialButton>(R.id.btn_status_disponivel)
        val btnAtendimento = view.findViewById<MaterialButton>(R.id.btn_status_atendimento)
        val btnManutencao = view.findViewById<MaterialButton>(R.id.btn_status_manutencao)

        val clickListener = View.OnClickListener { v ->
            val novoStatus = when (v.id) {
                R.id.btn_status_disponivel -> "Disponível"
                R.id.btn_status_atendimento -> "Em atendimento"
                else -> "Em manutenção" // Cobre o botão de "Indisponível"
            }
            dialog.dismiss() // Esconde o pop-up
            atualizarStatusNaAPI(veiculo, novoStatus) // Envia para o backend
        }

        btnDisponivel.setOnClickListener(clickListener)
        btnAtendimento.setOnClickListener(clickListener)
        btnManutencao.setOnClickListener(clickListener)

        dialog.show()
    }

    private fun atualizarStatusNaAPI(veiculo: Vehicle, novoStatus: String) {
        val dados = mapOf("provider_id" to userIdLogado.toString(), "status" to novoStatus)
        RetrofitClient.apiService.atualizarStatusVeiculo(veiculo.id, dados).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful) carregarDadosDoVeiculo()
            }
            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                Toast.makeText(this@StatusVeiculoActivity, "Erro ao mudar status", Toast.LENGTH_SHORT).show()
            }
        })
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