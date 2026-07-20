package iti.grad.nutriscan.data.local.datasource

import kotlinx.coroutines.flow.Flow

interface IAuthTokenLocalDataSource {
    suspend fun saveAuthState(stateJson: String)
    suspend fun getAuthState(): String?
    suspend fun clear()
}
