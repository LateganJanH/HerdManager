package com.herdmanager.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.herdmanager.app.domain.repository.AppConfigRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val COL_CONFIG = "config"
private const val DOC_APP = "app"
private const val FIELD_MIN_VERSION_CODE = "minVersionCode"
private const val FIELD_ACCESS_SUSPENDED = "accessSuspended"
private const val FIELD_BLOCK_SYNC_AFTER_EPOCH_MS = "blockSyncAfterEpochMs"

class AppConfigRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : AppConfigRepository {

    override suspend fun getMinVersionCode(uid: String): Int? {
        return try {
            val doc = firestore.collection("users").document(uid)
                .collection(COL_CONFIG).document(DOC_APP)
                .get().await()
            (doc.get(FIELD_MIN_VERSION_CODE) as? Number)?.toInt()
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getAccessSuspended(uid: String): Boolean {
        return try {
            val userDoc = firestore.collection("users").document(uid)
                .collection(COL_CONFIG).document(DOC_APP)
                .get().await()
            if ((userDoc.get(FIELD_ACCESS_SUSPENDED) as? Boolean) == true) return true
            val globalDoc = firestore.collection(COL_CONFIG).document(DOC_APP).get().await()
            (globalDoc.get(FIELD_ACCESS_SUSPENDED) as? Boolean) == true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun getBlockSyncAfterEpochMs(uid: String): Long? {
        return try {
            val userDoc = firestore.collection("users").document(uid)
                .collection(COL_CONFIG).document(DOC_APP)
                .get().await()
            (userDoc.get(FIELD_BLOCK_SYNC_AFTER_EPOCH_MS) as? Number)?.toLong()
                ?: (firestore.collection(COL_CONFIG).document(DOC_APP).get().await()
                    .get(FIELD_BLOCK_SYNC_AFTER_EPOCH_MS) as? Number)?.toLong()
        } catch (_: Exception) {
            null
        }
    }
}
