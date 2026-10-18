package com.techliex.presentation.routes

import com.techliex.domain.usecase.auth.ChangePasswordUseCase
import com.techliex.domain.usecase.auth.GetUserProfileUseCase
import com.techliex.domain.usecase.auth.LoginUseCase
import com.techliex.presentation.dto.*
import com.techliex.security.JwtConfig
import com.techliex.security.PasswordUtils
import com.techliex.security.getUserPrincipal
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(
    loginUseCase: LoginUseCase,
    getUserProfileUseCase: GetUserProfileUseCase,
    changePasswordUseCase: ChangePasswordUseCase
) {
    route("/api/v1/auth") {

        post("/login") {
            val req = try {
                call.receive<LoginRequest>()
            } catch (_: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid payload"))
            }

            val result = loginUseCase(req.username)
            if (result == null) {
                return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse<Unit>(false, "Invalid username or password"))
            }

            val (user, storedHash) = result
            if (!PasswordUtils.verifyPassword(req.password, storedHash)) {
                return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse<Unit>(false, "Invalid username or password"))
            }

            val userDto = user.toDto()
            val token = JwtConfig.generateToken(userDto)
            call.respond(HttpStatusCode.OK, ApiResponse(true, "Login successful", LoginResponse(token, userDto)))
        }

        authenticate("auth-jwt") {
            get("/me") {
                val principal = call.getUserPrincipal()
                val user = getUserProfileUseCase(principal.id)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "User not found"))

                call.respond(HttpStatusCode.OK, ApiResponse(true, "User profile retrieved", user.toDto()))
            }

            patch("/change-password") {
                val principal = call.getUserPrincipal()
                val req = try {
                    call.receive<ChangePasswordRequest>()
                } catch (_: Exception) {
                    return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid payload"))
                }

                val newPasswordHash = PasswordUtils.hashPassword(req.newPassword)
                val success = changePasswordUseCase(
                    userId = principal.id,
                    username = principal.username,
                    verifyOldPassword = { hash -> PasswordUtils.verifyPassword(req.oldPassword, hash) },
                    newPasswordHash = newPasswordHash
                )

                if (success) {
                    call.respond(HttpStatusCode.OK, ApiResponse<Unit>(true, "Password changed successfully"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Current password is incorrect"))
                }
            }
        }
    }
}
