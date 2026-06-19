package com.example.multiapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform