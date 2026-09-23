package com.techliex.domain.usecase.user

import com.techliex.domain.model.User
import com.techliex.domain.repository.UserRepository

class CreateUserUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(username: String, passwordHash: String, name: String, role: String): User {
        return userRepository.createUser(username, passwordHash, name, role)
    }
}

class GetAllUsersUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(): List<User> {
        return userRepository.getAllActiveUsers()
    }
}

class GetUserByUsernameUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(username: String): User? {
        return userRepository.findByUsername(username)
    }
}

class DeactivateUserUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(username: String): Boolean {
        return userRepository.deactivateUser(username)
    }
}
