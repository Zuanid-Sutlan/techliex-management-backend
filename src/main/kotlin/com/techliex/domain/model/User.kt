package com.techliex.domain.model

data class User(
    val id: Long,
    val username: String,
    val name: String,
    val role: String,
    val isActive: Boolean,
    val createdAt: String
)
