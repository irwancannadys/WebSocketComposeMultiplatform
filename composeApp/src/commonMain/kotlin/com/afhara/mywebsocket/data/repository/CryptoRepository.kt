package com.afhara.mywebsocket.data.repository

import com.afhara.mywebsocket.data.model.CryptoTicker
import com.afhara.mywebsocket.data.model.CryptoTrade
import kotlinx.coroutines.flow.Flow

interface CryptoRepository {
    suspend fun getMarkets(page: Int = 1, perPage: Int = 20): Result<List<CryptoTicker>>
    suspend fun getCoins(ids: List<String>): Result<List<CryptoTicker>>

    suspend fun initKrakenPairs()
    fun getKrakenSupportedIds(): Set<String>
    suspend fun streamPrices(assets: List<String>): Flow<Map<String, Double>>
    suspend fun disconnect()
}