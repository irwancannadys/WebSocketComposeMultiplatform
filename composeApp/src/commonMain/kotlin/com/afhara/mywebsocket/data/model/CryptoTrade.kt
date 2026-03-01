package com.afhara.mywebsocket.data.model

data class CryptoTrade(
    val symbol: String,
    val price: Double,
    val quantity: Double,
    val tradeTime: Long,
)
