package com.example.salvo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.salvo.adapter.PedidosOficinaAdapter
import com.example.salvo.model.ServiceRequest
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MeusPedidosOficinaActivity : AppCompatActivity() {

    private lateinit var rvPedidos: RecyclerView
    private lateinit var tabLayout: TabLayout
    private lateinit var adapter: PedidosOficinaAdapter

    private var providerId = -1
    private var todosPedidos: List<ServiceRequest> = emptyList() // Guarda tudo da API

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meus_pedidos_oficina)

        providerId = intent.getIntExtra("USER_ID", -1)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_pedidos_oficina)
        toolbar.setNavigationOnClickListener { finish() }

        tabLayout = findViewById(R.id.tab_layout_pedidos_oficina)
        rvPedidos = findViewById(R.id.rv_pedidos_oficina)
        rvPedidos.layoutManager = LinearLayoutManager(this)

        adapter = PedidosOficinaAdapter(emptyList()) { pedidoClicado ->
            abrirDetalhesDoPedido(pedidoClicado)
        }
        rvPedidos.adapter = adapter

        // Controla o clique nas abas
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                filtrarListaPelaAba(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        carregarHistorico()
    }

    private fun carregarHistorico() {
        RetrofitClient.apiService.obterHistoricoOficina(providerId).enqueue(object : Callback<List<ServiceRequest>> {
            override fun onResponse(call: Call<List<ServiceRequest>>, response: Response<List<ServiceRequest>>) {
                if (response.isSuccessful && response.body() != null) {
                    todosPedidos = response.body()!!

                    // Inicia mostrando os "Ativos" (Aba 0)
                    filtrarListaPelaAba(tabLayout.selectedTabPosition)
                } else {
                    Toast.makeText(this@MeusPedidosOficinaActivity, "Erro ao carregar pedidos", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<ServiceRequest>>, t: Throwable) {
                Toast.makeText(this@MeusPedidosOficinaActivity, "Falha de conexão", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filtrarListaPelaAba(posicaoAba: Int) {
        val listaFiltrada = when (posicaoAba) {
            // Aba 0: Ativos (A Caminho, No Local, Em Andamento, Confirmados)
            0 -> todosPedidos.filter {
                it.status == "accepted" || it.status == "en_route" ||
                        it.status == "arrived" || it.status == "in_progress"
            }
            // Aba 1: Histórico (Concluídos ou Cancelados)
            1 -> todosPedidos.filter {
                it.status == "completed" || it.status == "canceled"
            }
            else -> emptyList()
        }

        adapter.atualizarLista(listaFiltrada)
    }

    private fun abrirDetalhesDoPedido(pedido: ServiceRequest) {
        val intent = Intent(this, DetalhesPedidoOficinaActivity::class.java)

        intent.putExtra("NOME_CLIENTE", pedido.clienteNome ?: "Cliente Padrão")
        intent.putExtra("VEICULO_CLIENTE", pedido.vehicleInfo)
        intent.putExtra("DESC", pedido.description)
        intent.putExtra("DESTINO", pedido.destinoAddress ?: "Não informado")
        intent.putExtra("STATUS", pedido.getStatusTraduzido())
        intent.putExtra("DATA", pedido.createdAt)
        intent.putExtra("ORIGEM_LAT", pedido.latitude?: 0.0)
        intent.putExtra("ORIGEM_LNG", pedido.longitude?: 0.0)

        startActivity(intent)
    }
}