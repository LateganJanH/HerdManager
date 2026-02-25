package com.herdmanager.app.domain.repository

import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface SyncRepository {
    /** Last time sync completed successfully; null if never synced. */
    fun lastSyncedAt(): Flow<Instant?>

    /** Upload local data to cloud and download remote data; merges remote into local by document (newer wins per doc). Requires signed-in user. */
    suspend fun syncNow(): Result<Unit>
}
