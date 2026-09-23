package com.techliex

import com.techliex.data.db.DatabaseFactory
import com.techliex.plugins.*
import com.techliex.security.JwtConfig
import com.techliex.security.configureSecurity
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>) {
    EngineMain.main(args)
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
