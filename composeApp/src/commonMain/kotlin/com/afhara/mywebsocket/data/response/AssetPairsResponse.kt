package com.afhara.mywebsocket.data.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class AssetPairsResponse(
    @SerialName("error") val error: List<String>,
    @SerialName("result") val result: JsonObject,
)