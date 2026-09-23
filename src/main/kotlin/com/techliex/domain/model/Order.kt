package com.techliex.domain.model

data class Order(
    val id: Long,
    val orderDate: String,
    val userId: Long,
    val userName: String? = null,
    val productId: Long,
    val productTitle: String? = null,
    val address: String,
    val variationNote: String?,
    val quantity: Int,
    val listingPrice: Double,
    val paymentImageUrl: String,
    val trackId: String,
    val company: String,
    val status: String,
    val createdAt: String
)
