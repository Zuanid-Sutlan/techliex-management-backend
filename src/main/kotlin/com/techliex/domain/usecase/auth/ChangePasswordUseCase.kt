package com.techliex.domain.usecase.auth

import com.techliex.domain.repository.UserRepository

class ChangePasswordUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(userId: Long, username: String, verifyOldPassword: (String) -> Boolean, newPasswordHash: String): Boolean {
        val currentHash = userRepository.findUserPasswordHash(username) ?: return false
        if (!verifyOldPassword(currentHash)) {
            return false
        }
        return userRepository.updatePassword(userId, newPasswordHash)
    }
}
