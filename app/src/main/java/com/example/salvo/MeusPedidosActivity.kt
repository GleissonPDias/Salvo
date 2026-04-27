package com.example.salvo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

// O "Molde" do dado (como se fosse a tabela do banco)
data class Pedido(val servico: String, val status: String, val dataHora: String, val prestador: String, val preco: String)

class MeusPedidosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_meus_pedidos)

        // Ajuste da barra do sistema
        val mainLayout = findViewById<View>(R.id.main)
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        // Configurando a Toolbar (Setinha de voltar)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_pedidos)
        toolbar.setNavigationOnClickListener {
            finish() // Fecha a tela de pedidos e volta pra anterior
        }

        // Criando alguns dados de teste para vermos a tela funcionando
        val listaDePedidos = listOf(
            Pedido("S.O.S. Borracharia", "Concluído", "12 Mai 2026 • 14:30", "Carlos Borracharia", "R$ 45,00"),
            Pedido("S.O.S. Guincho", "Concluído", "10 Mai 2026 • 09:15", "Guinchos Rápidos", "R$ 120,00"),
            Pedido("S.O.S. Bateria", "Concluído", "01 Mai 2026 • 20:45", "Auto Elétrica Luz", "R$ 80,00")
        )

        // Vinculando a lista do XML (rv_pedidos) e colocando o adaptador nela
        val recyclerView = findViewById<RecyclerView>(R.id.rv_pedidos)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = PedidoAdapter(listaDePedidos)
    }
}

// =========================================================================
// O ADAPTADOR (Ele pega a lista e cria os cartões na tela)
// =========================================================================
class PedidoAdapter(private val pedidos: List<Pedido>) : RecyclerView.Adapter<PedidoAdapter.PedidoViewHolder>() {

    class PedidoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Encontra os campos dentro do item_pedido.xml
        val tvNomeServico: TextView = view.findViewById(R.id.tv_nome_servico)
        val tvStatus: TextView = view.findViewById(R.id.tv_status_pedido)
        val tvDataHora: TextView = view.findViewById(R.id.tv_data_hora)
        val tvPrestador: TextView = view.findViewById(R.id.tv_nome_prestador)
        val tvPreco: TextView = view.findViewById(R.id.tv_preco)
        val btnAcao: MaterialButton = view.findViewById(R.id.btn_acao_pedido)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        // Puxa o seu arquivo item_pedido.xml para servir de carimbo
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pedido, parent, false)
        return PedidoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        val pedidoAtual = pedidos[position]

        // Preenche o cartão com os dados da lista
        holder.tvNomeServico.text = pedidoAtual.servico
        holder.tvStatus.text = pedidoAtual.status
        holder.tvDataHora.text = pedidoAtual.dataHora
        holder.tvPrestador.text = pedidoAtual.prestador
        holder.tvPreco.text = pedidoAtual.preco

        holder.btnAcao.setOnClickListener {
            Toast.makeText(holder.itemView.context, "Repetindo: ${pedidoAtual.servico}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = pedidos.size
}