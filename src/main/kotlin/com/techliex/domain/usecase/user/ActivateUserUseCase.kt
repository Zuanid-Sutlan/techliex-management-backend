package com.techliex.domain.usecase.user

import com.techliex.domain.repository.UserRepository

class ActivateUserUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(username: String): Boolean {
        return userRepository.activateUser(username)
    }
}
