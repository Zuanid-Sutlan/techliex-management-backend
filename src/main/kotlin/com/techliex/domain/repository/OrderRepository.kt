package com.techliex.domain.repository

import com.techliex.domain.model.Order
import com.techliex.domain.model.RefundMessage

interface OrderRepository {
    suspend fun createOrder(
        userId: Long,
        orderNumber: String?,
        orderDate: String?,
        productId: Long,
        address: String,
        note: String?,
        quantity: Int,
        listingPrice: Double?,
        paymentScreenshot: String?
    ): Order

    suspend fun getOrdersForUser(userId: Long, userRole: String): List<Order>
    suspend fun getOrderById(orderId: Long): Order?
    suspend fun updateLogistics(orderId: Long, trackId: String, company: String): Order?
    suspend fun completeOrder(orderId: Long): Order?
    suspend fun updateOrderStatus(orderId: Long, status: String): Order?
    suspend fun openRefund(
        orderId: Long,
        userId: Long,
        userName: String,
        userRole: String,
        message: String?,
        screenshots: List<String>
    ): Order?
    suspend fun addRefundMessage(
        orderId: Long,
        senderId: Long,
        senderName: String,
        senderRole: String,
        message: String,
        screenshots: List<String>
    ): RefundMessage?
    suspend fun getRefundMessages(orderId: Long): List<RefundMessage>
    suspend fun deleteOrder(orderId: Long): Boolean
    suspend fun deleteOrderRefundMessages(orderId: Long): Boolean
}
