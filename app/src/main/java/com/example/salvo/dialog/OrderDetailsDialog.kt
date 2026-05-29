package com.example.salvo.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import com.example.salvo.R
import com.example.salvo.model.ServiceRequest
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Dialog que exibe detalhes completos do pedido/serviço em um Bottom Sheet
 * Mostra informações do pedido, cliente e localização
 */
class OrderDetailsDialog(
    context: Context,
    private val pedido: ServiceRequest,
    private val onStatusChangeClick: (ServiceRequest) -> Unit,
    private val onContactClick: (ServiceRequest) -> Unit,
    private val onDismiss: () -> Unit
) : BottomSheetDialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_order_details, null)
        setupViews(view)
        setContentView(view)

        // Quando o diálogo fecha, chamar callback
        setOnDismissListener { onDismiss() }
    }

    private fun setupViews(view: android.view.View) {
        // Informações do Pedido
        view.findViewById<TextView>(R.id.tv_pedido_id).text = "Pedido #${pedido.id}"
        view.findViewById<TextView>(R.id.tv_tipo_servico).text = pedido.serviceType
        view.findViewById<TextView>(R.id.tv_status_pedido).text = obterStatusFormatado(pedido.status)

        // Cor do status
        val tvStatus = view.findViewById<TextView>(R.id.tv_status_pedido)
        tvStatus.setTextColor(obterCorStatus(pedido.status))

        // 🔧 CORRIGIDO: Usar as propriedades que realmente existem
        // Cliente (usar ID já que não temos nome)
        view.findViewById<TextView>(R.id.tv_nome_cliente).text = "Cliente ID: ${pedido.customerId}"

        // Telefone/Contato (não existe no ServiceRequest, então ocultamos)
        val tvTelefone = view.findViewById<TextView>(R.id.tv_telefone_cliente)
        tvTelefone.text = "Sem contato direto"

        // Localização (usar destinoAddress)
        view.findViewById<TextView>(R.id.tv_endereco_servico).text =
            pedido.destinoAddress ?: "Localização não informada"

        // Detalhes adicionais (usar description)
        val tvDescricao = view.findViewById<TextView>(R.id.tv_descricao_servico)
        if (pedido.description.isNotEmpty()) {
            tvDescricao.text = pedido.description
        } else {
            tvDescricao.text = "Sem descrição adicional"
        }

        // Informações do Prestador (se disponível)
        val tvPrestador = view.findViewById<TextView>(R.id.tv_prestador_info)
        if (tvPrestador != null) {
            tvPrestador.text = pedido.prestadorNome ?: "Aguardando oficina..."
        }

        // Botões de ação
        view.findViewById<Button>(R.id.btn_alterar_status).setOnClickListener {
            onStatusChangeClick(pedido)
            dismiss()
        }

        view.findViewById<Button>(R.id.btn_contatar_cliente).setOnClickListener {
            onContactClick(pedido)
            dismiss()
        }

        view.findViewById<Button>(R.id.btn_fechar_detalhe).setOnClickListener {
            dismiss()
        }
    }

    private fun obterStatusFormatado(status: String): String {
        return when (status.lowercase()) {
            "searching" -> "⏳ Em Busca"
            "in_progress" -> "🔄 Em Andamento"
            "completed" -> "✔️ Concluído"
            "canceled" -> "❌ Cancelado"
            else -> "ℹ️ ${status.uppercase()}"
        }
    }

    private fun obterCorStatus(status: String): Int {
        return when (status.lowercase()) {
            "searching" -> context.getColor(android.R.color.holo_orange_dark)
            "in_progress" -> context.getColor(android.R.color.holo_blue_light)
            "completed" -> context.getColor(android.R.color.holo_green_dark)
            "canceled" -> context.getColor(android.R.color.holo_red_dark)
            else -> context.getColor(android.R.color.darker_gray)
        }
    }
}