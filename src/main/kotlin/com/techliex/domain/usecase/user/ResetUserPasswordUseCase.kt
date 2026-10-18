package com.techliex.domain.usecase.user

import com.techliex.domain.repository.UserRepository

class ResetUserPasswordUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(username: String, newPasswordHash: String): Boolean {
        return userRepository.updatePasswordByUsername(username, newPasswordHash)
    }
}
