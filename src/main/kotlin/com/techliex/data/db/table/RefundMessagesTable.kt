package com.techliex.data.db.table

import org.jetbrains.exposed.sql.Table

object RefundMessagesTable : Table("refund_messages") {
    val id = long("id").autoIncrement()
    val orderId = reference("order_id", OrdersTable.id)
    val senderId = reference("sender_id", UsersTable.id)
    val senderName = varchar("sender_name", 255)
    val senderRole = varchar("sender_role", 50)
    val message = text("message")
    val screenshots = text("screenshots").default("[]")
    val createdAt = varchar("created_at", 50)

    override val primaryKey = PrimaryKey(id)
}
