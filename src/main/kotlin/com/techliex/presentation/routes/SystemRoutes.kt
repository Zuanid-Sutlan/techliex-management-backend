package com.techliex.presentation.routes

import com.techliex.domain.usecase.system.GetAppVersionUseCase
import com.techliex.presentation.dto.ApiResponse
import com.techliex.presentation.dto.toDto
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.systemRoutes(getAppVersionUseCase: GetAppVersionUseCase) {
    route("/api/v1/version") {
        get {
            val appVersion = getAppVersionUseCase()
            call.respond(HttpStatusCode.OK, ApiResponse(true, "App version retrieved successfully", appVersion.toDto()))
        }
    }
}
