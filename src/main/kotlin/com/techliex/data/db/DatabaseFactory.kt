package com.techliex.data.db

import at.favre.lib.crypto.bcrypt.BCrypt
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

object DatabaseFactory {

    fun init(config: ApplicationConfig) {
        val driverClass = config.propertyOrNull("db.driver")?.getString() ?: "org.postgresql.Driver"
        val jdbcUrl = config.propertyOrNull("db.jdbcUrl")?.getString() ?: "jdbc:postgresql://localhost:5432/techliex_db"
        val dbUser = config.propertyOrNull("db.user")?.getString() ?: "techliex"
        val dbPassword = config.propertyOrNull("db.password")?.getString() ?: "techliex@123"

        try {
            val hikariConfig = HikariConfig().apply {
                driverClassName = driverClass
                this.jdbcUrl = jdbcUrl
                username = dbUser
                password = dbPassword
                maximumPoolSize = 10
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                validate()
            }

            val dataSource = HikariDataSource(hikariConfig)
            Database.connect(dataSource)

            transaction {
                SchemaUtils.create(UsersTable, ProductsTable, ProductSharesTable, OrdersTable)

                // Seed initial Admin user: name="Umair", username="umair", password="umair123" if users table is empty
                if (UsersTable.selectAll().count() == 0L) {
                    val adminPasswordHash = BCrypt.withDefaults().hashToString(12, "umair123".toCharArray())
                    UsersTable.insert {
                        it[username] = "umair"
                        it[passwordHash] = adminPasswordHash
                        it[name] = "Umair"
                        it[role] = "Admin"
                        it[isActive] = true
                        it[createdAt] = Instant.now().toString()
                    }
                }
            }
        } catch (e: Exception) {
            println("Database initialization skipped or failed: ${e.message}")
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
