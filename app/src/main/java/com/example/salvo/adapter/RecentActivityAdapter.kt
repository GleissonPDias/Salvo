package com.example.salvo.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.salvo.databinding.ItemRecentActivityBinding

// 1. O Modelo de dados simplificado para a nossa UI da Home
data class RecentActivity(
    val id: Int,
    val clientName: String,
    val serviceType: String,
    val distance: String,
    val value: String,
    val time: String
)

// 2. O Adapter responsável por inflar e reciclar os cartões na tela
class RecentActivityAdapter(
    private val activities: List<RecentActivity>
) : RecyclerView.Adapter<RecentActivityAdapter.ActivityViewHolder>() {

    // O ViewHolder segura as referências dos componentes visuais usando o ViewBinding do item
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
        val activity = activities[position]

        // Vincula os dados do Kotlin direto nas Views do XML
        with(holder.binding) {
            tvClienteNome.text = activity.clientName
            tvServicoDistancia.text = "${activity.serviceType} • ${activity.distance}"
            tvServicoValor.text = activity.value
            tvServicoHora.text = activity.time
        }
    }

    override fun getItemCount(): Int = activities.size
}