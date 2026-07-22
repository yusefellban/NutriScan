package iti.grad.nutriscan.domain.auth.model

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String?,
    val idToken: String?,
    val expiresIn: Long?,
    val refreshExpiresIn: Long?
)
