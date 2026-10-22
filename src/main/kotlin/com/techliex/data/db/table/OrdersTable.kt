package com.techliex.data.db.table

import org.jetbrains.exposed.sql.Table
import java.math.BigDecimal

object OrdersTable : Table("orders") {
    val id = long("id").autoIncrement()
    val orderNumber = varchar("order_number", 100).default("")
    val orderDate = varchar("order_date", 50)
    val userId = reference("user_id", UsersTable.id)
    val productId = long("product_id")
    val productName = varchar("product_name", 255).default("")
    val productImage = text("product_image").default("")
    val productSourcePrice = decimal("product_source_price", 10, 2).default(BigDecimal.ZERO)
    val address = text("address")
    val note = text("note").nullable()
    val variationNote = text("variation_note").nullable()
    val quantity = integer("quantity")
    val listingPrice = decimal("listing_price", 10, 2)
    val paymentScreenshot = text("payment_screenshot").default("")
    val paymentImageUrl = text("payment_image_url").default("")
    val trackId = varchar("track_id", 100).default("")
    val company = varchar("company", 100).default("")
    val status = varchar("status", 50).default("Active")
    val createdAt = varchar("created_at", 50)

    override val primaryKey = PrimaryKey(id)
}
