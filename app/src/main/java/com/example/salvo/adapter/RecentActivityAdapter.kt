package com.example.salvo.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.salvo.databinding.ItemRecentActivityBinding
import com.example.salvo.model.ServiceRequest // <-- Importe o seu modelo real

class RecentActivityAdapter(
    // Agora o Adapter recebe a lista diretamente da API
    private var activities: List<ServiceRequest>
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
            // 1. NOME DO CLIENTE OU ID
            // OBS: Se a sua ServiceRequest atual não tem a variável do nome do cliente,
            // usamos o ID do pedido provisoriamente até você adicionar no Backend.
            tvClienteNome.text = "Atendimento #${pedido.id}"

            // 2. TIPO DE SERVIÇO E DISTÂNCIA
            val distancia = pedido.finalDistance ?: 0.0
            val distanciaFormatada = String.format("%.1f", distancia).replace(".", ",")
            tvServicoDistancia.text = "${pedido.serviceType} • $distanciaFormatada km"

            // 3. VALOR DO SERVIÇO FORMATADO
            val preco = pedido.finalPrice ?: 0.0
            if (preco > 0) {
                tvServicoValor.text = "R$ ${String.format("%.2f", preco).replace(".", ",")}"
            } else {
                tvServicoValor.text = "Valor Indefinido"
            }

            // 4. HORA FORMATADA
            // O banco manda algo como "2026-05-17 18:55:50".
            // O substring(11, 16) recorta e pega apenas o "18:55" para ficar bonito na tela.
            val dataBanco = pedido.createdAt
            tvServicoHora.text = if (dataBanco.length >= 16) dataBanco.substring(11, 16) else dataBanco
        }
    }

    override fun getItemCount(): Int = activities.size

    // 🔥 Função de bônus: Facilita muito atualizar a tela quando o Retrofit responder!
    fun atualizarLista(novaLista: List<ServiceRequest>) {
        this.activities = novaLista
        notifyDataSetChanged()
    }
}