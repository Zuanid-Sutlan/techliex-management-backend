package com.techliex.data.repository

import com.techliex.data.db.*
import com.techliex.domain.model.Product
import com.techliex.domain.repository.ProductRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.time.Instant

class ProductRepositoryImpl : ProductRepository {

    override suspend fun createProduct(
        creatorId: Long,
        title: String,
        description: String?,
        productImageUrl: String,
        sourceLink: String,
        sourcePrice: Double,
        referenceLink: String,
        referencePrice: Double,
        shareWithUsernames: List<String>
    ): Product = DatabaseFactory.dbQuery {
        val now = Instant.now().toString()
        val generatedId = ProductsTable.insert {
            it[userId] = creatorId
            it[this.title] = title
            it[this.description] = description
            it[this.productImageUrl] = productImageUrl
            it[this.sourceLink] = sourceLink
            it[this.sourcePrice] = BigDecimal.valueOf(sourcePrice)
            it[this.referenceLink] = referenceLink
            it[this.referencePrice] = BigDecimal.valueOf(referencePrice)
            it[warehousePrice] = BigDecimal.ZERO
            it[warehouseNote] = null
            it[createdAt] = now
        } get ProductsTable.id

        if (shareWithUsernames.isNotEmpty()) {
            val sharedUserIds = UsersTable.selectAll()
                .where { UsersTable.username inList shareWithUsernames }
                .map { it[UsersTable.id] }

            for (targetUserId in sharedUserIds) {
                ProductSharesTable.insert {
                    it[productId] = generatedId
                    it[sharedUserId] = targetUserId
                }
            }
        }

        val creatorName = UsersTable.selectAll()
            .where { UsersTable.id eq creatorId }
            .map { it[UsersTable.name] }
            .singleOrNull()

        Product(
            id = generatedId,
            userId = creatorId,
            creatorName = creatorName,
            title = title,
            description = description,
            productImageUrl = productImageUrl,
            sourceLink = sourceLink,
            sourcePrice = sourcePrice,
            referenceLink = referenceLink,
            referencePrice = referencePrice,
            warehousePrice = 0.0,
            warehouseNote = null,
            sharedUsernames = shareWithUsernames,
            createdAt = now
        )
    }

    override suspend fun getProductsForUser(userId: Long, userRole: String): List<Product> = DatabaseFactory.dbQuery {
        val productIds = if (userRole == "Admin" || userRole == "Warehouse") {
            ProductsTable.selectAll().map { it[ProductsTable.id] }
        } else {
            val ownProductIds = ProductsTable.selectAll()
                .where { ProductsTable.userId eq userId }
                .map { it[ProductsTable.id] }

            val sharedProductIds = ProductSharesTable.selectAll()
                .where { ProductSharesTable.sharedUserId eq userId }
                .map { it[ProductSharesTable.productId] }

            (ownProductIds + sharedProductIds).distinct()
        }

        if (productIds.isEmpty()) return@dbQuery emptyList()

        ProductsTable.selectAll()
            .where { ProductsTable.id inList productIds }
            .map { rowToProduct(it) }
    }

    override suspend fun getProductById(productId: Long): Product? = DatabaseFactory.dbQuery {
        ProductsTable.selectAll()
            .where { ProductsTable.id eq productId }
            .map { rowToProduct(it) }
            .singleOrNull()
    }

    override suspend fun updateWarehouse(productId: Long, warehousePrice: Double, warehouseNote: String?): Product? = DatabaseFactory.dbQuery {
        val updatedRows = ProductsTable.update({ ProductsTable.id eq productId }) {
            it[ProductsTable.warehousePrice] = BigDecimal.valueOf(warehousePrice)
            it[ProductsTable.warehouseNote] = warehouseNote
        }

        if (updatedRows > 0) getProductByIdInternal(productId) else null
    }

    override suspend fun deleteProduct(productId: Long): Boolean = DatabaseFactory.dbQuery {
        ProductsTable.deleteWhere { ProductsTable.id eq productId } > 0
    }

    private fun getProductByIdInternal(productId: Long): Product? {
        return ProductsTable.selectAll()
            .where { ProductsTable.id eq productId }
            .map { rowToProduct(it) }
            .singleOrNull()
    }

    private fun rowToProduct(row: ResultRow): Product {
        val productId = row[ProductsTable.id]
        val creatorId = row[ProductsTable.userId]

        val creatorName = UsersTable.selectAll()
            .where { UsersTable.id eq creatorId }
            .map { it[UsersTable.name] }
            .singleOrNull()

        val sharedUsernames = (ProductSharesTable innerJoin UsersTable)
            .selectAll()
            .where { ProductSharesTable.productId eq productId }
            .map { it[UsersTable.username] }

        return Product(
            id = productId,
            userId = creatorId,
            creatorName = creatorName,
            title = row[ProductsTable.title],
            description = row[ProductsTable.description],
            productImageUrl = row[ProductsTable.productImageUrl],
            sourceLink = row[ProductsTable.sourceLink],
            sourcePrice = row[ProductsTable.sourcePrice].toDouble(),
            referenceLink = row[ProductsTable.referenceLink],
            referencePrice = row[ProductsTable.referencePrice].toDouble(),
            warehousePrice = row[ProductsTable.warehousePrice].toDouble(),
            warehouseNote = row[ProductsTable.warehouseNote],
            sharedUsernames = sharedUsernames,
            createdAt = row[ProductsTable.createdAt]
        )
    }
}
