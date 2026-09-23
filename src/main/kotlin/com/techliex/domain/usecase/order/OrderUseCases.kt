package com.techliex.domain.usecase.order

import com.techliex.domain.model.Order
import com.techliex.domain.repository.OrderRepository

class CreateOrderUseCase(private val orderRepository: OrderRepository) {
    suspend operator fun invoke(
        userId: Long,
        orderDate: String,
        productId: Long,
        address: String,
        variationNote: String?,
        quantity: Int,
        listingPrice: Double,
        paymentImageUrl: String
    ): Order {
        return orderRepository.createOrder(
            userId, orderDate, productId, address,
            variationNote, quantity, listingPrice, paymentImageUrl
        )
    }
}

class GetOrdersUseCase(private val orderRepository: OrderRepository) {
    suspend operator fun invoke(userId: Long, userRole: String): List<Order> {
        return orderRepository.getOrdersForUser(userId, userRole)
    }
}

class GetOrderByIdUseCase(private val orderRepository: OrderRepository) {
    suspend operator fun invoke(orderId: Long): Order? {
        return orderRepository.getOrderById(orderId)
    }
}

class UpdateLogisticsUseCase(private val orderRepository: OrderRepository) {
    suspend operator fun invoke(orderId: Long, trackId: String, company: String): Order? {
        return orderRepository.updateLogistics(orderId, trackId, company)
    }
}

class CompleteOrderUseCase(private val orderRepository: OrderRepository) {
    suspend operator fun invoke(orderId: Long): Order? {
        return orderRepository.completeOrder(orderId)
    }
}
