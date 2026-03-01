package com.afhara.mywebsocket.data.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CoinGeckoResponse(
    @SerialName("id") val id: String,
    @SerialName("symbol") val symbol: String,
    @SerialName("name") val name: String,
    @SerialName("current_price") val currentPrice: Double,
    @SerialName("price_change_percentage_24h") val priceChangePercentage24h: Double? = null,
    @SerialName("total_volume") val totalVolume: Double? = null,
    @SerialName("high_24h") val high24h: Double? = null,
    @SerialName("low_24h") val low24h: Double? = null,
    @SerialName("image") val image: String? = null,
    @SerialName("market_cap") val marketCap: Long? = null,
    @SerialName("market_cap_rank") val marketCapRank: Int? = null,
)
