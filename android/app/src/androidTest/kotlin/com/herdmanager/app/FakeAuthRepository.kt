package com.herdmanager.app

import com.herdmanager.app.domain.model.AuthUser
import com.herdmanager.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Fake [AuthRepository] that always emits a signed-in user.
 * Used by instrumented tests to bypass Firebase and show the main app.
 */
class FakeAuthRepository : AuthRepository {

    private val testUser = AuthUser(uid = "test-uid", email = "test@herdmanager.test")

    override fun authState(): Flow<AuthUser?> = flowOf(testUser)

    override suspend fun signIn(email: String, password: String): Result<AuthUser> =
        Result.success(testUser)

    override suspend fun signUp(email: String, password: String): Result<AuthUser> =
        Result.success(testUser)

    override fun signOut() {}
}
