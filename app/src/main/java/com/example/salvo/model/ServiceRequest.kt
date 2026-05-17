package com.example.salvo.model

import com.google.gson.annotations.SerializedName

data class ServiceRequest(
    val id: Int,

    @SerializedName("customer_id")
    val customerId: Int,

    @SerializedName("prestador_nome")
    val prestadorNome: String? = "Oficina Parceira",

    @SerializedName("service_type")
    val serviceType: String,

    val description: String,

    @SerializedName("vehicle_info")
    val vehicleInfo: String?,

    val status: String, // Valores esperados: 'searching', 'in_progress', 'completed', 'canceled'

    // Os campos abaixo podem vir nulos dependendo da fase do atendimento
    @SerializedName("assigned_provider_id")
    val assignedProviderId: Int?,

    @SerializedName("final_price")
    val finalPrice: Double?,

    @SerializedName("final_distance")
    val finalDistance: Double?,

    @SerializedName("destino_address")
    val destinoAddress: String?,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("prestador_foto")
    val prestadorFoto: String?,

    @SerializedName("veiculo_prestador_nome")
    val veiculoPrestadorNome: String?,

    @SerializedName("veiculo_prestador_placa")
    val veiculoPrestadorPlaca: String?
) {

    // --- TRUQUES DE INTERFACE (UI HELPERS) ---
    // Essas funções facilitam muito a vida do seu ViewHolder na hora de preencher a tela

    /** Retorna o preço formatado (ex: "R$ 150,00") ou um aviso se ainda não foi cobrado */
    fun getPrecoFormatado(): String {
        return finalPrice?.let { "R$ %.2f".format(it) } ?: "Pendente"
    }

    /** Retorna a distância com a unidade (ex: "15,5 km") */
    fun getDistanciaFormatada(): String {
        return finalDistance?.let { "%.1f km".format(it) } ?: ""
    }

    /** Traduz o status do banco para um texto amigável para o usuário ver na lista */
    fun getStatusTraduzido(): String {
        return when (status.lowercase()) {
            "searching" -> "Buscando Oficina..."
            "in_progress" -> "Em Andamento"
            "completed" -> "Concluído"
            "canceled" -> "Cancelado"
            else -> status
        }
    }
}
data class PollingStatusResponse(
    val status: String,
    val razaoCancelamento: String?,
    val detalhesOficina: OficinaDetalhesPolling?
)

data class OficinaDetalhesPolling(
    val nome: String,
    val fotoPerfil: String?,
    val valorFinal: Double,
    val distanciaKm: Double,
    val nomeVeiculo: String?,
    val placaVeiculo: String?
)

data class AceitarPedidoRequestApp(
    val requestId: Int,
    val providerId: Int,
    val price: Double,
    val distance: Double
)

data class AceitarPedidoResponse(
    val sucesso: Boolean,
    val mensagem: String
)
