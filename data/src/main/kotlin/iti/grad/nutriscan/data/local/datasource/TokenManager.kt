package iti.grad.nutriscan.data.local.datasource

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(@ApplicationContext context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = try {
        createEncryptedPrefs(context)
    } catch (e: Exception) {
        // If Keystore key is invalidated (e.g. app reinstall without clearing backup), 
        // EncryptedSharedPreferences throws AEADBadTagException/SecurityException.
        // Recovery: delete the corrupted preferences file and try again.
        context.deleteSharedPreferences("nutriscan_secure_prefs")
        createEncryptedPrefs(context)
    }

    private fun createEncryptedPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        "nutriscan_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveTokens(accessToken: String, refreshToken: String, idToken: String? = null) {
        val editor = sharedPreferences.edit()
            .putString("ACCESS_TOKEN", accessToken)
            .putString("REFRESH_TOKEN", refreshToken)
        if (idToken != null) editor.putString("ID_TOKEN", idToken)
        editor.apply()
    }

    fun getAccessToken(): String? = sharedPreferences.getString("ACCESS_TOKEN", null)

    fun getRefreshToken(): String? = sharedPreferences.getString("REFRESH_TOKEN", null)

    fun getIdToken(): String? = sharedPreferences.getString("ID_TOKEN", null)

    fun clearTokens() {
        sharedPreferences.edit().clear().apply()
    }
}
