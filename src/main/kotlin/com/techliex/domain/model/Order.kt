package com.techliex.domain.model

data class Order(
    val id: Long,
    val orderNumber: String,
    val orderDate: String,
    val userId: Long,
    val userName: String? = null,
    val productId: Long,
    val productName: String,
    val productImage: String,
    val productSourcePrice: Double,
    val productTitle: String? = null,
    val address: String,
    val note: String?,
    val variationNote: String?,
    val quantity: Int,
    val listingPrice: Double,
    val paymentScreenshot: String,
    val paymentImageUrl: String?,
    val trackId: String,
    val company: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String = ""
)
