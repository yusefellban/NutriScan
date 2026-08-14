package iti.grad.nutriscan.presentation.common.model

/**
 * Canonical error classification used across all screens.
 *
 * Produced by [throwableToAppErrorType] from any caught [Throwable] and stored in every
 * screen's UI state so the View layer can render the correct empty-state widget without
 * duplicating error-detection logic.
 */
enum class AppErrorType {
    /** TCP/DNS failure — no internet or server unreachable. */
    NETWORK,
    /** HTTP 5xx response — server is up but returned an error. */
    SERVER,
    /** HTTP 404 response — resource not found. */
    NOT_FOUND,
    /** Anything else (other 4xx business errors, parsing errors, etc.). */
    UNKNOWN,
}

/**
 * Maps any [Throwable] to an [AppErrorType].
 *
 * Convention: the [ErrorInterceptor][iti.grad.nutriscan.data.remote.interceptor.ErrorInterceptor]
 * throws a `ServerException` whose message starts with `"5xx"` for all HTTP 500-599 responses.
 * Network-level failures surface as [java.io.IOException] subclasses.
 */
fun throwableToAppErrorType(error: Throwable): AppErrorType = when {
    error.message?.startsWith("5xx") == true -> AppErrorType.SERVER
    error.message?.startsWith("404") == true || error.message?.contains("NOT_FOUND") == true -> AppErrorType.NOT_FOUND
    error is java.net.UnknownHostException
        || error is java.net.ConnectException
        || error is java.net.SocketTimeoutException
        || error is java.io.IOException -> AppErrorType.NETWORK
    else -> AppErrorType.UNKNOWN
}
