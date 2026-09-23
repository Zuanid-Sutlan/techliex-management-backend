package com.techliex.data.db

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import java.math.BigDecimal

object UsersTable : Table("users") {
    val id = long("id").autoIncrement()
    val username = varchar("username", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val name = varchar("name", 100)
    val role = varchar("role", 20).default("Member")
    val isActive = bool("is_active").default(true)
    val createdAt = varchar("created_at", 50)

    override val primaryKey = PrimaryKey(id)
}

object ProductsTable : Table("products") {
    val id = long("id").autoIncrement()
    val userId = reference("user_id", UsersTable.id)
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val productImageUrl = text("product_image_url")
    val sourceLink = text("source_link")
    val sourcePrice = decimal("source_price", 10, 2)
    val referenceLink = text("reference_link")
    val referencePrice = decimal("reference_price", 10, 2)
    val warehousePrice = decimal("warehouse_price", 10, 2).default(BigDecimal.ZERO)
    val warehouseNote = text("warehouse_note").nullable()
    val createdAt = varchar("created_at", 50)

    override val primaryKey = PrimaryKey(id)
}

object ProductSharesTable : Table("product_shares") {
    val id = long("id").autoIncrement()
    val productId = reference("product_id", ProductsTable.id, onDelete = ReferenceOption.CASCADE)
    val sharedUserId = reference("shared_user_id", UsersTable.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(id)
}

object OrdersTable : Table("orders") {
    val id = long("id").autoIncrement()
    val orderDate = varchar("order_date", 20)
    val userId = reference("user_id", UsersTable.id)
    val productId = reference("product_id", ProductsTable.id)
    val address = text("address")
    val variationNote = text("variation_note").nullable()
    val quantity = integer("quantity")
    val listingPrice = decimal("listing_price", 10, 2)
    val paymentImageUrl = text("payment_image_url")
    val trackId = varchar("track_id", 100).default("")
    val company = varchar("company", 100).default("")
    val status = varchar("status", 20).default("Active")
    val createdAt = varchar("created_at", 50)

    override val primaryKey = PrimaryKey(id)
}
