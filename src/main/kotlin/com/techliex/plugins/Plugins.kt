package com.techliex.plugins

import com.techliex.presentation.dto.ApiResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.json.Json

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
}

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiResponse<Unit>(
                    success = false,
                    message = cause.localizedMessage ?: "Internal server error"
                )
            )
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(
                status,
                ApiResponse<Unit>(
                    success = false,
                    message = "Requested endpoint or resource not found"
                )
            )
        }
        status(HttpStatusCode.Unauthorized) { call, status ->
            call.respond(
                status,
                ApiResponse<Unit>(
                    success = false,
                    message = "Authentication required or token expired"
                )
            )
        }
    }
}

fun Application.configureCORS() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        anyHost()
    }
}
