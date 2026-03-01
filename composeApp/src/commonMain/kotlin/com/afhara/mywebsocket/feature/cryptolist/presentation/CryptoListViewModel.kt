package com.afhara.mywebsocket.feature.cryptolist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afhara.mywebsocket.data.repository.CryptoRepository
import com.afhara.mywebsocket.feature.dashboard.presentation.PriceFlash
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CryptoListViewModel(
    private val repository: CryptoRepository,
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 20
    }

    private val _state = MutableStateFlow(CryptoListState())
    val state: StateFlow<CryptoListState> = _state.asStateFlow()

    private val _effect = Channel<CryptoListEffect>()
    val effect = _effect.receiveAsFlow()

    private var webSocketJob: Job? = null

    init {
        onEvent(CryptoListEvent.LoadMarket)
    }

    fun onEvent(event: CryptoListEvent) {
        when (event) {
            is CryptoListEvent.LoadMarket -> loadMarket()
            is CryptoListEvent.LoadMore -> loadMore()
            is CryptoListEvent.RetryLoad -> loadMarket()
            is CryptoListEvent.OnScreenResume -> connectWebSocket()
            is CryptoListEvent.OnScreenLeave -> disconnectWebSocket()
            is CryptoListEvent.OnCryptoClick -> {
                viewModelScope.launch {
                    _effect.send(CryptoListEffect.NavigateToDetail(event.id))
                }
            }
        }
    }

    private fun loadMarket() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, currentPage = 1) }

            launch { repository.initKrakenPairs() }

            repository.getMarkets(page = 1, perPage = PAGE_SIZE)
                .onSuccess { tickers ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            tickers = tickers,
                            currentPage = 1,
                            hasMorePages = tickers.size >= PAGE_SIZE,
                        )
                    }
                    connectWebSocket()
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isLoading = false, error = e.message ?: "Failed to load")
                    }
                }
        }
    }

    private fun loadMore() {
        if (_state.value.isLoadingMore || !_state.value.hasMorePages) return

        viewModelScope.launch {
            val nextPage = _state.value.currentPage + 1
            _state.update { it.copy(isLoadingMore = true) }

            repository.getMarkets(page = nextPage, perPage = PAGE_SIZE)
                .onSuccess { newTickers ->
                    _state.update {
                        it.copy(
                            isLoadingMore = false,
                            tickers = it.tickers + newTickers,
                            currentPage = nextPage,
                            hasMorePages = newTickers.size >= PAGE_SIZE,
                        )
                    }
                    // Don't reconnect WebSocket — new coins will update on next reconnect
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoadingMore = false) }
                    _effect.send(CryptoListEffect.ShowError("Failed to load more"))
                }
        }
    }

    private fun connectWebSocket() {
        if (webSocketJob?.isActive == true) return
        if (_state.value.tickers.isEmpty()) return

        val supportedIds = repository.getKrakenSupportedIds()
        val toSubscribe = _state.value.tickers
            .map { it.id }
            .filter { it in supportedIds }

        if (toSubscribe.isEmpty()) return

        webSocketJob = viewModelScope.launch {
            try {
                println("🔌 CryptoList WebSocket: Connecting for ${toSubscribe.size} coins...")
                repository.streamPrices(toSubscribe).collect { priceMap ->
                    _state.update { currentState ->
                        val flashMap = mutableMapOf<String, PriceFlash>()
                        val updatedTickers = currentState.tickers.map { ticker ->
                            val newPrice = priceMap[ticker.id]
                            if (newPrice != null && newPrice != ticker.price) {
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

                    if (priceMap.isNotEmpty()) {
                        delay(500)
                        _state.update { it.copy(priceFlash = emptyMap()) }
                    }
                }
            } catch (e: CancellationException) {
                // Normal cancellation (tab switch), don't log as error
                println("🔌 CryptoList WebSocket: Cancelled")
            } catch (e: Exception) {
                println("❌ CryptoList WebSocket error: ${e.message}")
            }
        }
    }

    private fun disconnectWebSocket() {
        webSocketJob?.cancel()
        webSocketJob = null
        viewModelScope.launch {
            repository.disconnect()
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnectWebSocket()
    }
}