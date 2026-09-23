package com.techliex.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.techliex.presentation.dto.UserDto
import io.ktor.server.config.*
import java.util.*

object JwtConfig {
    private var secret: String = "techliex-super-secret-jwt-key-2025!"
    var issuer: String = "http://0.0.0.0:8080/"
        private set
    var audience: String = "http://0.0.0.0:8080/api/v1"
        private set
    var realm: String = "Techliex Management API"
        private set

    // 30 days expiration in milliseconds
    private const val VALIDITY_IN_MS = 30L * 24 * 60 * 60 * 1000

    private val algorithm: Algorithm
        get() = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier by lazy {
        JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(audience)
            .build()
    }

    fun init(config: ApplicationConfig) {
        secret = config.propertyOrNull("jwt.secret")?.getString() ?: secret
        issuer = config.propertyOrNull("jwt.issuer")?.getString() ?: issuer
        audience = config.propertyOrNull("jwt.audience")?.getString() ?: audience
        realm = config.propertyOrNull("jwt.realm")?.getString() ?: realm
    }

    fun generateToken(user: UserDto): String {
        val expiration = Date(System.currentTimeMillis() + VALIDITY_IN_MS)
        return JWT.create()
            .withSubject("Authentication")
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("id", user.id)
            .withClaim("username", user.username)
            .withClaim("role", user.role)
            .withExpiresAt(expiration)
            .sign(algorithm)
    }
}
