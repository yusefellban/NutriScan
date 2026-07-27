package iti.grad.nutriscan.domain.common.model

sealed class DomainException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    class UnauthorizedException(message: String? = null, cause: Throwable? = null) : DomainException(message, cause)
    class NetworkException(message: String? = null, cause: Throwable? = null) : DomainException(message, cause)
    class ServerException(message: String? = null, cause: Throwable? = null) : DomainException(message, cause)
    class UnknownException(message: String? = null, cause: Throwable? = null) : DomainException(message, cause)
}
