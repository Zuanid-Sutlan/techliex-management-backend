package com.techliex.presentation.routes

import com.techliex.domain.usecase.order.*
import com.techliex.presentation.dto.*
import com.techliex.security.getUserPrincipal
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.orderRoutes(
    createOrderUseCase: CreateOrderUseCase,
    getOrdersUseCase: GetOrdersUseCase,
    getOrderByIdUseCase: GetOrderByIdUseCase,
    updateLogisticsUseCase: UpdateLogisticsUseCase,
    completeOrderUseCase: CompleteOrderUseCase
) {
    route("/api/v1/orders") {
        authenticate("auth-jwt") {

            // Create new order
            post {
                val principal = call.getUserPrincipal()
                val req = try {
                    call.receive<CreateOrderRequest>()
                } catch (_: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid payload"))
                }

                val order = createOrderUseCase(
                    userId = principal.id,
                    orderDate = req.orderDate,
                    productId = req.productId,
                    address = req.address,
                    variationNote = req.variationNote,
                    quantity = req.quantity,
                    listingPrice = req.listingPrice,
                    paymentImageUrl = req.paymentImageUrl
                )
                call.respond(HttpStatusCode.Created, ApiResponse(true, "Order placed successfully", order.toDto()))
            }

            // Get orders scoped by role
            get {
                val principal = call.getUserPrincipal()
                val orders = getOrdersUseCase(principal.id, principal.role).map { it.toDto() }
                call.respond(HttpStatusCode.OK, ApiResponse(true, "Orders retrieved", orders))
            }

            // Get order by ID
            get("/{id}") {
                val orderId = call.parameters["id"]?.toLongOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid order ID"))

                val order = getOrderByIdUseCase(orderId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "Order not found"))

                call.respond(HttpStatusCode.OK, ApiResponse(true, "Order details retrieved", order.toDto()))
            }

            // Admin only: update logistics (trackId, company) -> sets status to "Shipped"
            patch("/{id}/logistics") {
                val principal = call.getUserPrincipal()
                if (principal.role != "Admin") {
                    return@patch call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, "Admin role required"))
                }

                val orderId = call.parameters["id"]?.toLongOrNull()
                    ?: return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid order ID"))

                val req = try {
                    call.receive<UpdateLogisticsRequest>()
                } catch (_: Exception) {
                    return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid payload"))
                }

                val updatedOrder = updateLogisticsUseCase(orderId, req.trackId, req.company)
                    ?: return@patch call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "Order not found"))

                call.respond(HttpStatusCode.OK, ApiResponse(true, "Logistics updated and order shipped", updatedOrder.toDto()))
            }

            // Mark order as Completed
            patch("/{id}/complete") {
                val orderId = call.parameters["id"]?.toLongOrNull()
                    ?: return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid order ID"))

                val completedOrder = completeOrderUseCase(orderId)
                    ?: return@patch call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "Order not found"))

                call.respond(HttpStatusCode.OK, ApiResponse(true, "Order finalized and completed", completedOrder.toDto()))
            }
        }
    }
}
