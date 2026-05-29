package com.example.salvo.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.salvo.databinding.ItemRecentActivityBinding
import com.example.salvo.model.ServiceRequest

class RecentActivityAdapter(
    private var activities: List<ServiceRequest>,
    private val onPedidoClick: (ServiceRequest) -> Unit // NOVO: Gatilho de clique!
) : RecyclerView.Adapter<RecentActivityAdapter.ActivityViewHolder>() {

    class ActivityViewHolder(val binding: ItemRecentActivityBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val binding = ItemRecentActivityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ActivityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val pedido = activities[position]

        with(holder.binding) {
            tvClienteNome.text = "Atendimento #${pedido.id}"

            val distancia = pedido.finalDistance ?: 0.0
            val distanciaFormatada = String.format("%.1f", distancia).replace(".", ",")
            tvServicoDistancia.text = "${pedido.serviceType} • $distanciaFormatada km"

            val preco = pedido.finalPrice ?: 0.0
            if (preco > 0) {
                tvServicoValor.text = "R$ ${String.format("%.2f", preco).replace(".", ",")}"
            } else {
                tvServicoValor.text = "Valor Indefinido"
            }

            val dataBanco = pedido.createdAt
            tvServicoHora.text = if (dataBanco.length >= 16) dataBanco.substring(11, 16) else dataBanco

            // NOVO: Ação de clique no cartão inteiro!
            root.setOnClickListener {
                onPedidoClick(pedido)
            }
        }
    }

    override fun getItemCount(): Int = activities.size

    fun atualizarLista(novaLista: List<ServiceRequest>) {
        this.activities = novaLista
        notifyDataSetChanged()
    }
}