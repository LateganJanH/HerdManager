package com.herdmanager.app.domain.repository

interface BackupRepository {
    suspend fun exportToJson(): String
    suspend fun importFromJson(json: String): Result<Unit>
}
