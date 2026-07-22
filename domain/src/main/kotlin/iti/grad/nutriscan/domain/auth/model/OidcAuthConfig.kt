package iti.grad.nutriscan.domain.auth.model

data class OidcAuthConfig(
    val authorizationEndpoint: String,
    val tokenEndpoint: String,
    val clientId: String,
    val redirectUri: String
)
