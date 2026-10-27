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
    getRefundMessagesUseCase: GetRefundMessagesUseCase,
    deleteOrderUseCase: DeleteOrderUseCase,
    deleteOrderRefundMessagesUseCase: DeleteOrderRefundMessagesUseCase
) {
    route("/api/v1/orders") {
        authenticate("auth-jwt") {

            // Create new order(s) (supports single or multi-item orders)
            post {
                val principal = call.getUserPrincipal()
                val req = try {
                    call.receive<CreateOrderRequest>()
                } catch (_: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid payload"))
                }

                val rawItems = when {
                    req.items.isNotEmpty() -> req.items
                    req.orders.isNotEmpty() -> req.orders
                    req.productId != null || !req.productName.isNullOrBlank() || !req.productTitle.isNullOrBlank() -> listOf(
                        CreateOrderItemRequest(
                            productId = req.productId,
                            productName = req.productName ?: req.productTitle,
                            productTitle = req.productTitle ?: req.productName,
                            productImage = req.productImage,
                            productSourcePrice = req.productSourcePrice,
                            quantity = req.quantity,
                            note = req.note ?: req.variationNote,
                            variationNote = req.variationNote ?: req.note,
                            listingPrice = req.listingPrice
                        )
                    )
                    else -> emptyList()
                }

                if (rawItems.isEmpty()) {
                    return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "At least one product item must be specified"))
                }

                try {
                    val createdOrders = rawItems.map { item ->
                        createOrderUseCase(
                            userId = principal.id,
                            orderNumber = req.orderNumber,
                            orderDate = req.orderDate,
                            productId = item.productId,
                            productName = item.productName ?: item.productTitle,
                            productImage = item.productImage,
                            productSourcePrice = item.productSourcePrice,
                            address = req.address,
                            note = item.note ?: item.variationNote,
                            quantity = item.quantity,
                            listingPrice = item.listingPrice,
                            paymentScreenshot = req.paymentScreenshot ?: req.paymentImageUrl
                        )
                    }

                    if (createdOrders.size == 1) {
                        call.respond(HttpStatusCode.Created, ApiResponse(true, "Order placed successfully", createdOrders.first().toDto()))
                    } else {
                        call.respond(HttpStatusCode.Created, ApiResponse(true, "${createdOrders.size} orders placed successfully", createdOrders.map { it.toDto() }))
                    }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, e.message ?: "Failed to place order"))
                }
            }

            // Batch create multiple orders
            post("/batch") {
                val principal = call.getUserPrincipal()
                val req = try {
                    call.receive<CreateOrderRequest>()
                } catch (_: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid payload"))
                }

                val rawItems = when {
                    req.items.isNotEmpty() -> req.items
                    req.orders.isNotEmpty() -> req.orders
                    else -> emptyList()
                }

                if (rawItems.isEmpty()) {
                    return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "No order items provided in batch request"))
                }

                try {
                    val createdOrders = rawItems.map { item ->
                        createOrderUseCase(
                            userId = principal.id,
                            orderNumber = req.orderNumber,
                            orderDate = req.orderDate,
                            productId = item.productId,
                            productName = item.productName ?: item.productTitle,
                            productImage = item.productImage,
                            productSourcePrice = item.productSourcePrice,
                            address = req.address,
                            note = item.note ?: item.variationNote,
                            quantity = item.quantity,
                            listingPrice = item.listingPrice,
                            paymentScreenshot = req.paymentScreenshot ?: req.paymentImageUrl
                        )
                    }
                    call.respond(HttpStatusCode.Created, ApiResponse(true, "${createdOrders.size} orders placed successfully", createdOrders.map { it.toDto() }))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, e.message ?: "Failed to place batch orders"))
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

            // Admin or order owner: delete order permanently (and its chats)
            delete("/{id}") {
                val principal = call.getUserPrincipal()
                val orderId = call.parameters["id"]?.toLongOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid order ID"))

                val existingOrder = getOrderByIdUseCase(orderId)
                    ?: return@delete call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "Order not found"))

                if (principal.role != "Admin" && existingOrder.userId != principal.id) {
                    return@delete call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, "Access denied"))
                }

                val deleted = deleteOrderUseCase(orderId)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, ApiResponse<Unit>(true, "Order and associated chats deleted permanently"))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "Order not found"))
                }
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

            // Admin or order owner: delete all refund chat messages against the order
            delete("/{id}/refund/messages") {
                val principal = call.getUserPrincipal()
                val orderId = call.parameters["id"]?.toLongOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid order ID"))

                val existingOrder = getOrderByIdUseCase(orderId)
                    ?: return@delete call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "Order not found"))

                if (principal.role != "Admin" && existingOrder.userId != principal.id) {
                    return@delete call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, "Access denied"))
                }

                val deleted = deleteOrderRefundMessagesUseCase(orderId)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, ApiResponse<Unit>(true, "All refund chat messages deleted successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "Order not found"))
                }
            }
        }
    }
}
