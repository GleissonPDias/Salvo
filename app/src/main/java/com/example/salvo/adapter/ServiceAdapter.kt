package com.example.salvo.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.salvo.R
import com.example.salvo.model.ServiceItem

class ServiceAdapter(
    private var services: MutableList<ServiceItem>,
    private val onStatusChanged: (ServiceItem, Boolean) -> Unit,
    private val onServiceClicked: (ServiceItem) -> Unit
) : RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder>() {

    class ServiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNome: TextView = view.findViewById(R.id.tv_servico_nome)
        val tvPreco: TextView = view.findViewById(R.id.tv_servico_preco)
        val switchAtivo: SwitchCompat = view.findViewById(R.id.switch_servico_ativo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_servico_oficina, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val servico = services[position]

        holder.tvNome.text = servico.serviceType

        val precoBase = String.format(java.util.Locale.US, "R$ %.2f", servico.basePrice)
        if (servico.pricePerKm > 0.1) {
            val precoKm = String.format(java.util.Locale.US, "R$ %.2f", servico.pricePerKm)
            holder.tvPreco.text = "Saída: $precoBase | Adicional: $precoKm/KM"
        } else {
            holder.tvPreco.text = "Preço Fixo: $precoBase"
        }

        // --- LÓGICA DE ESTADO (ATIVO/INATIVO) ---

        holder.switchAtivo.setOnCheckedChangeListener(null) // Limpa listener para evitar loops
        holder.switchAtivo.isChecked = servico.isActive

        // Se desativado, o card fica "esfumaçado" mas CONTINUA na tela
        holder.itemView.alpha = if (servico.isActive) 1.0f else 0.7f

        holder.switchAtivo.setOnCheckedChangeListener { _, isChecked ->
            servico.isActive = isChecked // Muda no objeto local do adapter
            holder.itemView.alpha = if (isChecked) 1.0f else 0.7f
            onStatusChanged(servico, isChecked) // Avisa a Activity
        }

        holder.itemView.setOnClickListener { onServiceClicked(servico) }
    }

    override fun getItemCount(): Int = services.size

    // Método vital: atualiza a lista sem destruir o adapter
    fun updateList(newList: List<ServiceItem>) {
        this.services = newList.toMutableList()
        notifyDataSetChanged()
    }
}