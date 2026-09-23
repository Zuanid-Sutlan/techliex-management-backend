package com.techliex.domain.model

data class Product(
    val id: Long,
    val userId: Long,
    val creatorName: String? = null,
    val title: String,
    val description: String?,
    val productImageUrl: String,
    val sourceLink: String,
    val sourcePrice: Double,
    val referenceLink: String,
    val referencePrice: Double,
    val warehousePrice: Double,
    val warehouseNote: String?,
    val sharedUsernames: List<String> = emptyList(),
    val createdAt: String
)
