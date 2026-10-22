package com.techliex.domain.repository

import com.techliex.domain.model.Product

interface ProductRepository {
    suspend fun createProduct(
        creatorId: Long,
        title: String,
        description: String?,
        productImageUrl: String,
        sourceLink: String,
        sourcePrice: Double,
        referenceLink: String,
        referencePrice: Double,
        sharedUserIds: List<Long> = emptyList(),
        shareWithUsernames: List<String> = emptyList()
    ): Product
    suspend fun getProductsForUser(userId: Long, userRole: String): List<Product>
    suspend fun getProductById(productId: Long): Product?
    suspend fun updateProduct(
        productId: Long,
        title: String? = null,
        description: String? = null,
        productImageUrl: String? = null,
        sourceLink: String? = null,
        sourcePrice: Double? = null,
        referenceLink: String? = null,
        referencePrice: Double? = null,
        sharedUserIds: List<Long>? = null,
        shareWithUsernames: List<String>? = null
    ): Product?
    suspend fun updateWarehouse(productId: Long, warehousePrice: Double, warehouseNote: String?): Product?
    suspend fun updateProductShares(
        productId: Long,
        sharedUserIds: List<Long> = emptyList(),
        shareWithUsernames: List<String> = emptyList()
    ): Product?
    suspend fun deleteProduct(productId: Long): Boolean
}
