package com.techliex.domain.usecase.product

import com.techliex.domain.model.Product
import com.techliex.domain.repository.ProductRepository

class CreateProductUseCase(private val productRepository: ProductRepository) {
    suspend operator fun invoke(
        creatorId: Long,
        title: String,
        description: String?,
        productImageUrl: String,
        sourceLink: String,
        sourcePrice: Double,
        referenceLink: String,
        referencePrice: Double,
        shareWithUsernames: List<String>
    ): Product {
        return productRepository.createProduct(
            creatorId, title, description, productImageUrl,
            sourceLink, sourcePrice, referenceLink, referencePrice, shareWithUsernames
        )
    }
}

class GetProductsUseCase(private val productRepository: ProductRepository) {
    suspend operator fun invoke(userId: Long, userRole: String): List<Product> {
        return productRepository.getProductsForUser(userId, userRole)
    }
}

class GetProductByIdUseCase(private val productRepository: ProductRepository) {
    suspend operator fun invoke(productId: Long): Product? {
        return productRepository.getProductById(productId)
    }
}

class UpdateWarehouseUseCase(private val productRepository: ProductRepository) {
    suspend operator fun invoke(productId: Long, warehousePrice: Double, warehouseNote: String?): Product? {
        return productRepository.updateWarehouse(productId, warehousePrice, warehouseNote)
    }
}

class DeleteProductUseCase(private val productRepository: ProductRepository) {
    suspend operator fun invoke(productId: Long): Boolean {
        return productRepository.deleteProduct(productId)
    }
}
