package com.techliex.presentation.routes

import com.techliex.data.db.DatabaseFactory
import com.techliex.data.db.table.OrderStatusesTable
import com.techliex.domain.usecase.system.GetAppVersionUseCase
import com.techliex.presentation.dto.ApiResponse
import com.techliex.presentation.dto.CreateStatusRequest
import com.techliex.presentation.dto.HealthCheckDto
import com.techliex.presentation.dto.StatusDto
import com.techliex.presentation.dto.toDto
import com.techliex.security.getUserPrincipal
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
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

    // Custom Order Statuses Management
    route("/api/v1/statuses") {
        get {
            val statuses = DatabaseFactory.dbQuery {
                OrderStatusesTable.selectAll().map {
                    StatusDto(it[OrderStatusesTable.id], it[OrderStatusesTable.statusName])
                }
            }
            call.respond(HttpStatusCode.OK, ApiResponse(true, "Statuses retrieved", statuses))
        }

        authenticate("auth-jwt") {
            post {
                val principal = call.getUserPrincipal()
                if (principal.role != "Admin") {
                    return@post call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, "Admin access required"))
                }
                val req = try {
                    call.receive<CreateStatusRequest>()
                } catch (_: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid payload"))
                }

                val newId = DatabaseFactory.dbQuery {
                    val existing = OrderStatusesTable.selectAll().where { OrderStatusesTable.statusName eq req.statusName }.count()
                    if (existing > 0) return@dbQuery -1L
                    OrderStatusesTable.insert {
                        it[statusName] = req.statusName
                    }[OrderStatusesTable.id]
                }

                if (newId == -1L) {
                    return@post call.respond(HttpStatusCode.Conflict, ApiResponse<Unit>(false, "Status already exists"))
                }

                call.respond(HttpStatusCode.Created, ApiResponse(true, "Status created successfully", StatusDto(newId, req.statusName)))
            }

            delete("/{id}") {
                val principal = call.getUserPrincipal()
                if (principal.role != "Admin") {
                    return@delete call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, "Admin access required"))
                }
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid ID"))

                val deleted = DatabaseFactory.dbQuery {
                    OrderStatusesTable.deleteWhere { OrderStatusesTable.id eq id } > 0
                }

                if (deleted) {
                    call.respond(HttpStatusCode.OK, ApiResponse<Unit>(true, "Status deleted successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "Status not found"))
                }
            }
        }
    }
}
