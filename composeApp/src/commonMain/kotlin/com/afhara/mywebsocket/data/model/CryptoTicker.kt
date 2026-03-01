package com.afhara.mywebsocket.data.model

data class CryptoTicker(
    val id: String,
    val symbol: String,
    val name: String,
    val price: Double,
    val priceChangePercent: Double,
    val volume: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val image: String? = null,
    val marketCap: Long? = null,
    val marketCapRank: Int? = null,
)
