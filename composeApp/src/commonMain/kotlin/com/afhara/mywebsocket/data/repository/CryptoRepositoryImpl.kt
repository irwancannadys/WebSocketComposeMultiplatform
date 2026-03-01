package com.afhara.mywebsocket.data.repository

import com.afhara.mywebsocket.data.datasource.CryptoRemoteDataSource
import com.afhara.mywebsocket.data.mapper.toDomain
import com.afhara.mywebsocket.data.model.CryptoTicker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

class CryptoRepositoryImpl(
    private val remoteDataSource: CryptoRemoteDataSource,
) : CryptoRepository {

    companion object {
        private const val CACHE_DURATION_MS = 30_000L // 30 seconds
    }

    // Cache
    private val mutex = Mutex()
    private var dashboardCache: List<CryptoTicker>? = null
    private var dashboardCacheTime: Long = 0L
    private var marketsCache: MutableMap<Int, List<CryptoTicker>> = mutableMapOf() // page -> data
    private var marketsCacheTime: MutableMap<Int, Long> = mutableMapOf()

    private fun currentTime(): Long = Clock.System.now().toEpochMilliseconds()

    private fun isCacheValid(cacheTime: Long): Boolean {
        return (currentTime() - cacheTime) < CACHE_DURATION_MS
    }

    override suspend fun getMarkets(page: Int, perPage: Int): Result<List<CryptoTicker>> {
        return mutex.withLock {
            // Check cache
            val cached = marketsCache[page]
            val cacheTime = marketsCacheTime[page] ?: 0L
            if (cached != null && isCacheValid(cacheTime)) {
                println("📦 Markets page $page from cache")
                return@withLock Result.success(cached)
            }

            // Fetch from API
            try {
                val response = remoteDataSource.getMarkets(page, perPage)
                val tickers = response.map { it.toDomain() }
                marketsCache[page] = tickers
                marketsCacheTime[page] = currentTime()
                println("🌐 Markets page $page from API")
                Result.success(tickers)
            } catch (e: Exception) {
                // Return stale cache if available
                if (cached != null) {
                    println("⚠️ API failed, returning stale cache for page $page")
                    Result.success(cached)
                } else {
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun getCoins(ids: List<String>): Result<List<CryptoTicker>> {
        return mutex.withLock {
            // Check cache
            if (dashboardCache != null && isCacheValid(dashboardCacheTime)) {
                println("📦 Dashboard coins from cache")
                return@withLock Result.success(dashboardCache!!)
            }

            // Fetch from API
            try {
                val response = remoteDataSource.getCoins(ids)
                val tickers = response.map { it.toDomain() }
                dashboardCache = tickers
                dashboardCacheTime = currentTime()
                println("🌐 Dashboard coins from API")
                Result.success(tickers)
            } catch (e: Exception) {
                if (dashboardCache != null) {
                    println("⚠️ API failed, returning stale dashboard cache")
                    Result.success(dashboardCache!!)
                } else {
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun initKrakenPairs() {
        remoteDataSource.initKrakenPairs()
    }

    override fun getKrakenSupportedIds(): Set<String> {
        return remoteDataSource.getKrakenSupportedIds()
    }

    override suspend fun streamPrices(assets: List<String>): Flow<Map<String, Double>> {
        return remoteDataSource.streamPrices(assets)
    }

    override suspend fun disconnect() {
        remoteDataSource.disconnect()
    }
}