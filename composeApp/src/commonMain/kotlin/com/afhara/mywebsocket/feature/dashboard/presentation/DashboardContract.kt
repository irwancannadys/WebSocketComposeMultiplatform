package com.afhara.mywebsocket.feature.dashboard.presentation

import com.afhara.mywebsocket.data.model.CryptoTicker

data class DashboardState(
    val isLoading: Boolean = true,
    val tickers: List<CryptoTicker> = emptyList(),
    val error: String? = null,
    val priceFlash: Map<String, PriceFlash> = emptyMap(),
)

enum class PriceFlash {
    UP, DOWN, NONE
}

sealed interface DashboardEvent {
    data object LoadDashboard : DashboardEvent
    data object RetryLoad : DashboardEvent
    data object OnScreenLeave : DashboardEvent
    data object OnScreenResume : DashboardEvent
    data class OnCryptoClick(val symbol: String) : DashboardEvent
}

sealed interface DashboardEffect {
    data class NavigateToDetail(val symbol: String) : DashboardEffect
    data class ShowError(val message: String) : DashboardEffect
}