package com.techliex.domain.usecase.auth

import com.techliex.domain.model.User
import com.techliex.domain.repository.UserRepository

class LoginUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(username: String): Pair<User, String>? {
        val user = userRepository.findByUsername(username) ?: return null
        if (!user.isActive) return null
        val hash = userRepository.findUserPasswordHash(username) ?: return null
        return Pair(user, hash)
    }
}

class GetUserProfileUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(userId: Long): User? {
        return userRepository.findById(userId)
    }
}
