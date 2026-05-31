package com.example.salvo.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.salvo.databinding.ItemRecentActivityBinding
import com.example.salvo.model.ServiceRequest

class RecentActivityAdapter(
    private var activities: List<ServiceRequest>,
    private val onPedidoClick: (ServiceRequest) -> Unit
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

            // 1. Nome do Cliente e ID
            tvClienteNome.text = "${pedido.clienteNome ?: "Cliente"} (#${pedido.id})"

            // 2. Serviço e Distância
            val distancia = pedido.finalDistance ?: 0.0
            val distanciaFormatada = String.format("%.1f", distancia).replace(".", ",")
            tvServicoDistancia.text = "${pedido.serviceType} • $distanciaFormatada km"

            // 3. Localização (Puxando da variável de endereço do banco)
            tvLocalizacao.text = "📍 ${pedido.destinoAddress ?: "Localização via GPS"}"

            // 4. Veículo da Oficina
            if (!pedido.veiculoPrestadorNome.isNullOrEmpty()) {
                tvVeiculoDesignado.text = "🚛 ${pedido.veiculoPrestadorNome} - ${pedido.veiculoPrestadorPlaca ?: ""}"
            } else {
                tvVeiculoDesignado.text = "🚛 Veículo pendente"
            }

            // 5. Valor
            val preco = pedido.finalPrice ?: 0.0
            if (preco > 0) {
                tvServicoValor.text = "R$ ${String.format("%.2f", preco).replace(".", ",")}"
            } else {
                tvServicoValor.text = "Pendente"
            }

            // 6. Data/Hora
            tvServicoHora.text = formatarHoraParaLocal(pedido.createdAt)

            // 7. Status e Cores dinâmicas
            tvStatus.text = pedido.getStatusTraduzido()
            when (pedido.status.lowercase()) {
                "completed" -> tvStatus.setTextColor(Color.parseColor("#10B981")) // Verde
                "canceled" -> tvStatus.setTextColor(Color.parseColor("#EF4444")) // Vermelho
                "en_route", "accepted" -> tvStatus.setTextColor(Color.parseColor("#F59E0B")) // Laranja (A caminho)
                "in_progress", "arrived" -> tvStatus.setTextColor(Color.parseColor("#3B82F6")) // Azul (No local/Em andamento)
                else -> tvStatus.setTextColor(Color.parseColor("#9CA3AF")) // Cinza (Outros)
            }

            // Ação de clique no cartão inteiro!
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

    private fun formatarHoraParaLocal(dataBanco: String?): String {
        if (dataBanco.isNullOrEmpty()) return "--:--"
        return try {
            // Normaliza a string caso venha com 'T' ou 'Z' do Ktor
            val dataLimpa = dataBanco.replace("Z", "").replace("T", " ").substring(0, 19)

            // Diz ao Android que esta hora que chegou está em UTC (Fuso 0)
            val formatoEntrada = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            formatoEntrada.timeZone = java.util.TimeZone.getTimeZone("UTC")

            val dataConvertida = formatoEntrada.parse(dataLimpa)

            // Prepara a saída apenas com Hora e Minuto baseada no fuso do Telemóvel
            val formatoSaida = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            formatoSaida.timeZone = java.util.TimeZone.getDefault() // Fuso do aparelho (ex: Brasília)

            if (dataConvertida != null) {
                formatoSaida.format(dataConvertida)
            } else {
                dataBanco.substring(11, 16) // Fallback caso seja nulo
            }
        } catch (e: Exception) {
            // Se o formato vier estranho, previne o crash e usa a técnica antiga do corte
            if (dataBanco.length >= 16) dataBanco.substring(11, 16) else dataBanco
        }
    }
}