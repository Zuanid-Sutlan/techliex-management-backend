package com.techliex.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long = -1,
    val username: String = "",
    val password: String = "",
    val name: String = "",
    val role: String = "",
    val isActive: Boolean = false,
    val createdAt: String = ""
)
