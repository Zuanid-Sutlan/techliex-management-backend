package com.techliex.data.db.table

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object ProductSharesTable : Table("product_shares") {
    val id = long("id").autoIncrement()
    val productId = reference("product_id", ProductsTable.id, onDelete = ReferenceOption.CASCADE)
    val sharedUserId = reference("shared_user_id", UsersTable.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(id)
}
