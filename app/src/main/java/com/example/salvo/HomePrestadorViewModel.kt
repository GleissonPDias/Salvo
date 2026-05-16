package com.example.salvo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
        viewModelScope.launch {
            // Atualiza localmente para resposta rápida
            _uiState.update { it.copy(isOnline = isNowOnline) }

            // Aqui futuramente a chamada de rede real
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