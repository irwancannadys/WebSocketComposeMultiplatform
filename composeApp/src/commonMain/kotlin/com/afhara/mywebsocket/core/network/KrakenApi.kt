package com.afhara.mywebsocket.core.network

import com.afhara.mywebsocket.data.response.AssetPairsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class KrakenApi(
    private val client: HttpClient
) {
    companion object {
        private const val BASE_URL = "https://api.kraken.com/0/public"
    }

    suspend fun getAvailablePairs(): Set<String> {
        return try {
            val response: AssetPairsResponse = client.get("$BASE_URL/AssetPairs").body()
            response.result.values.mapNotNull { entry ->
                entry.jsonObject["wsname"]?.jsonPrimitive?.content
            }.filter { it.endsWith("/USD") }.toSet()
        } catch (e: Exception) {
            println("❌ Failed to fetch Kraken pairs: ${e.message}")
            emptySet()
        }
    }

}