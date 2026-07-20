package iti.grad.nutriscan.data.remote.interceptor

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import iti.grad.nutriscan.data.local.datasource.IAuthTokenLocalDataSource
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationService
import org.json.JSONException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class AuthTokenProviderImpl @Inject constructor(
    private val authTokenLocalDataSource: IAuthTokenLocalDataSource,
    @ApplicationContext private val context: Context
) : IAuthTokenProvider {

    override suspend fun getToken(): String? {
        val stateJson = authTokenLocalDataSource.getAuthState() ?: return null
        
        val authState = try {
            AuthState.jsonDeserialize(stateJson)
        } catch (e: JSONException) {
            return null
        }

        if (!authState.isAuthorized) return null

        return try {
            suspendCancellableCoroutine { continuation ->
                val authService = AuthorizationService(context)
                
                try {
                    authState.performActionWithFreshTokens(authService) { accessToken, _, ex ->
                        if (ex != null) {
                            // Refresh failed, token is invalid.
                            continuation.resume(null)
                        } else {
                            continuation.resume(accessToken)
                        }
                        
                        authService.dispose()
                    }
                } catch (e: IllegalStateException) {
                    authService.dispose()
                    continuation.resume(null)
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
