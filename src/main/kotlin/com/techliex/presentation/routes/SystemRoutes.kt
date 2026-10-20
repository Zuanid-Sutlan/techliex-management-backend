package com.techliex.presentation.routes

import com.techliex.data.db.DatabaseFactory
import com.techliex.domain.usecase.system.GetAppVersionUseCase
import com.techliex.presentation.dto.ApiResponse
import com.techliex.presentation.dto.HealthCheckDto
import com.techliex.presentation.dto.toDto
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.Instant

fun Route.systemRoutes(getAppVersionUseCase: GetAppVersionUseCase) {
    route("/api/v1/version") {
        get {
            val appVersion = getAppVersionUseCase()
            call.respond(HttpStatusCode.OK, ApiResponse(true, "App version retrieved successfully", appVersion.toDto()))
        }
    }

    route("/api/v1/health") {
        get {
            val dbHealth = DatabaseFactory.checkHealth()
            val healthDto = HealthCheckDto(
                serverStatus = "UP",
                dbStatus = if (dbHealth.isConnected) "CONNECTED" else "DISCONNECTED",
                databaseType = dbHealth.databaseType,
                dbMessage = dbHealth.message,
                timestamp = Instant.now().toString()
            )

            if (dbHealth.isConnected) {
                call.respond(HttpStatusCode.OK, ApiResponse(true, "Server and Database are healthy", healthDto))
            } else {
                call.respond(HttpStatusCode.ServiceUnavailable, ApiResponse(false, "Database connection error", healthDto))
            }
        }
    }

    // Top-level health endpoint for Dokploy / Docker container health checks
    get("/health") {
        val dbHealth = DatabaseFactory.checkHealth()
        val healthDto = HealthCheckDto(
            serverStatus = "UP",
            dbStatus = if (dbHealth.isConnected) "CONNECTED" else "DISCONNECTED",
            databaseType = dbHealth.databaseType,
            dbMessage = dbHealth.message,
            timestamp = Instant.now().toString()
        )

        if (dbHealth.isConnected) {
            call.respond(HttpStatusCode.OK, ApiResponse(true, "Server and Database are healthy", healthDto))
        } else {
            call.respond(HttpStatusCode.ServiceUnavailable, ApiResponse(false, "Database connection error", healthDto))
        }
    }
}
