package com.youtubevault.app.auth

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.youtubevault.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed class AuthResult {
    data class Success(val email: String, val accessToken: String) : AuthResult()
    data class RecoverableError(val intent: Intent) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class GoogleAuthManager(private val context: Context) {

    companion object {
        private const val SCOPE_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
        private const val SCOPE_DRIVE_READONLY = "https://www.googleapis.com/auth/drive.readonly"
        private const val SCOPE_USERINFO_EMAIL = "https://www.googleapis.com/auth/userinfo.email"
        private const val OAUTH_SCOPE_STRING =
            "oauth2:$SCOPE_DRIVE_FILE $SCOPE_DRIVE_READONLY $SCOPE_USERINFO_EMAIL"
    }

    private val signInOptions: GoogleSignInOptions by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope(SCOPE_DRIVE_FILE),
                Scope(SCOPE_DRIVE_READONLY),
                Scope(SCOPE_USERINFO_EMAIL)
            )
            .build()
    }

    val signInClient: GoogleSignInClient by lazy {
        GoogleSignIn.getClient(context, signInOptions)
    }

    fun getSignInIntent(): Intent = signInClient.signInIntent

    fun getLastSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    fun isEmailAllowed(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val raw = BuildConfig.ALLOWED_EMAILS.trim()
        if (raw.isEmpty()) return true
        val allowed = raw.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        return allowed.isEmpty() || allowed.contains(email.lowercase())
    }

    suspend fun handleSignInIntentResult(data: Intent?): AuthResult {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            fetchAccessToken(account)
        } catch (e: ApiException) {
            AuthResult.Error("Google Sign-In failed (code ${e.statusCode}): ${e.localizedMessage}")
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Sign-In failed")
        }
    }

    suspend fun fetchAccessToken(account: GoogleSignInAccount): AuthResult {
        val email = account.email
        if (!isEmailAllowed(email)) {
            signOut()
            return AuthResult.Error("Access denied: Unauthorized Google account ($email).")
        }

        val androidAccount: Account = account.account
            ?: return AuthResult.Error("No Android account found for $email")

        return withContext(Dispatchers.IO) {
            try {
                val token = GoogleAuthUtil.getToken(context, androidAccount, OAUTH_SCOPE_STRING)
                AuthResult.Success(email = email.orEmpty(), accessToken = token)
            } catch (e: UserRecoverableAuthException) {
                e.intent?.let { AuthResult.RecoverableError(it) }
                    ?: AuthResult.Error("Additional Drive permission required")
            } catch (e: Exception) {
                AuthResult.Error(e.localizedMessage ?: "Failed to obtain access token")
            }
        }
    }

    suspend fun signOut() {
        try {
            signInClient.signOut().await()
        } catch (_: Exception) {
        }
    }
}
