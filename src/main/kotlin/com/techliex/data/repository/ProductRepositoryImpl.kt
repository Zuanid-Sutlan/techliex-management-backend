package com.techliex.data.repository

import com.techliex.data.db.*
import com.techliex.data.db.table.ProductsTable
import com.techliex.data.db.table.UsersTable
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
        sharedUserIds: List<Long>,
        shareWithUsernames: List<String>
    ): Product = DatabaseFactory.dbQuery {
        val now = Instant.now().toString()
        val (finalUserIds, finalUsernames) = resolveShares(sharedUserIds, shareWithUsernames)

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
            it[ProductsTable.sharedUserIds] = finalUserIds.joinToString(",")
            it[ProductsTable.sharedUsernames] = finalUsernames.joinToString(",")
            it[createdAt] = now
        } get ProductsTable.id

        getProductByIdInternal(generatedId)!!
    }

    override suspend fun getProductsForUser(userId: Long, userRole: String): List<Product> = DatabaseFactory.dbQuery {
        val allProducts = ProductsTable.selectAll().map { rowToProduct(it) }

        if (userRole == "Admin" || userRole == "Warehouse") {
            allProducts
        } else {
            allProducts.filter { product ->
                product.userId == userId || userId in product.sharedUserIds
            }
        }
    }

    override suspend fun getProductById(productId: Long): Product? = DatabaseFactory.dbQuery {
        getProductByIdInternal(productId)
    }

    override suspend fun updateProduct(
        productId: Long,
        title: String?,
        description: String?,
        productImageUrl: String?,
        sourceLink: String?,
        sourcePrice: Double?,
        referenceLink: String?,
        referencePrice: Double?,
        sharedUserIds: List<Long>?,
        shareWithUsernames: List<String>?
    ): Product? = DatabaseFactory.dbQuery {
        getProductByIdInternal(productId) ?: return@dbQuery null

        val (finalUserIds, finalUsernames) = if (sharedUserIds != null || shareWithUsernames != null) {
            resolveShares(sharedUserIds ?: emptyList(), shareWithUsernames ?: emptyList())
        } else Pair(null, null)

        ProductsTable.update({ ProductsTable.id eq productId }) {
            if (title != null) it[ProductsTable.title] = title
            if (description != null) it[ProductsTable.description] = description
            if (productImageUrl != null) it[ProductsTable.productImageUrl] = productImageUrl
            if (sourceLink != null) it[ProductsTable.sourceLink] = sourceLink
            if (sourcePrice != null) it[ProductsTable.sourcePrice] = BigDecimal.valueOf(sourcePrice)
            if (referenceLink != null) it[ProductsTable.referenceLink] = referenceLink
            if (referencePrice != null) it[ProductsTable.referencePrice] = BigDecimal.valueOf(referencePrice)
            if (finalUserIds != null) it[ProductsTable.sharedUserIds] = finalUserIds.joinToString(",")
            if (finalUsernames != null) it[ProductsTable.sharedUsernames] = finalUsernames.joinToString(",")
        }

        getProductByIdInternal(productId)
    }

    override suspend fun updateWarehouse(
        productId: Long,
        warehousePrice: Double,
        warehouseNote: String?,
        warehouseImageUrl: String?
    ): Product? = DatabaseFactory.dbQuery {
        val updatedRows = ProductsTable.update({ ProductsTable.id eq productId }) {
            it[ProductsTable.warehousePrice] = BigDecimal.valueOf(warehousePrice)
            it[ProductsTable.warehouseNote] = warehouseNote
            if (warehouseImageUrl != null) {
                it[ProductsTable.warehouseImageUrl] = warehouseImageUrl
            }
        }

        if (updatedRows > 0) getProductByIdInternal(productId) else null
    }

    override suspend fun updateProductShares(
        productId: Long,
        sharedUserIds: List<Long>,
        shareWithUsernames: List<String>
    ): Product? = DatabaseFactory.dbQuery {
        getProductByIdInternal(productId) ?: return@dbQuery null
        val (finalUserIds, finalUsernames) = resolveShares(sharedUserIds, shareWithUsernames)

        ProductsTable.update({ ProductsTable.id eq productId }) {
            it[ProductsTable.sharedUserIds] = finalUserIds.joinToString(",")
            it[ProductsTable.sharedUsernames] = finalUsernames.joinToString(",")
        }

        getProductByIdInternal(productId)
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

    private fun resolveShares(
        sharedUserIds: List<Long>,
        shareWithUsernames: List<String>
    ): Pair<List<Long>, List<String>> {
        val userIdsFromUsernames = if (shareWithUsernames.isNotEmpty()) {
            UsersTable.selectAll()
                .where { UsersTable.username inList shareWithUsernames }
                .map { it[UsersTable.id] }
        } else emptyList()

        val combinedUserIds = (sharedUserIds + userIdsFromUsernames).distinct()

        val validUsers = if (combinedUserIds.isNotEmpty()) {
            UsersTable.selectAll()
                .where { UsersTable.id inList combinedUserIds }
                .map { Pair(it[UsersTable.id], it[UsersTable.username]) }
        } else emptyList()

        val resolvedUserIds = validUsers.map { it.first }
        val resolvedUsernames = validUsers.map { it.second }

        return Pair(resolvedUserIds, resolvedUsernames)
    }

    private fun rowToProduct(row: ResultRow): Product {
        val productId = row[ProductsTable.id]
        val creatorId = row[ProductsTable.userId]

        val creatorName = UsersTable.selectAll()
            .where { UsersTable.id eq creatorId }
            .map { it[UsersTable.name] }
            .singleOrNull()

        val sharedUserIdsRaw = row[ProductsTable.sharedUserIds]
        val sharedUsernamesRaw = row[ProductsTable.sharedUsernames]

        val sharedUserIds = sharedUserIdsRaw.split(",")
            .mapNotNull { it.trim().toLongOrNull() }

        val sharedUsernames = sharedUsernamesRaw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val storedWarehouseImageUrl = try { row[ProductsTable.warehouseImageUrl] } catch (_: Exception) { "" }
        val effectiveWarehouseImageUrl = storedWarehouseImageUrl.ifEmpty { null }

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
            warehouseImageUrl = effectiveWarehouseImageUrl,
            warehouseImage = effectiveWarehouseImageUrl,
            sharedUserIds = sharedUserIds,
            sharedUsernames = sharedUsernames,
            createdAt = row[ProductsTable.createdAt]
        )
    }
}
