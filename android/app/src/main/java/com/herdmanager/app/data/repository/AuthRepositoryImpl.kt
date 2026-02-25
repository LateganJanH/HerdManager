package com.herdmanager.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.herdmanager.app.domain.model.AuthUser
import com.herdmanager.app.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override fun authState(): Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toAuthUser())
        }
        firebaseAuth.addAuthStateListener(listener)
        trySend(firebaseAuth.currentUser?.toAuthUser())
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(email: String, password: String): Result<AuthUser> = runCatching {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        result.user?.toAuthUser() ?: error("Sign-in succeeded but user is null")
    }

    override suspend fun signUp(email: String, password: String): Result<AuthUser> = runCatching {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        result.user?.toAuthUser() ?: error("Sign-up succeeded but user is null")
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }
}

private fun com.google.firebase.auth.FirebaseUser.toAuthUser(): AuthUser =
    AuthUser(uid = uid, email = email)
