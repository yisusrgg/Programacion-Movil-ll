package com.example.miprimerkmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform