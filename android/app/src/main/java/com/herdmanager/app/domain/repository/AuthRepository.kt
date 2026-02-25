package com.herdmanager.app.domain.repository

import com.herdmanager.app.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Emits the current user when signed in, or null when signed out. */
    fun authState(): Flow<AuthUser?>

    suspend fun signIn(email: String, password: String): Result<AuthUser>

    suspend fun signUp(email: String, password: String): Result<AuthUser>

    fun signOut()
}
