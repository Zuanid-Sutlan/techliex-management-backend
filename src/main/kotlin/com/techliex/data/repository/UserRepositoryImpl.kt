package com.techliex.data.repository

import com.techliex.data.db.DatabaseFactory
import com.techliex.data.db.UsersTable
import com.techliex.domain.model.User
import com.techliex.domain.repository.UserRepository
import org.jetbrains.exposed.sql.*
import java.time.Instant

class UserRepositoryImpl : UserRepository {

    override suspend fun findByUsername(username: String): User? = DatabaseFactory.dbQuery {
        UsersTable.selectAll()
            .where { UsersTable.username eq username }
            .map { rowToUser(it) }
            .singleOrNull()
    }

    override suspend fun findById(id: Long): User? = DatabaseFactory.dbQuery {
        UsersTable.selectAll()
            .where { UsersTable.id eq id }
            .map { rowToUser(it) }
            .singleOrNull()
    }

    override suspend fun findUserPasswordHash(username: String): String? = DatabaseFactory.dbQuery {
        UsersTable.selectAll()
            .where { UsersTable.username eq username }
            .map { it[UsersTable.passwordHash] }
            .singleOrNull()
    }

    override suspend fun createUser(username: String, passwordHash: String, name: String, role: String): User = DatabaseFactory.dbQuery {
        val now = Instant.now().toString()
        val generatedId = UsersTable.insert {
            it[UsersTable.username] = username
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.name] = name
            it[UsersTable.role] = role
            it[UsersTable.isActive] = true
            it[UsersTable.createdAt] = now
        } get UsersTable.id

        User(
            id = generatedId,
            username = username,
            name = name,
            role = role,
            isActive = true,
            createdAt = now
        )
    }

    override suspend fun getAllActiveUsers(): List<User> = DatabaseFactory.dbQuery {
        UsersTable.selectAll()
            .where { UsersTable.isActive eq true }
            .map { rowToUser(it) }
    }

    override suspend fun deactivateUser(username: String): Boolean = DatabaseFactory.dbQuery {
        val updatedRows = UsersTable.update({ UsersTable.username eq username }) {
            it[isActive] = false
        }
        updatedRows > 0
    }

    private fun rowToUser(row: ResultRow): User {
        return User(
            id = row[UsersTable.id],
            username = row[UsersTable.username],
            name = row[UsersTable.name],
            role = row[UsersTable.role],
            isActive = row[UsersTable.isActive],
            createdAt = row[UsersTable.createdAt]
        )
    }
}
