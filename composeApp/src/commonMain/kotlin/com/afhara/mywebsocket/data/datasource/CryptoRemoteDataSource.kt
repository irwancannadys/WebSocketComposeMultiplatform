package com.afhara.mywebsocket.data.datasource

import com.afhara.mywebsocket.core.network.KrakenWebSocket
import com.afhara.mywebsocket.core.network.CoinGeckoApi
import com.afhara.mywebsocket.data.response.CoinGeckoResponse
import kotlinx.coroutines.flow.Flow

class CryptoRemoteDataSource(
    private val api: CoinGeckoApi,
    private val webSocket: KrakenWebSocket,
) {
    suspend fun getMarkets(page: Int = 1, perPage: Int = 20): List<CoinGeckoResponse> {
        return api.getMarkets(page, perPage)
    }

    suspend fun getCoins(ids: List<String>): List<CoinGeckoResponse> {
        return api.getCoins(ids)
    }

    // WebSocket: real-time prices
    suspend fun streamPrices(assets: List<String>): Flow<Map<String, Double>> {
        return webSocket.connect(assets)
    }

    suspend fun disconnect() {
        webSocket.disconnect()
    }
}