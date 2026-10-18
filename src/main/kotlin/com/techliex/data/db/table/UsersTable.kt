package com.techliex.data.db.table

import org.jetbrains.exposed.sql.Table

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
