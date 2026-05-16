package com.example.salvo

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.salvo.adapter.ServiceAdapter
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

    // Mantemos uma referência única para o Adapter para evitar que a lista "pisque" ou suma
    private var serviceAdapter: ServiceAdapter? = null
    private var providerId: Int = -1

    // Cache principal: contém TODOS os serviços (Ativos e Inativos)
    private var listaCompletaServicos: MutableList<ServiceItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cardapio_servicos)

        // 1. Recuperação do ID com redundância
        providerId = intent.getIntExtra("USER_ID", -1)
        if (providerId == -1) providerId = intent.getIntExtra("ID", -1)

        if (providerId == -1) {
            Toast.makeText(this, "Erro ao identificar oficina", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 2. Inicialização da UI
        rvServicos = findViewById(R.id.rv_servicos)
        rvServicos.layoutManager = LinearLayoutManager(this)
        tabLayout = findViewById(R.id.tab_layout_servicos)

        findViewById<ImageView>(R.id.btn_voltar).setOnClickListener { finish() }
        findViewById<FloatingActionButton>(R.id.fab_adicionar_servico).setOnClickListener {
            abrirDialogoCadastroServico(null)
        }

        // 3. Inicializa o Adapter uma única vez (vazio)
        configurarAdapter()

        // 4. Configura as abas
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

    /**
     * Filtra a lista mestre apenas pelo tipo de preço.
     * ITENS INATIVOS CONTINUAM APARECENDO.
     */
    private fun filtrarEExibirServicos(position: Int) {
        if (listaCompletaServicos.isEmpty()) {
            serviceAdapter?.updateList(emptyList())
            return
        }

        val listaFiltrada = if (position == 0) {
            // Preço Fixo
            listaCompletaServicos.filter { it.pricePerKm < 0.1 }
        } else {
            // Por KM/Hora
            listaCompletaServicos.filter { it.pricePerKm >= 0.1 }
        }

        // Apenas enviamos a nova lista para o adapter já existente
        serviceAdapter?.updateList(listaFiltrada)
    }

    // --- COMUNICAÇÃO COM O SERVIDOR ---

    private fun carregarTodosServicos() {
        RetrofitClient.apiService.obterServicos(providerId).enqueue(object : Callback<List<ServiceItem>> {
            override fun onResponse(call: Call<List<ServiceItem>>, response: Response<List<ServiceItem>>) {
                if (response.isSuccessful) {
                    listaCompletaServicos = response.body()?.toMutableList() ?: mutableListOf()
                    
                    Log.e("DEBUG_SALVO", "=== CARREGAMENTO DA API ===")
                    Log.e("DEBUG_SALVO", "Total de serviços retornados pelo servidor: ${listaCompletaServicos.size}")
                    listaCompletaServicos.forEach { 
                        Log.e("DEBUG_SALVO", "-> Nome: ${it.serviceType} | Preço KM: ${it.pricePerKm} | Ativo: ${it.isActive}")
                    }
                    Log.e("DEBUG_SALVO", "===========================")

                    filtrarEExibirServicos(tabLayout.selectedTabPosition)
                }
            }
            override fun onFailure(call: Call<List<ServiceItem>>, t: Throwable) {
                Log.e("API", "Falha ao carregar serviços")
            }
        })
    }

    private fun atualizarStatusServicoNaApi(servicoId: Int, isAtivo: Boolean) {
        // AJUSTE DE SINCRONIA: Atualiza o cache local IMEDIATAMENTE
        // para que o item não suma ao mudar de aba
        listaCompletaServicos.find { it.id == servicoId }?.isActive = isAtivo

        val dados = mapOf(
            "provider_id" to providerId.toString(),
            "is_active" to isAtivo.toString()
        )

        RetrofitClient.apiService.alternarStatusServico(servicoId, dados).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (!response.isSuccessful) {
                    Log.e("DEBUG_API", "Erro no servidor ao alterar status: HTTP ${response.code()}")
                    Toast.makeText(this@CardapioServicosActivity, "Erro ao salvar status", Toast.LENGTH_SHORT).show()
                    carregarTodosServicos() // Reverte visualmente se falhar no banco
                }
            }
            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                Log.e("DEBUG_API", "Falha na requisição de alterar status (Retrofit/Gson): ${t.message}", t)
                carregarTodosServicos()
            }
        })
    }

    private fun deletarServicoNaApi(servicoId: Int) {
        RetrofitClient.apiService.excluirServico(servicoId, providerId).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful) {
                    carregarTodosServicos()
                }
            }
            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {}
        })
    }

    // --- FORMULÁRIOS E DIÁLOGOS ---

    private fun abrirMenuAcoesServico(servico: ServiceItem) {
        val opcoes = arrayOf("Editar Valores / Nome", "Excluir Serviço")
        AlertDialog.Builder(this)
            .setTitle(servico.serviceType)
            .setItems(opcoes) { _, which ->
                when (which) {
                    0 -> abrirDialogoCadastroServico(servico)
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

    private fun abrirDialogoCadastroServico(servicoAtual: ServiceItem?) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_dialog_add_servico, null)
        dialog.setContentView(view)

        val etNome = view.findViewById<TextInputEditText>(R.id.et_nome_servico)
        val etPrecoBase = view.findViewById<TextInputEditText>(R.id.et_preco_base)
        val etPrecoKm = view.findViewById<TextInputEditText>(R.id.et_preco_km)
        val btnSalvar = view.findViewById<Button>(R.id.btn_salvar_servico)

        servicoAtual?.let {
            etNome.setText(it.serviceType)
            etPrecoBase.setText(it.basePrice.toString())
            etPrecoKm.setText(it.pricePerKm.toString())
            btnSalvar.text = "SALVAR ALTERAÇÕES"
        }

        btnSalvar.setOnClickListener {
            val nome = etNome.text.toString().trim()
            val precoBaseStr = etPrecoBase.text.toString().trim().replace(",", ".")
            val precoKmStr = etPrecoKm.text.toString().trim().replace(",", ".")

            if (nome.isNotEmpty() && precoBaseStr.isNotEmpty()) {
                val precoBase = precoBaseStr.toDoubleOrNull() ?: 0.0
                val precoKm = precoKmStr.toDoubleOrNull() ?: 0.0

                val dados = mapOf(
                    "provider_id" to providerId.toString(),
                    "service_type" to nome,
                    "base_price" to precoBase.toString(),
                    "price_per_km" to precoKm.toString()
                )

                if (servicoAtual == null) enviarNovoServicoParaApi(dados, dialog)
                else atualizarServicoExistenteNaApi(servicoAtual.id, dados, dialog)
            }
        }
        dialog.show()
    }

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