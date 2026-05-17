package com.example.salvo.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.salvo.R
import com.example.salvo.model.ServiceRequest
import com.google.android.material.button.MaterialButton

class PedidosAdapter(
    private var listaPedidos: List<ServiceRequest>,
    private val onPedirNovamenteClick: (ServiceRequest) -> Unit
) : RecyclerView.Adapter<PedidosAdapter.PedidoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pedido, parent, false)
        return PedidoViewHolder(view)
    }

    override fun getItemCount(): Int = listaPedidos.size

    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        val pedido = listaPedidos[position]
        holder.bind(pedido)
    }

    fun atualizarLista(novaLista: List<ServiceRequest>) {
        listaPedidos = novaLista
        notifyDataSetChanged()
    }

    inner class PedidoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcone: ImageView = itemView.findViewById(R.id.iv_icone_servico)
        private val tvNomeServico: TextView = itemView.findViewById(R.id.tv_nome_servico)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status_pedido)
        private val tvDataHora: TextView = itemView.findViewById(R.id.tv_data_hora)

        // Novos elementos da Oficina
        private val ivFotoPrestador: ImageView = itemView.findViewById(R.id.iv_foto_prestador)
        private val tvNomePrestador: TextView = itemView.findViewById(R.id.tv_nome_prestador)
        private val tvVeiculoPrestador: TextView = itemView.findViewById(R.id.tv_veiculo_prestador)
        private val tvPreco: TextView = itemView.findViewById(R.id.tv_preco)
        private val btnAcao: MaterialButton = itemView.findViewById(R.id.btn_acao_pedido)

        fun bind(pedido: ServiceRequest) {
            val context = itemView.context

            android.util.Log.w("TESTE_FOTO", "Oficina: ${pedido.prestadorNome} | Foto chegou? ${pedido.prestadorFoto != null}")

            tvNomeServico.text = pedido.serviceType
            tvDataHora.text = pedido.createdAt

            // Lógica Padrão do Preço e Botão
            val precoFinal = pedido.finalPrice ?: 0.0
            val precoTexto = "R$ ${String.format("%.2f", precoFinal).replace(".", ",")}"

            // 1. CARREGAMENTO DO VEÍCULO DA OFICINA
            if (!pedido.veiculoPrestadorNome.isNullOrEmpty() && !pedido.veiculoPrestadorPlaca.isNullOrEmpty()) {
                tvVeiculoPrestador.text =
                    "${pedido.veiculoPrestadorNome} • ${pedido.veiculoPrestadorPlaca}"
                tvVeiculoPrestador.visibility = View.VISIBLE
            } else {
                tvVeiculoPrestador.visibility = View.GONE
            }

            // 2. CARREGAMENTO DA FOTO EM BASE64
            if (!pedido.prestadorFoto.isNullOrEmpty()) {
                try {
                    val decodedString = android.util.Base64.decode(
                        pedido.prestadorFoto,
                        android.util.Base64.DEFAULT
                    )
                    val decodedByte = android.graphics.BitmapFactory.decodeByteArray(
                        decodedString,
                        0,
                        decodedString.size
                    )
                    ivFotoPrestador.setImageBitmap(decodedByte)
                    ivFotoPrestador.visibility = View.VISIBLE
                } catch (e: Exception) {
                    ivFotoPrestador.setImageResource(R.drawable.logo) // Logo padrão se der erro na imagem
                }
            } else {
                ivFotoPrestador.setImageResource(R.drawable.logo)
            }

            // 3. LÓGICA DE CORES E STATUS
            when (pedido.status?.lowercase()) {
                "searching" -> {
                    tvStatus.text = "Buscando..."
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#FF9800"))

                    tvNomePrestador.text = "Procurando oficinas..."
                    tvVeiculoPrestador.visibility = View.GONE
                    ivFotoPrestador.visibility = View.GONE
                    tvPreco.text = "---"
                    btnAcao.visibility = View.GONE
                }

                "accepted", "in_progress" -> {
                    tvStatus.text = "A CAMINHO"
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))

                    tvNomePrestador.text = pedido.prestadorNome ?: "Oficina Parceira"
                    tvPreco.text = precoTexto
                    btnAcao.visibility = View.GONE
                }

                "completed" -> {
                    tvStatus.text = "Concluído"
                    tvStatus.setTextColor(context.getColor(R.color.salvo_laranja))

                    tvNomePrestador.text = pedido.prestadorNome ?: "Oficina Concluída"
                    tvPreco.text = precoTexto

                    btnAcao.visibility = View.VISIBLE
                    btnAcao.text = "Pedir Novamente"
                }

                "canceled" -> {
                    tvStatus.text = "Cancelado"
                    tvStatus.setTextColor(context.getColor(android.R.color.holo_red_dark))

                    tvNomePrestador.text = "Nenhum prestador vinculado"
                    tvVeiculoPrestador.visibility = View.GONE
                    ivFotoPrestador.visibility = View.GONE
                    tvPreco.text = "---"

                    btnAcao.visibility = View.VISIBLE
                    btnAcao.text = "Tentar Novamente"
                }
            }

            btnAcao.setOnClickListener {
                onPedirNovamenteClick(pedido)
            }
        }
    }

}