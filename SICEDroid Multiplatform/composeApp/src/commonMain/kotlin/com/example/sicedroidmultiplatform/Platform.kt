package com.example.sicedroidmultiplatform

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform