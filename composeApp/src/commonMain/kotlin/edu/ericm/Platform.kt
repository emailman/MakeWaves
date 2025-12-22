package edu.ericm

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform