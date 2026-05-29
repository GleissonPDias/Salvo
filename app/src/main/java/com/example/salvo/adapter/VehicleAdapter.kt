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
    private val vehicles: MutableList<Vehicle>,
    private val onVehicleClick: (Vehicle) -> Unit,
    private val onVehicleLongClick: (Vehicle) -> Unit // NOVO: Escuta o clique longo

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

        when (v.status.lowercase()) {
            "disponível" -> holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#10B981"))
            "em atendimento" -> holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#F59E0B"))
            else -> holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#EF4444"))
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

        // Toque Simples (Abre a nova tela)
        holder.itemView.setOnClickListener {
            onVehicleClick(v)
        }

        // Toque Longo (Abre o menu de edição)
        holder.itemView.setOnLongClickListener {
            onVehicleLongClick(v)
            true // Retorna 'true' para indicar que a ação longa foi consumida
        }
    }

    override fun getItemCount() = vehicles.size

    fun getVehicleAt(position: Int): Vehicle = vehicles[position]

    fun removerItem(position: Int) {
        vehicles.removeAt(position)
        notifyItemRemoved(position)
    }
}