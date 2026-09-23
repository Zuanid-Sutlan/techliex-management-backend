package com.techliex.domain.repository

import com.techliex.domain.model.Order

interface OrderRepository {
    suspend fun createOrder(
        userId: Long,
        orderDate: String,
        productId: Long,
        address: String,
        variationNote: String?,
        quantity: Int,
        listingPrice: Double,
        paymentImageUrl: String
    ): Order
    suspend fun getOrdersForUser(userId: Long, userRole: String): List<Order>
    suspend fun getOrderById(orderId: Long): Order?
    suspend fun updateLogistics(orderId: Long, trackId: String, company: String): Order?
    suspend fun completeOrder(orderId: Long): Order?
}
