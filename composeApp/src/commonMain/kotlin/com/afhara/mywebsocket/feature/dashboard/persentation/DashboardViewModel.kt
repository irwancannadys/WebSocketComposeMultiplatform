package com.afhara.mywebsocket.feature.dashboard.persentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afhara.mywebsocket.data.repository.CryptoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: CryptoRepository,
) : ViewModel() {

    companion object {
        val DASHBOARD_IDS = listOf("bitcoin", "ethereum", "binancecoin", "solana")
    }

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val _effect = Channel<DashboardEffect>()
    val effect = _effect.receiveAsFlow()

    private var webSocketJob: Job? = null

    init {
        onEvent(DashboardEvent.LoadDashboard)
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.LoadDashboard -> loadDashboard()
            is DashboardEvent.RetryLoad -> loadDashboard()
            is DashboardEvent.OnScreenLeave -> disconnectWebSocket()
            is DashboardEvent.OnScreenResume -> connectWebSocket()
            is DashboardEvent.OnCryptoClick -> {
                viewModelScope.launch {
                    _effect.send(DashboardEffect.NavigateToDetail(event.symbol))
                }
            }
        }
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.getCoins(DASHBOARD_IDS)
                .onSuccess { tickers ->
                    _state.update {
                        it.copy(isLoading = false, tickers = tickers)
                    }
                    connectWebSocket()
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isLoading = false, error = e.message ?: "Failed to load data")
                    }
                }
        }
    }

    private fun connectWebSocket() {
        // Don't reconnect if already running
        if (webSocketJob?.isActive == true) return
        // Don't connect if no tickers loaded
        if (_state.value.tickers.isEmpty()) return

        webSocketJob = viewModelScope.launch {
            try {
                println("🔌 WebSocket: Connecting...")
                repository.streamPrices(DASHBOARD_IDS).collect { priceMap ->
                    _state.update { currentState ->
                        val flashMap = mutableMapOf<String, PriceFlash>()
                        val updatedTickers = currentState.tickers.map { ticker ->
                            val newPrice = priceMap[ticker.id]
                            if (newPrice != null && newPrice != ticker.price) {
                                println("💰 Updating ${ticker.id}: ${ticker.price} -> $newPrice")
                                flashMap[ticker.id] = if (newPrice > ticker.price) {
                                    PriceFlash.UP
                                } else {
                                    PriceFlash.DOWN
                                }
                                ticker.copy(price = newPrice)
                            } else {
                                ticker
                            }
                        }
                        currentState.copy(
                            tickers = updatedTickers,
                            priceFlash = currentState.priceFlash + flashMap,
                        )
                    }

                    // Clear flash after 500ms
                    if (priceMap.isNotEmpty()) {
                        delay(500)
                        _state.update { it.copy(priceFlash = emptyMap()) }
                    }
                }
            } catch (e: Exception) {
                println("❌ WebSocket error: ${e.message}")
                _effect.send(DashboardEffect.ShowError("Live updates disconnected"))
            }
        }
    }

    private fun disconnectWebSocket() {
        viewModelScope.launch {
            println("🔌 WebSocket: Disconnecting...")
            webSocketJob?.cancel()
            webSocketJob = null
            repository.disconnect()
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnectWebSocket()
    }
}