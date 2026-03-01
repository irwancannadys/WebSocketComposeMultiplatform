package com.afhara.mywebsocket.data.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TradeResponse(
    @SerialName("s") val symbol: String,
    @SerialName("p") val price: String,
    @SerialName("q") val quantity: String,
    @SerialName("T") val tradeTime: Long,
)

@Serializable
data class StreamWrapper(
    @SerialName("stream") val stream: String,
    @SerialName("data") val data: TradeResponse,
)
