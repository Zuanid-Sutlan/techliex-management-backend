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
    completeOrderUseCase: CompleteOrderUseCase,
    updateOrderStatusUseCase: UpdateOrderStatusUseCase,
    openRefundUseCase: OpenRefundUseCase,
    sendRefundMessageUseCase: SendRefundMessageUseCase,
    getRefundMessagesUseCase: GetRefundMessagesUseCase
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

                try {
                    val order = createOrderUseCase(
                        userId = principal.id,
                        orderNumber = req.orderNumber,
                        orderDate = req.orderDate,
                        productId = req.productId,
                        address = req.address,
                        note = req.note ?: req.variationNote,
                        quantity = req.quantity,
                        listingPrice = req.listingPrice,
                        paymentScreenshot = req.paymentScreenshot ?: req.paymentImageUrl
                    )
                    call.respond(HttpStatusCode.Created, ApiResponse(true, "Order placed successfully", order.toDto()))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, e.message ?: "Failed to place order"))
                }
            }

            // Get orders scoped by role
            get {
                val principal = call.getUserPrincipal()
                val orders = getOrdersUseCase(principal.id, principal.role).map { it.toDto() }
                call.respond(HttpStatusCode.OK, ApiResponse(true, "Orders retrieved", orders))
            }

            // Get order by ID
            get("/{id}") {
                val principal = call.getUserPrincipal()
                val orderId = call.parameters["id"]?.toLongOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid order ID"))

                val order = getOrderByIdUseCase(orderId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "Order not found"))

                if (principal.role != "Admin" && principal.role != "Warehouse" && order.userId != principal.id) {
                    return@get call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, "Access denied"))
                }

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

            // Update custom status text
            patch("/{id}/status") {
                val principal = call.getUserPrincipal()
                val orderId = call.parameters["id"]?.toLongOrNull()
                    ?: return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid order ID"))

                val existingOrder = getOrderByIdUseCase(orderId)
                    ?: return@patch call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "Order not found"))

                if (principal.role != "Admin" && principal.role != "Warehouse" && existingOrder.userId != principal.id) {
                    return@patch call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, "Access denied"))
                }

                val req = try {
                    call.receive<UpdateOrderStatusRequest>()
                } catch (_: Exception) {
                    return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid payload"))
                }

                val updatedOrder = updateOrderStatusUseCase(orderId, req.status)
                    ?: return@patch call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "Order not found"))

                call.respond(HttpStatusCode.OK, ApiResponse(true, "Order status updated", updatedOrder.toDto()))
            }

            // Open Refund on Order
            post("/{id}/refund") {
                val principal = call.getUserPrincipal()
                val orderId = call.parameters["id"]?.toLongOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid order ID"))

                val existingOrder = getOrderByIdUseCase(orderId)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "Order not found"))

                if (principal.role != "Admin" && principal.role != "Warehouse" && existingOrder.userId != principal.id) {
                    return@post call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, "Access denied"))
                }

                val req = try {
                    call.receive<OpenRefundRequest>()
                } catch (_: Exception) {
                    OpenRefundRequest()
                }

                val updatedOrder = openRefundUseCase(
                    orderId = orderId,
                    userId = principal.id,
                    userName = principal.username,
                    userRole = principal.role,
                    message = req.message,
                    screenshots = req.screenshots
                ) ?: return@post call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "Order not found"))

                call.respond(HttpStatusCode.OK, ApiResponse(true, "Refund initiated successfully", updatedOrder.toDto()))
            }

            // Send Refund Chat Message / Screenshot
            post("/{id}/refund/messages") {
                val principal = call.getUserPrincipal()
                val orderId = call.parameters["id"]?.toLongOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid order ID"))

                val existingOrder = getOrderByIdUseCase(orderId)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "Order not found"))

                if (principal.role != "Admin" && principal.role != "Warehouse" && existingOrder.userId != principal.id) {
                    return@post call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, "Access denied"))
                }

                val req = try {
                    call.receive<SendRefundMessageRequest>()
                } catch (_: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid payload"))
                }

                val message = sendRefundMessageUseCase(
                    orderId = orderId,
                    senderId = principal.id,
                    senderName = principal.username,
                    senderRole = principal.role,
                    message = req.message,
                    screenshots = req.screenshots
                ) ?: return@post call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(false, "Failed to send message"))

                call.respond(HttpStatusCode.Created, ApiResponse(true, "Refund message sent", message.toDto()))
            }

            // Get Refund Chat Messages
            get("/{id}/refund/messages") {
                val principal = call.getUserPrincipal()
                val orderId = call.parameters["id"]?.toLongOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid order ID"))

                val existingOrder = getOrderByIdUseCase(orderId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "Order not found"))

                if (principal.role != "Admin" && principal.role != "Warehouse" && existingOrder.userId != principal.id) {
                    return@get call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, "Access denied"))
                }

                val messages = getRefundMessagesUseCase(orderId).map { it.toDto() }
                call.respond(HttpStatusCode.OK, ApiResponse(true, "Refund messages retrieved", messages))
            }
        }
    }
}
