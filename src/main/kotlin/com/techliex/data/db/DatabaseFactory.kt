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
import org.jetbrains.exposed.sql.update
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
                seedAdminUser()
            }
        } catch (e: Exception) {
            println("⚠️ Primary database connection failed (${e.message}). Falling back to embedded H2 database for local environment...")
            try {
                val h2Config = HikariConfig().apply {
                    driverClassName = "org.h2.Driver"
                    this.jdbcUrl = "jdbc:h2:mem:techliex_db;DB_CLOSE_DELAY=-1"
                    maximumPoolSize = 5
                    isAutoCommit = false
                    validate()
                }
                val h2DataSource = HikariDataSource(h2Config)
                Database.connect(h2DataSource)

                transaction {
                    SchemaUtils.create(UsersTable, ProductsTable, ProductSharesTable, OrdersTable)
                    seedAdminUser()
                }
                println("✅ H2 in-memory database successfully initialized and seeded!")
            } catch (h2Ex: Exception) {
                println("❌ Database initialization error: ${h2Ex.message}")
            }
        }
    }

    private fun seedAdminUser() {
        val adminPasswordHash = BCrypt.withDefaults().hashToString(12, "umair123".toCharArray())
        val count = UsersTable.selectAll().where { UsersTable.username eq "umair" }.count()

        if (count == 0L) {
            UsersTable.insert {
                it[username] = "umair"
                it[passwordHash] = adminPasswordHash
                it[name] = "Umair"
                it[role] = "Admin"
                it[isActive] = true
                it[createdAt] = Instant.now().toString()
            }
        } else {
            UsersTable.update({ UsersTable.username eq "umair" }) {
                it[passwordHash] = adminPasswordHash
                it[isActive] = true
            }
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
