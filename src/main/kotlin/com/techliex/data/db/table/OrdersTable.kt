package com.techliex.data.db.table

import org.jetbrains.exposed.sql.Table

object OrdersTable : Table("orders") {
    val id = long("id").autoIncrement()
    val orderDate = varchar("order_date", 20)
    val userId = reference("user_id", UsersTable.id)
    val productId = long("product_id")
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
