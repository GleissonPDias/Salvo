package com.example.salvo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// 1. O Estado da Tela
data class HomeUiState(
    val isLoading: Boolean = true,
    val isOnline: Boolean = false,
    val errorMessage: String? = null
)

// 2. A Regra de Negócio Principal
class HomePrestadorViewModel(
    private val repository: ProviderHomeRepository,
    private val currentProviderId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun toggleStatus(isNowOnline: Boolean) {
        val providerId = currentProviderId.toIntOrNull() ?: return

        // 1. Atualiza localmente de forma otimista para a UI responder instantaneamente
        _uiState.update { it.copy(isOnline = isNowOnline) }

        // 2. Realiza a chamada de rede real usando o PerfilRepository
        PerfilRepository().alternarStatusOnline(providerId, isNowOnline) { sucesso ->
            if (!sucesso) {
                // Se falhar no banco de dados, revertemos o status e injetamos o erro
                _uiState.update {
                    it.copy(
                        isOnline = !isNowOnline, // Desfaz a mudança do Switch
                        errorMessage = "Falha ao sincronizar status com o servidor."
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

// 3. A FÁBRICA (FACTORY) FICA AQUI, NO MESMO ARQUIVO!
class HomePrestadorViewModelFactory(
    private val repository: ProviderHomeRepository,
    private val providerId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomePrestadorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomePrestadorViewModel(repository, providerId) as T
        }
        throw IllegalArgumentException("Classe ViewModel desconhecida")
    }
}