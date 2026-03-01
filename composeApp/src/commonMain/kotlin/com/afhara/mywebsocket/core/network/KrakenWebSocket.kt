package com.afhara.mywebsocket.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class KrakenWebSocket(private val client: HttpClient) {

    companion object {
        private const val WS_URL = "wss://ws.kraken.com"
    }

    private val json = Json { ignoreUnknownKeys = true }
    private var session: WebSocketSession? = null

    // Dynamic mappings — populated at runtime
    private var idToPair = mutableMapOf<String, String>()
    private var pairToId = mutableMapOf<String, String>()

    // Set available pairs from Kraken API
    fun setAvailablePairs(availablePairs: Set<String>) {
        val krakenMapping = mapOf(
            "bitcoin" to "XBT/USD",
            "ethereum" to "ETH/USD",
            "solana" to "SOL/USD",
            "ripple" to "XRP/USD",
            "cardano" to "ADA/USD",
            "dogecoin" to "DOGE/USD",
            "polkadot" to "DOT/USD",
            "chainlink" to "LINK/USD",
            "avalanche-2" to "AVAX/USD",
            "matic-network" to "MATIC/USD",
            "litecoin" to "LTC/USD",
            "uniswap" to "UNI/USD",
            "stellar" to "XLM/USD",
            "cosmos" to "ATOM/USD",
            "tron" to "TRX/USD",
            "near" to "NEAR/USD",
            "aptos" to "APT/USD",
            "arbitrum" to "ARB/USD",
            "optimism" to "OP/USD",
            "filecoin" to "FIL/USD",
            "aave" to "AAVE/USD",
            "algorand" to "ALGO/USD",
            "the-sandbox" to "SAND/USD",
            "shiba-inu" to "SHIB/USD",
            "pepe" to "PEPE/USD",
        )

        idToPair.clear()
        pairToId.clear()

        if (availablePairs.isEmpty()) {
            // Fallback: use all known mappings
            println("⚠️ Using fallback Kraken pairs (REST failed)")
            krakenMapping.forEach { (id, pair) ->
                idToPair[id] = pair
                pairToId[pair] = id
            }
        } else {
            krakenMapping.forEach { (id, pair) ->
                if (availablePairs.contains(pair)) {
                    idToPair[id] = pair
                    pairToId[pair] = id
                }
            }
        }

        println("✅ Kraken supported pairs: ${idToPair.keys}")
    }

    fun getSupportedIds(): Set<String> = idToPair.keys

    suspend fun connect(assets: List<String>): Flow<Map<String, Double>> {
        val pairs = assets.mapNotNull { idToPair[it] }
        if (pairs.isEmpty()) throw IllegalArgumentException("No valid pairs to subscribe")

        val wsSession = client.webSocketSession(WS_URL)
        session = wsSession

        val subscribeMsg = """
            {
                "event": "subscribe",
                "pair": ${pairs.map { "\"$it\"" }},
                "subscription": {"name": "ticker"}
            }
        """.trimIndent()
        wsSession.send(Frame.Text(subscribeMsg))

        println("📡 Subscribed to: $pairs")

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
            if (element is JsonArray && element.size >= 4) {
                val pair = element.last().jsonPrimitive.content
                val coinId = pairToId[pair] ?: return null
                val data = element[1].jsonObject
                val price = data["c"]?.jsonArray?.get(0)?.jsonPrimitive?.content?.toDoubleOrNull()
                    ?: return null
                mapOf(coinId to price)
            } else {
                null
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