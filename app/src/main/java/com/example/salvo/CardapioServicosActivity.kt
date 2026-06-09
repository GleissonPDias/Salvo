package com.example.salvo

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.salvo.adapter.ServiceAdapter
import com.example.salvo.dialog.ServicePriceModeDialog
import com.example.salvo.model.AuthResponse
import com.example.salvo.model.ServiceItem
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CardapioServicosActivity : AppCompatActivity() {

    private lateinit var rvServicos: RecyclerView
    private lateinit var tabLayout: TabLayout

    private var serviceAdapter: ServiceAdapter? = null
    private var providerId: Int = -1

    private var listaCompletaServicos: MutableList<ServiceItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cardapio_servicos)

        providerId = intent.getIntExtra("USER_ID", -1)
        if (providerId == -1) providerId = intent.getIntExtra("ID", -1)

        if (providerId == -1) {
            Toast.makeText(this, "Erro ao identificar oficina", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        rvServicos = findViewById(R.id.rv_servicos)
        rvServicos.layoutManager = LinearLayoutManager(this)
        tabLayout = findViewById(R.id.tab_layout_servicos)

        findViewById<ImageView>(R.id.btn_voltar).setOnClickListener { finish() }

        // NOVO FLUXO: Ao invés de abrir o form direto, abre o seletor de tipo de preço!
        findViewById<FloatingActionButton>(R.id.fab_adicionar_servico).setOnClickListener {
            iniciarFluxoAdicionarServico()
        }

        configurarAdapter()
        configurarOuvinteDasAbas()
    }

    override fun onResume() {
        super.onResume()
        carregarTodosServicos()
    }

    private fun configurarAdapter() {
        serviceAdapter = ServiceAdapter(
            services = mutableListOf(),
            onStatusChanged = { servico, novoStatus ->
                atualizarStatusServicoNaApi(servico.id, novoStatus)
            },
            onServiceClicked = { servico ->
                abrirMenuAcoesServico(servico)
            }
        )
        rvServicos.adapter = serviceAdapter
    }

    private fun configurarOuvinteDasAbas() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let { filtrarEExibirServicos(it.position) }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun filtrarEExibirServicos(position: Int) {
        if (listaCompletaServicos.isEmpty()) {
            serviceAdapter?.updateList(emptyList())
            return
        }

        val listaFiltrada = if (position == 0) {
            listaCompletaServicos.filter { it.pricePerKm < 0.1 } // Fixo
        } else {
            listaCompletaServicos.filter { it.pricePerKm >= 0.1 } // KM
        }
        serviceAdapter?.updateList(listaFiltrada)
    }

    // --- COMUNICAÇÃO COM O SERVIDOR ---

    private fun carregarTodosServicos() {
        RetrofitClient.apiService.obterServicos(providerId).enqueue(object : Callback<List<ServiceItem>> {
            override fun onResponse(call: Call<List<ServiceItem>>, response: Response<List<ServiceItem>>) {
                if (response.isSuccessful) {
                    listaCompletaServicos = response.body()?.toMutableList() ?: mutableListOf()
                    filtrarEExibirServicos(tabLayout.selectedTabPosition)
                }
            }
            override fun onFailure(call: Call<List<ServiceItem>>, t: Throwable) {
                Log.e("API", "Falha ao carregar serviços")
            }
        })
    }

    private fun atualizarStatusServicoNaApi(servicoId: Int, isAtivo: Boolean) {
        listaCompletaServicos.find { it.id == servicoId }?.isActive = isAtivo
        val dados = mapOf("provider_id" to providerId.toString(), "is_active" to isAtivo.toString())

        RetrofitClient.apiService.alternarStatusServico(servicoId, dados).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (!response.isSuccessful) {
                    Toast.makeText(this@CardapioServicosActivity, "Erro ao salvar status", Toast.LENGTH_SHORT).show()
                    carregarTodosServicos()
                }
            }
            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                carregarTodosServicos()
            }
        })
    }

    private fun deletarServicoNaApi(servicoId: Int) {
        RetrofitClient.apiService.excluirServico(servicoId, providerId).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful) carregarTodosServicos()
            }
            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {}
        })
    }

    // --- NOVO FLUXO DE DIÁLOGOS DE SERVIÇO ---

    private fun iniciarFluxoAdicionarServico() {
        val dialog = ServicePriceModeDialog(this) { mode ->
            if (mode == "fixed") {
                abrirDialogoPrecoFixo(null)
            } else {
                abrirDialogoPrecoKm(null)
            }
        }
        dialog.show()
    }

    private fun abrirMenuAcoesServico(servico: ServiceItem) {
        val opcoes = arrayOf("Editar Valores / Nome", "Excluir Serviço")
        AlertDialog.Builder(this)
            .setTitle(servico.serviceType)
            .setItems(opcoes) { _, which ->
                when (which) {
                    0 -> {
                        // Decide qual diálogo abrir na edição com base no preço por KM
                        if (servico.pricePerKm > 0.1) abrirDialogoPrecoKm(servico)
                        else abrirDialogoPrecoFixo(servico)
                    }
                    1 -> confirmarExclusao(servico)
                }
            }
            .show()
    }

    private fun confirmarExclusao(servico: ServiceItem) {
        AlertDialog.Builder(this)
            .setTitle("Excluir serviço?")
            .setMessage("Deseja realmente remover ${servico.serviceType}?")
            .setPositiveButton("Excluir") { _, _ -> deletarServicoNaApi(servico.id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // DIÁLOGO: PREÇO FIXO
    private fun abrirDialogoPrecoFixo(servicoAtual: ServiceItem?) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_add_servico_fixed_price, null)
        dialog.setContentView(view)

        val etNome = view.findViewById<TextInputEditText>(R.id.et_nome_servico_fixo)
        val etPrecoFixo = view.findViewById<TextInputEditText>(R.id.et_preco_fixo)
        val btnCancelar = view.findViewById<Button>(R.id.btn_cancelar_fixo)
        val btnSalvar = view.findViewById<Button>(R.id.btn_salvar_fixo)

        servicoAtual?.let {
            etNome.setText(it.serviceType)
            etPrecoFixo.setText(it.basePrice.toString())
            btnSalvar.text = "SALVAR ALTERAÇÕES"
        }

        btnCancelar.setOnClickListener { dialog.dismiss() }

        btnSalvar.setOnClickListener {
            val nome = etNome.text.toString().trim()
            val precoFixoStr = etPrecoFixo.text.toString().trim().replace(",", ".")

            if (nome.isNotEmpty() && precoFixoStr.isNotEmpty()) {
                val precoFixo = precoFixoStr.toDoubleOrNull() ?: 0.0
                val dados = mapOf(
                    "provider_id" to providerId.toString(),
                    "service_type" to nome,
                    "base_price" to precoFixo.toString(),
                    "price_per_km" to "0.0" // Fixo não tem KM
                )

                if (servicoAtual == null) enviarNovoServicoParaApi(dados, dialog)
                else atualizarServicoExistenteNaApi(servicoAtual.id, dados, dialog)
            } else {
                Toast.makeText(this, "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    // DIÁLOGO: PREÇO POR KM
    private fun abrirDialogoPrecoKm(servicoAtual: ServiceItem?) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_add_servico_per_km, null)
        dialog.setContentView(view)

        val etNome = view.findViewById<TextInputEditText>(R.id.et_nome_servico_km)
        val etPrecoKm = view.findViewById<TextInputEditText>(R.id.et_preco_km)
        val btnCancelar = view.findViewById<Button>(R.id.btn_cancelar_km)
        val btnSalvar = view.findViewById<Button>(R.id.btn_salvar_km)

        servicoAtual?.let {
            etNome.setText(it.serviceType)
            etPrecoKm.setText(it.pricePerKm.toString())
            btnSalvar.text = "SALVAR ALTERAÇÕES"
        }

        btnCancelar.setOnClickListener { dialog.dismiss() }

        btnSalvar.setOnClickListener {
            val nome = etNome.text.toString().trim()
            val precoKmStr = etPrecoKm.text.toString().trim().replace(",", ".")

            if (nome.isNotEmpty() && precoKmStr.isNotEmpty()) {
                val precoKm = precoKmStr.toDoubleOrNull() ?: 0.0

                // Envia "0.0" para o base_price já que a cobrança é exclusivamente por Km
                val dados = mapOf(
                    "provider_id" to providerId.toString(),
                    "service_type" to nome,
                    "base_price" to "0.0",
                    "price_per_km" to precoKm.toString()
                )

                if (servicoAtual == null) enviarNovoServicoParaApi(dados, dialog)
                else atualizarServicoExistenteNaApi(servicoAtual.id, dados, dialog)
            } else {
                Toast.makeText(this, "Preencha o nome e o valor por Km", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    // --- MÉTODOS DE ENVIO (MANTIDOS IGUAIS) ---

    private fun enviarNovoServicoParaApi(dados: Map<String, String>, dialog: BottomSheetDialog) {
        RetrofitClient.apiService.adicionarServico(dados).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful) {
                    dialog.dismiss()
                    carregarTodosServicos()
                }
            }
            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {}
        })
    }

    private fun atualizarServicoExistenteNaApi(servicoId: Int, dados: Map<String, String>, dialog: BottomSheetDialog) {
        RetrofitClient.apiService.atualizarServico(servicoId, dados).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful) {
                    dialog.dismiss()
                    carregarTodosServicos()
                }
            }
            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {}
        })
    }
}