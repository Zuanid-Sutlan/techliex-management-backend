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
        sharedUserIds: List<Long> = emptyList(),
        shareWithUsernames: List<String> = emptyList()
    ): Product {
        return productRepository.createProduct(
            creatorId, title, description, productImageUrl,
            sourceLink, sourcePrice, referenceLink, referencePrice,
            sharedUserIds, shareWithUsernames
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

class UpdateProductUseCase(private val productRepository: ProductRepository) {
    suspend operator fun invoke(
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
    ): Product? {
        return productRepository.updateProduct(
            productId, title, description, productImageUrl,
            sourceLink, sourcePrice, referenceLink, referencePrice,
            sharedUserIds, shareWithUsernames
        )
    }
}

class UpdateWarehouseUseCase(private val productRepository: ProductRepository) {
    suspend operator fun invoke(productId: Long, warehousePrice: Double, warehouseNote: String?): Product? {
        return productRepository.updateWarehouse(productId, warehousePrice, warehouseNote)
    }
}

class UpdateProductSharesUseCase(private val productRepository: ProductRepository) {
    suspend operator fun invoke(
        productId: Long,
        sharedUserIds: List<Long> = emptyList(),
        shareWithUsernames: List<String> = emptyList()
    ): Product? {
        return productRepository.updateProductShares(productId, sharedUserIds, shareWithUsernames)
    }
}

class DeleteProductUseCase(private val productRepository: ProductRepository) {
    suspend operator fun invoke(productId: Long): Boolean {
        return productRepository.deleteProduct(productId)
    }
}
