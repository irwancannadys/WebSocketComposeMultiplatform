package com.afhara.mywebsocket.data.repository

import com.afhara.mywebsocket.data.datasource.CryptoRemoteDataSource
import com.afhara.mywebsocket.data.mapper.toDomain
import com.afhara.mywebsocket.data.model.CryptoTicker
import kotlinx.coroutines.flow.Flow

class CryptoRepositoryImpl(
    private val remoteDataSource: CryptoRemoteDataSource,
) : CryptoRepository {

    override suspend fun getMarkets(page: Int, perPage: Int): Result<List<CryptoTicker>> {
        return try {
            val response = remoteDataSource.getMarkets(page, perPage)
            Result.success(response.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCoins(ids: List<String>): Result<List<CryptoTicker>> {
        return try {
            val response = remoteDataSource.getCoins(ids)
            Result.success(response.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun streamPrices(assets: List<String>): Flow<Map<String, Double>> {
        return remoteDataSource.streamPrices(assets)
    }

    override suspend fun disconnect() {
        remoteDataSource.disconnect()
    }
}