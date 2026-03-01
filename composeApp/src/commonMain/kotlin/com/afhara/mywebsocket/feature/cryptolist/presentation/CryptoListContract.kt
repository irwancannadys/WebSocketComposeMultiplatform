package com.afhara.mywebsocket.feature.cryptolist.presentation

import com.afhara.mywebsocket.data.model.CryptoTicker
import com.afhara.mywebsocket.feature.dashboard.presentation.PriceFlash

data class CryptoListState(
    val isLoading: Boolean = true,
    val tickers: List<CryptoTicker> = emptyList(),
    val error: String? = null,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 1,
    val hasMorePages: Boolean = true,
    val priceFlash: Map<String, PriceFlash> = emptyMap(),
)

sealed interface CryptoListEvent {
    data object LoadMarket : CryptoListEvent
    data object LoadMore : CryptoListEvent
    data object RetryLoad : CryptoListEvent
    data object OnScreenResume : CryptoListEvent
    data object OnScreenLeave : CryptoListEvent
    data class OnCryptoClick(val id: String) : CryptoListEvent
}

sealed interface CryptoListEffect {
    data class NavigateToDetail(val id: String) : CryptoListEffect
    data class ShowError(val message: String) : CryptoListEffect
}