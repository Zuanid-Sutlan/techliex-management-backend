package com.techliex.security

import at.favre.lib.crypto.bcrypt.BCrypt
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

data class UserPrincipal(
    val id: Long,
    val username: String,
    val role: String
)

fun Application.configureSecurity() {
    authentication {
        jwt("auth-jwt") {
            realm = JwtConfig.realm
            verifier(JwtConfig.verifier)
            validate { credential ->
                val id = credential.payload.getClaim("id").asLong()
                val username = credential.payload.getClaim("username").asString()
                val role = credential.payload.getClaim("role").asString()
                if (id != null && !username.isNullOrEmpty() && !role.isNullOrEmpty()) {
                    UserPrincipal(id, username, role)
                } else {
                    null
                }
            }
        }
    }
}

fun ApplicationCall.getUserPrincipal(): UserPrincipal {
    return principal<UserPrincipal>() ?: throw IllegalStateException("User not authenticated")
}

object PasswordUtils {
    fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray())
    }

    fun verifyPassword(password: String, hash: String): Boolean {
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified
    }
}
