package com.techliex

import com.techliex.data.db.DatabaseFactory
import com.techliex.plugins.*
import com.techliex.security.JwtConfig
import com.techliex.security.configureSecurity
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain
import java.net.NetworkInterface

fun main(args: Array<String>) {
    printLocalIpAddresses()
    EngineMain.main(args)
}

private fun printLocalIpAddresses() {
    println("\n==================================================")
    println("🚀 Starting Techliex Management Ktor Server...")
    println("📱 To connect from an Android phone / device on Wi-Fi:")
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val netInterface = interfaces.nextElement()
            if (!netInterface.isLoopback && netInterface.isUp) {
                val addresses = netInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr.hostAddress.contains(".")) {
                        println("   👉 Base API: http://${addr.hostAddress}:8080/api/v1")
                        println("   📖 Swagger:  http://${addr.hostAddress}:8080/swagger")
                    }
                }
            }
        }
    } catch (_: Exception) {
        println("   👉 Base API: http://<YOUR_WIFI_IP>:8080/api/v1")
    }
    println("==================================================\n")
}

fun Application.module() {
    DatabaseFactory.init(environment.config)
    JwtConfig.init(environment.config)

    configureSerialization()
    configureSecurity()
    configureStatusPages()
    configureCORS()
    configureRouting()
}
