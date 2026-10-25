package com.techliex.data.db.table

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import java.math.BigDecimal

object ProductsTable : Table("products") {
    val id = long("id").autoIncrement()
    val userId = reference("user_id", UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val productImageUrl = text("product_image_url")
    val sourceLink = text("source_link")
    val sourcePrice = decimal("source_price", 10, 2)
    val referenceLink = text("reference_link")
    val referencePrice = decimal("reference_price", 10, 2)
    val warehousePrice = decimal("warehouse_price", 10, 2).default(BigDecimal.ZERO)
    val warehouseNote = text("warehouse_note").nullable()
    val sharedUserIds = text("shared_user_ids").default("")
    val sharedUsernames = text("shared_usernames").default("")
    val createdAt = varchar("created_at", 50)

    override val primaryKey = PrimaryKey(id)
}
