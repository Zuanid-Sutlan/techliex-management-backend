package com.techliex.presentation.routes

import com.techliex.domain.usecase.dashboard.GetDashboardStatsUseCase
import com.techliex.presentation.dto.*
import com.techliex.security.getUserPrincipal
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.dashboardRoutes(getDashboardStatsUseCase: GetDashboardStatsUseCase) {
    route("/api/v1/dashboard") {
        authenticate("auth-jwt") {
            get("/stats") {
                val principal = call.getUserPrincipal()
                val stats = getDashboardStatsUseCase(principal.id, principal.role)
                call.respond(HttpStatusCode.OK, ApiResponse(true, "Dashboard statistics retrieved", stats.toDto()))
            }
        }
    }
}
