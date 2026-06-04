package com.example.salvo.model

import com.google.gson.annotations.SerializedName

data class ServiceRequest(
    val id: Int,

    @SerializedName("customer_id")
    val customerId: Int,

    @SerializedName("prestador_nome")
    val prestadorNome: String? = "Oficina Parceira",

    @SerializedName("cliente_nome")
    val clienteNome: String?,

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
    val veiculoPrestadorPlaca: String?,

    @SerializedName("latitude")
    val latitude: Double? = null,

    @SerializedName("longitude")
    val longitude: Double? = null
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
            "en_route" -> "A caminho"
            "accepted" -> "Confirmado"
            "arrived" -> "No Local"
            "in_progress" -> "Em Andamento"
            "completed" -> "Concluído"
            "canceled" -> "Cancelado"
            else -> status
        }
    }
}
data class PollingStatusResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("provider_name") val providerName: String?,
    @SerializedName("vehicle_name") val vehicleName: String?,
    @SerializedName("vehicle_plate") val vehiclePlate: String?,
    @SerializedName("eta") val eta: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?
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
    val distance: Double,
    val vehicleId: Int
)

data class AceitarPedidoResponse(
    val sucesso: Boolean,
    val mensagem: String
)
