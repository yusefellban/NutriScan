package iti.grad.nutriscan.data.remote.interceptor

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub [IAuthTokenProvider] that returns null (no token).
 * This will be replaced with a real implementation when Login is integrated,
 * reading the access token from local storage (DataStore/Proto).
 */
@Singleton
class StubAuthTokenProvider @Inject constructor() : IAuthTokenProvider {
    override suspend fun getToken(): String? = null
}
