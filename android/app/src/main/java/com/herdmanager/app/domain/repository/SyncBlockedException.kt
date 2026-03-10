package com.herdmanager.app.domain.repository

/**
 * Thrown when sync is blocked by policy (e.g. EOL date).
 * UI should show an "Update required" message when this is the cause of sync failure.
 */
class SyncBlockedException(
    val blockSyncAfterEpochMs: Long,
    message: String = "Sync is disabled for this app version. Please update the app."
) : Exception(message)
