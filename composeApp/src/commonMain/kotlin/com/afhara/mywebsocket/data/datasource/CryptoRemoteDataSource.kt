package com.afhara.mywebsocket.data.datasource

import com.afhara.mywebsocket.core.network.CoinGeckoApi
import com.afhara.mywebsocket.core.network.KrakenApi
import com.afhara.mywebsocket.core.network.KrakenWebSocket
import com.afhara.mywebsocket.data.response.CoinGeckoResponse
import kotlinx.coroutines.flow.Flow

class CryptoRemoteDataSource(
    private val coinGeckoApi: CoinGeckoApi,
    private val krakenApi: KrakenApi,
    private val webSocket: KrakenWebSocket,
) {
    private var krakenInitialized = false

    suspend fun getMarkets(page: Int = 1, perPage: Int = 20): List<CoinGeckoResponse> {
        return coinGeckoApi.getMarkets(page, perPage)
    }

    suspend fun getCoins(ids: List<String>): List<CoinGeckoResponse> {
        return coinGeckoApi.getCoins(ids)
    }

    suspend fun initKrakenPairs() {
        if (krakenInitialized) return
        val pairs = krakenApi.getAvailablePairs()
        webSocket.setAvailablePairs(pairs)
        krakenInitialized = true
    }

    fun getKrakenSupportedIds(): Set<String> {
        return webSocket.getSupportedIds()
    }

    suspend fun streamPrices(assets: List<String>): Flow<Map<String, Double>> {
        return webSocket.connect(assets)
    }

    suspend fun disconnect() {
        webSocket.disconnect()
    }
}