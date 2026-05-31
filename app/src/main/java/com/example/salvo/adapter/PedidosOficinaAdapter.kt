package com.example.salvo.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.salvo.R
import com.example.salvo.model.ServiceRequest

class PedidosOficinaAdapter(
    private var listaPedidos: List<ServiceRequest>,
    private val onPedidoClick: (ServiceRequest) -> Unit
) : RecyclerView.Adapter<PedidosOficinaAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pedido_oficina, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = listaPedidos.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listaPedidos[position])
    }

    fun atualizarLista(novaLista: List<ServiceRequest>) {
        listaPedidos = novaLista
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTipoServico: TextView = itemView.findViewById(R.id.tv_tipo_servico_oficina)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status_oficina)
        private val tvVeiculoCliente: TextView = itemView.findViewById(R.id.tv_veiculo_cliente)
        private val tvMeuGuincho: TextView = itemView.findViewById(R.id.tv_meu_guincho)
        private val tvDistancia: TextView = itemView.findViewById(R.id.tv_distancia_oficina)
        private val tvPreco: TextView = itemView.findViewById(R.id.tv_preco_oficina)

        fun bind(pedido: ServiceRequest) {
            tvTipoServico.text = pedido.serviceType

            // Veículo do Cliente (Ex: Honda Civic - ABC1234)
            tvVeiculoCliente.text = "Cliente: ${pedido.vehicleInfo}"

            // Guincho da Oficina que prestou o serviço
            val nomeGuincho = pedido.veiculoPrestadorNome ?: "Veículo não especificado"
            tvMeuGuincho.text = "Guincho: $nomeGuincho"

            // Distância e Preço
            tvDistancia.text = String.format("%.1f km", pedido.finalDistance ?: 0.0)
            tvPreco.text = String.format("R$ %.2f", pedido.finalPrice ?: 0.0).replace(".", ",")

            // Status Traduzido e com cor
            tvStatus.text = pedido.getStatusTraduzido()
            when (pedido.status?.lowercase()) {
                "accepted" -> tvStatus.setTextColor(Color.parseColor("#10B981"))
                "en_route" -> tvStatus.setTextColor(Color.parseColor("#F59E0B"))
                "arrived" -> tvStatus.setTextColor(Color.parseColor("#8B5CF6"))
                "in_progress" -> tvStatus.setTextColor(Color.parseColor("#3B82F6"))
                "completed" -> tvStatus.setTextColor(Color.parseColor("#10B981"))
                "canceled" -> tvStatus.setTextColor(Color.parseColor("#EF4444"))
                else -> tvStatus.setTextColor(Color.GRAY)
            }

            // Clique no item para abrir os detalhes
            itemView.setOnClickListener { onPedidoClick(pedido) }
        }
    }
}