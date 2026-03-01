package com.afhara.mywebsocket.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class KrakenWebSocket(private val client: HttpClient) {

    companion object {
        private const val WS_URL = "wss://ws.kraken.com"

        // Mapping: CoinGecko id -> Kraken pair
        val ID_TO_PAIR = mapOf(
            "bitcoin" to "XBT/USD",
            "ethereum" to "ETH/USD",
            "solana" to "SOL/USD",
            "binancecoin" to "BNB/USD",
        )

        // Reverse: Kraken pair -> CoinGecko id
        val PAIR_TO_ID = ID_TO_PAIR.entries.associate { (k, v) -> v to k }
    }

    private val json = Json { ignoreUnknownKeys = true }
    private var session: WebSocketSession? = null

    suspend fun connect(assets: List<String>): Flow<Map<String, Double>> {
        val pairs = assets.mapNotNull { ID_TO_PAIR[it] }
        if (pairs.isEmpty()) throw IllegalArgumentException("No valid pairs")

        val wsSession = client.webSocketSession(WS_URL)
        session = wsSession

        // Subscribe to ticker
        val subscribeMsg = """
            {
                "event": "subscribe",
                "pair": ${pairs.map { "\"$it\"" }},
                "subscription": {"name": "ticker"}
            }
        """.trimIndent()
        wsSession.send(Frame.Text(subscribeMsg))

        return wsSession.incoming
            .receiveAsFlow()
            .filter { it is Frame.Text }
            .mapNotNull { frame ->
                val text = (frame as Frame.Text).readText()
                parseTicker(text)
            }
    }

    private fun parseTicker(text: String): Map<String, Double>? {
        return try {
            val element = json.parseToJsonElement(text)

            // Ticker data comes as array: [channelId, {data}, "ticker", "XBT/USD"]
            if (element is JsonArray && element.size >= 4) {
                val pair = element.last().jsonPrimitive.content  // "XBT/USD"
                val coinId = PAIR_TO_ID[pair] ?: return null
                val data = element[1].jsonObject
                // "c" = last trade closed [price, lotVolume]
                val price = data["c"]?.jsonArray?.get(0)?.jsonPrimitive?.content?.toDoubleOrNull()
                    ?: return null

                mapOf(coinId to price)
            } else {
                null // skip events, heartbeats, subscription status
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun disconnect() {
        session?.close()
        session = null
    }

    fun isConnected(): Boolean = session != null
}