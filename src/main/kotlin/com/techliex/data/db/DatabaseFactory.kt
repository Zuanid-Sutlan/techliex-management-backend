package com.techliex.data.db

import at.favre.lib.crypto.bcrypt.BCrypt
import com.techliex.data.db.table.OrdersTable
import com.techliex.data.db.table.ProductsTable
import com.techliex.data.db.table.RefundMessagesTable
import com.techliex.data.db.table.UsersTable
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

data class DbHealthStatus(
    val isConnected: Boolean,
    val databaseType: String,
    val message: String
)

object DatabaseFactory {

    @Volatile
    private var activeDataSource: HikariDataSource? = null

    @Volatile
    var currentDbType: String = "Not Initialized"
        private set

    fun init(config: ApplicationConfig) {
        val (jdbcUrl, dbUser, dbPassword) = parseConnectionDetails(config)
        val driverClass = config.propertyOrNull("db.driver")?.getString() ?: "org.postgresql.Driver"

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
            activeDataSource = dataSource
            currentDbType = "PostgreSQL"

            initSchemaAndSeed()
            println("✅ Successfully connected to PostgreSQL database ($jdbcUrl)!")
        } catch (e: Exception) {
            println("⚠️ Primary PostgreSQL connection failed (${e.message}). Initializing persistent local H2 database on disk...")
            try {
                val h2Config = HikariConfig().apply {
                    driverClassName = "org.h2.Driver"
                    this.jdbcUrl = "jdbc:h2:./data/techliex_db;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE"
                    maximumPoolSize = 5
                    isAutoCommit = false
                    validate()
                }
                val h2DataSource = HikariDataSource(h2Config)
                Database.connect(h2DataSource)
                activeDataSource = h2DataSource
                currentDbType = "H2 File-based Fallback"

                initSchemaAndSeed()
                println("✅ Persistent file-based database successfully loaded at ./data/techliex_db.mv.db!")
            } catch (h2Ex: Exception) {
                currentDbType = "Disconnected"
                println("❌ Database initialization error: ${h2Ex.message}")
            }
        }
    }

    private fun initSchemaAndSeed() {
        transaction {
            try {
                exec("DELETE FROM refund_messages WHERE order_id NOT IN (SELECT id FROM orders) OR sender_id NOT IN (SELECT id FROM users);")
                exec("DELETE FROM orders WHERE user_id NOT IN (SELECT id FROM users);")
                exec("DELETE FROM products WHERE user_id NOT IN (SELECT id FROM users);")
            } catch (_: Exception) {
                // Table might not exist yet on fresh setup
            }
            SchemaUtils.createMissingTablesAndColumns(UsersTable, ProductsTable, OrdersTable, RefundMessagesTable)
            seedAdminUser()
        }
    }

    internal fun parseConnectionDetails(config: ApplicationConfig): Triple<String, String, String> {
        val rawUrl = System.getenv("DATABASE_URL")
            ?: System.getenv("DB_JDBC_URL")
            ?: config.propertyOrNull("db.jdbcUrl")?.getString()
            ?: "jdbc:postgresql://localhost:5432/techliex_db"

        var user = System.getenv("DB_USER")
            ?: config.propertyOrNull("db.user")?.getString()
            ?: "techliex"

        var password = System.getenv("DB_PASSWORD")
            ?: config.propertyOrNull("db.password")?.getString()
            ?: "techliex@123"

        var jdbcUrl = rawUrl.trim()

        // Normalize postgres:// or postgresql:// scheme to jdbc:postgresql://
        if (jdbcUrl.startsWith("postgres://")) {
            jdbcUrl = "jdbc:postgresql://" + jdbcUrl.removePrefix("postgres://")
        } else if (jdbcUrl.startsWith("postgresql://")) {
            jdbcUrl = "jdbc:postgresql://" + jdbcUrl.removePrefix("postgresql://")
        }

        // Handle credentials embedded in URL (e.g. jdbc:postgresql://user:pass@host:port/dbname)
        if (jdbcUrl.startsWith("jdbc:postgresql://")) {
            val pathAndHostPart = jdbcUrl.removePrefix("jdbc:postgresql://")

            // Find boundary where database name or query parameters start
            val slashIndex = pathAndHostPart.indexOf('/')
            val hostAndUserSection = if (slashIndex != -1) {
                pathAndHostPart.substring(0, slashIndex)
            } else {
                pathAndHostPart
            }

            // Find the LAST '@' before '/' which separates user:pass credentials from the host
            val lastAtIndex = hostAndUserSection.lastIndexOf('@')
            if (lastAtIndex != -1) {
                val userInfo = hostAndUserSection.substring(0, lastAtIndex)
                val hostPort = hostAndUserSection.substring(lastAtIndex + 1)

                // userInfo is user:password
                val colonIndex = userInfo.indexOf(':')
                if (colonIndex != -1) {
                    user = java.net.URLDecoder.decode(userInfo.substring(0, colonIndex), "UTF-8")
                    password = java.net.URLDecoder.decode(userInfo.substring(colonIndex + 1), "UTF-8")
                } else {
                    user = java.net.URLDecoder.decode(userInfo, "UTF-8")
                }

                val restOfUrl = if (slashIndex != -1) pathAndHostPart.substring(slashIndex) else ""
                jdbcUrl = "jdbc:postgresql://$hostPort$restOfUrl"
            }
        }

        if (!jdbcUrl.startsWith("jdbc:")) {
            jdbcUrl = "jdbc:$jdbcUrl"
        }

        return Triple(jdbcUrl, user, password)
    }

    fun checkHealth(): DbHealthStatus {
        if (activeDataSource == null) {
            return DbHealthStatus(
                isConnected = false,
                databaseType = "Not Initialized",
                message = "Database datasource is not initialized."
            )
        }
        return try {
            transaction {
                exec("SELECT 1;") { }
            }
            DbHealthStatus(
                isConnected = true,
                databaseType = currentDbType,
                message = "Database is connected and healthy."
            )
        } catch (e: Exception) {
            DbHealthStatus(
                isConnected = false,
                databaseType = currentDbType,
                message = "Database check failed: ${e.message}"
            )
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
