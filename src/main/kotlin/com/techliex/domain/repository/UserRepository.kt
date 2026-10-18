package com.techliex.domain.repository

import com.techliex.domain.model.User

interface UserRepository {
    suspend fun findByUsername(username: String): User?
    suspend fun findById(id: Long): User?
    suspend fun findUserPasswordHash(username: String): String?
    suspend fun createUser(username: String, passwordHash: String, name: String, role: String): User
    suspend fun getAllUsers(): List<User>
    suspend fun deactivateUser(username: String): Boolean
    suspend fun activateUser(username: String): Boolean
    suspend fun updatePassword(userId: Long, newPasswordHash: String): Boolean
    suspend fun updatePasswordByUsername(username: String, newPasswordHash: String): Boolean
}
