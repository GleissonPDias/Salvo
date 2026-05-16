package com.example.salvo.adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.salvo.R // Substitua pelo R do seu pacote principal
import com.example.salvo.model.ServiceRequest
import com.google.android.material.button.MaterialButton


class PedidosAdapter(
    private var listaPedidos: List<ServiceRequest>,
    private val onPedirNovamenteClick: (ServiceRequest) -> Unit
) : RecyclerView.Adapter<PedidosAdapter.PedidoViewHolder>() {

    // Cria a visualização inflando o seu XML lindo
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pedido, parent, false) // ou item_historico, dependendo de como salvou
        return PedidoViewHolder(view)
    }

    // Retorna a quantidade de itens na lista
    override fun getItemCount(): Int = listaPedidos.size

    // Preenche cada linha com os dados do banco
    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        val pedido = listaPedidos[position]
        holder.bind(pedido)
    }

    // Função para atualizar a lista quando vierem novos dados da API
    fun atualizarLista(novaLista: List<ServiceRequest>) {
        listaPedidos = novaLista
        notifyDataSetChanged()
    }

    // --- O VIEWHOLDER INTERNO ---
    inner class PedidoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcone: ImageView = itemView.findViewById(R.id.iv_icone_servico)
        private val tvNomeServico: TextView = itemView.findViewById(R.id.tv_nome_servico)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status_pedido)
        private val tvDataHora: TextView = itemView.findViewById(R.id.tv_data_hora)
        private val tvNomePrestador: TextView = itemView.findViewById(R.id.tv_nome_prestador)
        private val tvPreco: TextView = itemView.findViewById(R.id.tv_preco)
        private val btnAcao: MaterialButton = itemView.findViewById(R.id.btn_acao_pedido)

        fun bind(pedido: ServiceRequest) {
            tvNomeServico.text = pedido.serviceType
            tvStatus.text = pedido.getStatusTraduzido()
            tvDataHora.text = pedido.createdAt // ou formatar caso venha apenas o timestamp
            tvPreco.text = pedido.getPrecoFormatado()

            // Preenche o nome do prestador (se houver)
            tvNomePrestador.text = pedido.prestadorNome ?: "Buscando prestador..."

            // Altera a cor do status dinamicamente (Opcional, mas dá um toque sênior)
            val context = itemView.context
            when (pedido.status.lowercase()) {
                "completed" -> tvStatus.setTextColor(context.getColor(R.color.salvo_laranja)) // ou verde
                "canceled" -> tvStatus.setTextColor(context.getColor(android.R.color.holo_red_dark))
                else -> tvStatus.setTextColor(context.getColor(R.color.salvo_azul_neon))
            }

            // Ação do botão "Pedir Novamente"
            btnAcao.setOnClickListener {
                onPedirNovamenteClick(pedido)
            }
        }
    }
}