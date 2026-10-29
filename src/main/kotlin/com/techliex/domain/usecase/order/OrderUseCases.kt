package com.techliex.domain.usecase.order

import com.techliex.domain.model.Order
import com.techliex.domain.model.RefundMessage
import com.techliex.domain.repository.OrderRepository

class CreateOrderUseCase(private val orderRepository: OrderRepository) {
    suspend operator fun invoke(
        userId: Long,
        orderNumber: String?,
        orderDate: String?,
        productId: Long?,
        productName: String? = null,
        productImage: String? = null,
        productSourcePrice: Double? = null,
        address: String,
        note: String?,
        quantity: Int,
        listingPrice: Double?,
        paymentScreenshot: String?
    ): Order {
        return orderRepository.createOrder(
            userId = userId,
            orderNumber = orderNumber,
            orderDate = orderDate,
            productId = productId,
            productName = productName,
            productImage = productImage,
            productSourcePrice = productSourcePrice,
            address = address,
            note = note,
            quantity = quantity,
            listingPrice = listingPrice,
            paymentScreenshot = paymentScreenshot
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

class UpdateOrderStatusUseCase(private val orderRepository: OrderRepository) {
    suspend operator fun invoke(orderId: Long, status: String): Order? {
        return orderRepository.updateOrderStatus(orderId, status)
    }
}

class OpenRefundUseCase(private val orderRepository: OrderRepository) {
    suspend operator fun invoke(
        orderId: Long,
        userId: Long,
        userName: String,
        userRole: String,
        message: String?,
        screenshots: List<String>
    ): Order? {
        return orderRepository.openRefund(orderId, userId, userName, userRole, message, screenshots)
    }
}

class SendRefundMessageUseCase(private val orderRepository: OrderRepository) {
    suspend operator fun invoke(
        orderId: Long,
        senderId: Long,
        senderName: String,
        senderRole: String,
        message: String,
        screenshots: List<String>
    ): RefundMessage? {
        return orderRepository.addRefundMessage(orderId, senderId, senderName, senderRole, message, screenshots)
    }
}

class GetRefundMessagesUseCase(private val orderRepository: OrderRepository) {
    suspend operator fun invoke(orderId: Long): List<RefundMessage> {
        return orderRepository.getRefundMessages(orderId)
    }
}

class DeleteOrderUseCase(private val orderRepository: OrderRepository) {
    suspend operator fun invoke(orderId: Long): Boolean {
        return orderRepository.deleteOrder(orderId)
    }
}

class DeleteOrderRefundMessagesUseCase(private val orderRepository: OrderRepository) {
    suspend operator fun invoke(orderId: Long): Boolean {
        return orderRepository.deleteOrderRefundMessages(orderId)
    }
}
