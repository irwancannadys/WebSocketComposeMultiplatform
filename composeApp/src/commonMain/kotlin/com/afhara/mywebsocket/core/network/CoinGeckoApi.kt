package com.afhara.mywebsocket.core.network

import com.afhara.mywebsocket.data.response.CoinGeckoResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class CoinGeckoApi(private val client: HttpClient) {

    companion object {
        private const val BASE_URL = "https://api.coingecko.com/api/v3"
    }

    // Get top coins by market cap
    suspend fun getMarkets(
        page: Int = 1,
        perPage: Int = 20,
    ): List<CoinGeckoResponse> {
        return client.get("$BASE_URL/coins/markets") {
            parameter("vs_currency", "usd")
            parameter("order", "market_cap_desc")
            parameter("per_page", perPage)
            parameter("page", page)
            parameter("sparkline", false)
        }.body()
    }

    // Get specific coins
    suspend fun getCoins(ids: List<String>): List<CoinGeckoResponse> {
        return client.get("$BASE_URL/coins/markets") {
            parameter("vs_currency", "usd")
            parameter("ids", ids.joinToString(","))
            parameter("sparkline", false)
        }.body()
    }
}