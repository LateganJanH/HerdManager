package com.herdmanager.app

import com.herdmanager.app.domain.repository.AppConfigRepository

/** Returns null so instrumented tests never hit "Update required" and can proceed to main app. */
class FakeAppConfigRepository : AppConfigRepository {
    override suspend fun getMinVersionCode(uid: String): Int? = null
    override suspend fun getAccessSuspended(uid: String): Boolean = false
    override suspend fun getBlockSyncAfterEpochMs(uid: String): Long? = null
}
