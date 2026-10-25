package com.techliex.presentation.routes

import com.techliex.domain.usecase.auth.ChangePasswordUseCase
import com.techliex.domain.usecase.user.*
import com.techliex.presentation.dto.*
import com.techliex.security.PasswordUtils
import com.techliex.security.getUserPrincipal
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes(
    createUserUseCase: CreateUserUseCase,
    getAllUsersUseCase: GetAllUsersUseCase,
    getUserByUsernameUseCase: GetUserByUsernameUseCase,
    deactivateUserUseCase: DeactivateUserUseCase,
    activateUserUseCase: ActivateUserUseCase,
    resetUserPasswordUseCase: ResetUserPasswordUseCase,
    changePasswordUseCase: ChangePasswordUseCase,
    deleteUserPermanentlyUseCase: DeleteUserPermanentlyUseCase
) {
    route("/api/v1/users") {
        authenticate("auth-jwt") {

            // Admin only: create user
            post {
                val principal = call.getUserPrincipal()
                if (principal.role != "Admin") {
                    return@post call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, "Admin access required"))
                }

                val req = try {
                    call.receive<CreateUserRequest>()
                } catch (_: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid payload"))
                }

                val existingUser = getUserByUsernameUseCase(req.username)
                if (existingUser != null) {
                    return@post call.respond(HttpStatusCode.Conflict, ApiResponse<Unit>(false, "Username already exists"))
                }

                val passwordHash = PasswordUtils.hashPassword(req.password)
                val newUser = createUserUseCase(req.username, passwordHash, req.name, req.role)
                call.respond(HttpStatusCode.Created, ApiResponse(true, "User created successfully", newUser.toDto()))
            }

            // Get all active & inactive users
            get {
                val users = getAllUsersUseCase().map { it.toDto() }
                call.respond(HttpStatusCode.OK, ApiResponse(true, "Users retrieved", users))
            }

            // Change self password (oldPassword & newPassword)
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

            // Get user by username
            get("/{username}") {
                val username = call.parameters["username"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Username parameter missing"))

                val user = getUserByUsernameUseCase(username)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "User not found"))

                call.respond(HttpStatusCode.OK, ApiResponse(true, "User found", user.toDto()))
            }

            // Admin only: deactivate user
            delete("/{username}") {
                val principal = call.getUserPrincipal()
                if (principal.role != "Admin") {
                    return@delete call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, "Admin access required"))
                }

                val username = call.parameters["username"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Username parameter missing"))

                val success = deactivateUserUseCase(username)
                if (success) {
                    call.respond(HttpStatusCode.OK, ApiResponse<Unit>(true, "User deactivated successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "User not found"))
                }
            }

            // Admin only: delete user permanently from database
            delete("/{username}/permanent") {
                val principal = call.getUserPrincipal()
                if (principal.role != "Admin") {
                    return@delete call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, "Admin access required"))
                }

                val username = call.parameters["username"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Username parameter missing"))

                val success = deleteUserPermanentlyUseCase(username)
                if (success) {
                    call.respond(HttpStatusCode.OK, ApiResponse<Unit>(true, "User permanently deleted from database"))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "User not found"))
                }
            }

            // Admin only: re-activate user account
            patch("/{username}/activate") {
                val principal = call.getUserPrincipal()
                if (principal.role != "Admin") {
                    return@patch call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, "Admin access required"))
                }

                val username = call.parameters["username"]
                    ?: return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Username parameter missing"))

                val success = activateUserUseCase(username)
                if (success) {
                    call.respond(HttpStatusCode.OK, ApiResponse<Unit>(true, "User activated successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "User not found"))
                }
            }

            // Admin only: reset any user's password (forgot password override)
            patch("/{username}/reset-password") {
                val principal = call.getUserPrincipal()
                if (principal.role != "Admin") {
                    return@patch call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, "Admin access required"))
                }

                val username = call.parameters["username"]
                    ?: return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Username parameter missing"))

                val req = try {
                    call.receive<ResetPasswordRequest>()
                } catch (_: Exception) {
                    return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid payload"))
                }

                val newPasswordHash = PasswordUtils.hashPassword(req.newPassword)
                val success = resetUserPasswordUseCase(username, newPasswordHash)

                if (success) {
                    call.respond(HttpStatusCode.OK, ApiResponse<Unit>(true, "User password reset successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "User not found"))
                }
            }
        }
    }
}
