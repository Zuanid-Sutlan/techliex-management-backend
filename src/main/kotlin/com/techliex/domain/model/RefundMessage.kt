package com.techliex.domain.model

data class RefundMessage(
    val id: Long,
    val orderId: Long,
    val senderId: Long,
    val senderName: String,
    val senderRole: String,
    val message: String,
    val screenshots: List<String> = emptyList(),
    val createdAt: String
)
