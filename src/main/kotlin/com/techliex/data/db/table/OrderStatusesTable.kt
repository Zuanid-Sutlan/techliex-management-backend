package com.techliex.data.db.table

import org.jetbrains.exposed.sql.Table

object OrderStatusesTable : Table("order_statuses") {
    val id = long("id").autoIncrement()
    val statusName = varchar("status_name", 100).uniqueIndex()

    override val primaryKey = PrimaryKey(id)
}
