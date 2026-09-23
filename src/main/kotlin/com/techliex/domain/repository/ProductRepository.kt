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
        shareWithUsernames: List<String>
    ): Product
    suspend fun getProductsForUser(userId: Long, userRole: String): List<Product>
    suspend fun getProductById(productId: Long): Product?
    suspend fun updateWarehouse(productId: Long, warehousePrice: Double, warehouseNote: String?): Product?
    suspend fun deleteProduct(productId: Long): Boolean
}
