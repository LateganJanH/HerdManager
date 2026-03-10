package com.herdmanager.app.domain.repository

/**
 * Reads app-level config from Firestore (e.g. minimum supported version, access suspended).
 * Optional: if [getMinVersionCode] returns null, the app does not enforce an update.
 */
interface AppConfigRepository {

    /**
     * Fetches the minimum required version code from Firestore `users/{uid}/config/app`.
     * Returns null if the doc is missing, has no minVersionCode, or on error (app allows access).
     */
    suspend fun getMinVersionCode(uid: String): Int?

    /**
     * Returns true if access is suspended (e.g. subscription lapsed) from Firestore `users/{uid}/config/app` field `accessSuspended`.
     * When true, the app should show "Subscription lapsed. Contact support." (see BILLING-IMPLEMENTATION §3.4).
     */
    suspend fun getAccessSuspended(uid: String): Boolean

    /**
     * Returns the epoch millis after which sync is blocked (EOL policy).
     * Read from Firestore `users/{uid}/config/app` then `config/app`; user overrides global.
     * If non-null and [System.currentTimeMillis()] >= returned value, sync should be blocked and
     * [SyncBlockedException] used so UI can show "Update required".
     */
    suspend fun getBlockSyncAfterEpochMs(uid: String): Long?
}
