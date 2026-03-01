package com.afhara.mywebsocket.data.mapper

import com.afhara.mywebsocket.data.model.CryptoTicker
import com.afhara.mywebsocket.data.model.CryptoTrade
import com.afhara.mywebsocket.data.response.CoinGeckoResponse
import com.afhara.mywebsocket.data.response.TradeResponse

fun CoinGeckoResponse.toDomain(): CryptoTicker {
    return CryptoTicker(
        symbol = symbol.uppercase(),
        price = currentPrice,
        priceChangePercent = priceChangePercentage24h ?: 0.0,
        volume = totalVolume ?: 0.0,
        highPrice = high24h ?: 0.0,
        lowPrice = low24h ?: 0.0,
        name = name,
        image = image,
        marketCap = marketCap,
        marketCapRank = marketCapRank,
        id = id,
    )
}

fun TradeResponse.toDomain(): CryptoTrade {
    return CryptoTrade(
        symbol = symbol,
        price = price.toDoubleOrNull() ?: 0.0,
        quantity = quantity.toDoubleOrNull() ?: 0.0,
        tradeTime = tradeTime,
    )
}