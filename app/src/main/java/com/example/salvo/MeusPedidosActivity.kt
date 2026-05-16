package com.example.salvo

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.salvo.adapter.PedidosAdapter
import com.example.salvo.model.ServiceRequest
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MeusPedidosActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tabLayout: TabLayout
    private lateinit var adapter: PedidosAdapter

    // Guarda a lista completa que vem da API do Render
    private var todosPedidos: List<ServiceRequest> = emptyList()

    // ⚠️ ID do usuário: fixo para teste. No app real, você puxará isso do login (SharedPreferences)
    private var userIdLogado = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_meus_pedidos)

        userIdLogado = intent.getIntExtra("USER_ID", -1)

        if (userIdLogado == -1) {
            Toast.makeText(this, "Erro: Usuário não identificado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        configurarSistemaEWindowInsets()
        configurarToolbar()
        inicializarComponentes()
        buscarDadosDaApi()
    }

    private fun configurarSistemaEWindowInsets() {
        val mainLayout = findViewById<View>(R.id.main)
        mainLayout?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }
    }

    private fun configurarToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_pedidos)
        toolbar.setNavigationOnClickListener { finish() } // Fecha a tela ao clicar em voltar
    }

    private fun inicializarComponentes() {
        tabLayout = findViewById(R.id.tab_layout_pedidos)
        recyclerView = findViewById(R.id.rv_pedidos)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Inicializa o nosso adaptador oficial (vazio no começo) e configura o clique do botão
        adapter = PedidosAdapter(emptyList()) { pedidoClicado ->
            Toast.makeText(
                this,
                "Pedir novamente: ${pedidoClicado.serviceType}",
                Toast.LENGTH_SHORT
            ).show()
            // Futuro: Redirecionar para a tela de pedir socorro com os dados pré-preenchidos
        }
        recyclerView.adapter = adapter

        // Controla a troca de abas (Em Andamento / Concluídos)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                filtrarListaPelaAba(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun buscarDadosDaApi() {
        // Usa o seu Singleton oficial para chamar a rede
        val api = RetrofitClient.apiService

        api.listarPedidos(userIdLogado).enqueue(object : Callback<List<ServiceRequest>> {
            override fun onResponse(
                call: Call<List<ServiceRequest>>,
                response: Response<List<ServiceRequest>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    todosPedidos = response.body()!!

                    // Alimenta a lista da tela de acordo com a aba selecionada no momento
                    filtrarListaPelaAba(tabLayout.selectedTabPosition)
                } else {
                    Toast.makeText(this@MeusPedidosActivity, "Erro ao carregar lista", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<ServiceRequest>>, t: Throwable) {
                Toast.makeText(this@MeusPedidosActivity, "Sem conexão com o servidor", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Filtra a lista principal dependendo da aba clicada
     */
    private fun filtrarListaPelaAba(posicaoAba: Int) {
        val listaFiltrada = when (posicaoAba) {
            // Aba 0: Em Andamento
            0 -> todosPedidos.filter { it.status == "searching" || it.status == "in_progress" }

            // Aba 1: Concluídos ou Cancelados
            1 -> todosPedidos.filter { it.status == "completed" || it.status == "canceled" }

            else -> emptyList()
        }

        adapter.atualizarLista(listaFiltrada)
    }
}