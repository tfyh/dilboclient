package org.dilbo.dilboclient

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform