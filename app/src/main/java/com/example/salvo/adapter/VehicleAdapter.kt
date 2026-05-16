package com.example.salvo.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.salvo.R
import com.example.salvo.model.Vehicle

class VehicleAdapter(
    private val vehicles: MutableList<Vehicle>, // 1. Mudar para MutableList
    private val onVehicleClick: (Vehicle) -> Unit // 2. Callback para o clique
) : RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder>() {

    class VehicleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStatus: TextView = view.findViewById(R.id.tv_veiculo_status)
        val tvNome: TextView = view.findViewById(R.id.tv_veiculo_nome)
        val tvPlaca: TextView = view.findViewById(R.id.tv_veiculo_placa)
        val ivFoto: ImageView = view.findViewById(R.id.iv_veiculo_foto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_veiculo_frota, parent, false)
        return VehicleViewHolder(view)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        val v = vehicles[position]
        holder.tvStatus.text = "Status: ${v.status}"
        holder.tvNome.text = v.name
        holder.tvPlaca.text = v.plate

        // Cor do status dinâmica para ajudar na visualização rápida
        when (v.status.lowercase()) {
            "disponível" -> holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#10B981")) // Verde
            "em atendimento" -> holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#F59E0B")) // Laranja
            else -> holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#EF4444")) // Vermelho
        }

        if (!v.vehiclePhoto.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(v.vehiclePhoto, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.ivFoto.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.ivFoto.setImageResource(android.R.drawable.ic_menu_camera)
            }
        }

        // Ativa o clique no card inteiro para mudar o status
        holder.itemView.setOnClickListener {
            onVehicleClick(v)
        }
    }

    override fun getItemCount() = vehicles.size

    // 3. Funções de apoio para o Swipe-to-Delete
    fun getVehicleAt(position: Int): Vehicle = vehicles[position]

    fun removerItem(position: Int) {
        vehicles.removeAt(position)
        notifyItemRemoved(position)
    }
}