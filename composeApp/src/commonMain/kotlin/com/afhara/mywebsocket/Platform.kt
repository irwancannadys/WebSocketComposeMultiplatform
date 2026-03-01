package com.afhara.mywebsocket

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform