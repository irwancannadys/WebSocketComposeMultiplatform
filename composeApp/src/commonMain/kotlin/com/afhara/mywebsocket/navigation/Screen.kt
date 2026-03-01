package com.afhara.mywebsocket.navigation


import kotlinx.serialization.Serializable

sealed interface Screen {

    @Serializable
    data object Login : Screen

    @Serializable
    data object Dashboard : Screen

    @Serializable
    data object CryptoList : Screen

    @kotlinx.serialization.Serializable
    data class Detail(val symbol: String) : Screen
}