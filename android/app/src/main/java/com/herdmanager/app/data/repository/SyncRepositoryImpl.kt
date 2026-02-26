package com.herdmanager.app.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.util.UUID
import kotlinx.coroutines.tasks.await
import com.herdmanager.app.data.local.dao.AnimalDao
import com.herdmanager.app.data.local.dao.BreedingEventDao
import com.herdmanager.app.data.local.dao.CalvingEventDao
import com.herdmanager.app.data.local.dao.HealthEventDao
import com.herdmanager.app.data.local.dao.HerdAssignmentDao
import com.herdmanager.app.data.local.dao.HerdDao
import com.herdmanager.app.data.local.dao.PhotoDao
import com.herdmanager.app.data.local.dao.WeightRecordDao
import com.herdmanager.app.data.local.entity.AnimalEntity
import com.herdmanager.app.data.local.entity.BreedingEventEntity
import com.herdmanager.app.data.local.entity.CalvingEventEntity
import com.herdmanager.app.data.local.entity.HealthEventEntity
import com.herdmanager.app.data.local.entity.HerdAssignmentEntity
import com.herdmanager.app.data.local.entity.HerdEntity
import com.herdmanager.app.data.local.entity.PhotoEntity
import com.herdmanager.app.data.local.entity.WeightRecordEntity
import com.herdmanager.app.domain.model.FarmSettings
import com.herdmanager.app.domain.model.FarmContact
import com.herdmanager.app.domain.repository.AuthRepository
import com.herdmanager.app.domain.repository.FarmSettingsRepository
import com.herdmanager.app.domain.repository.SyncRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject

private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_preferences")
private val KEY_LAST_SYNCED_AT = longPreferencesKey("last_synced_at")
private val KEY_DEVICE_ID = stringPreferencesKey("device_id")

private const val COL_ANIMALS = "animals"
private const val COL_DEVICES = "devices"
private const val COL_HERDS = "herds"
private const val COL_HERD_ASSIGNMENTS = "herd_assignments"
private const val COL_BREEDING_EVENTS = "breeding_events"
private const val COL_CALVING_EVENTS = "calving_events"
private const val COL_HEALTH_EVENTS = "health_events"
private const val COL_WEIGHT_RECORDS = "weight_records"
private const val COL_PHOTOS = "photos"
private const val COL_SETTINGS = "settings"
private const val DOC_FARM = "farm"

class SyncRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val authRepository: AuthRepository,
    private val animalDao: AnimalDao,
    private val herdDao: HerdDao,
    private val herdAssignmentDao: HerdAssignmentDao,
    private val breedingEventDao: BreedingEventDao,
    private val calvingEventDao: CalvingEventDao,
    private val healthEventDao: HealthEventDao,
    private val weightRecordDao: WeightRecordDao,
    private val photoDao: PhotoDao,
    private val farmSettingsRepository: FarmSettingsRepository
) : SyncRepository {

    override fun lastSyncedAt(): Flow<Instant?> = context.syncDataStore.data.map { prefs ->
        prefs[KEY_LAST_SYNCED_AT]?.let { Instant.ofEpochMilli(it) }
    }

    override suspend fun syncNow(): Result<Unit> = runCatching {
        val user = authRepository.authState().first() ?: error("Not signed in")
        val uid = user.uid

        val userRef = firestore.collection("users").document(uid)

        // Download farm settings first so this device gets updates from others before we overwrite.
        // Otherwise the last device to sync would overwrite Firestore with its (possibly empty) settings.
        downloadFarmSettingsFirst(userRef)

        // Upload local to Firestore
        uploadFarmSettings(userRef)
        uploadAnimals(userRef)
        uploadHerds(userRef)
        uploadHerdAssignments(userRef)
        uploadBreedingEvents(userRef)
        uploadCalvingEvents(userRef)
        uploadHealthEvents(userRef)
        uploadWeightRecords(userRef)
        uploadPhotos(userRef, uid)

        // Download remote and replace local
        downloadAndReplaceLocal(userRef)

        val now = System.currentTimeMillis()
        context.syncDataStore.edit { it[KEY_LAST_SYNCED_AT] = now }

        // Register this device for the web dashboard "linked devices" list
        val deviceId = getOrCreateDeviceId()
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifEmpty { "Android device" }
        userRef.collection(COL_DEVICES).document(deviceId).set(
            mapOf(
                "name" to deviceName,
                "lastSyncAt" to now,
                "platform" to "Android"
            )
        ).await()
    }

    private suspend fun getOrCreateDeviceId(): String {
        val prefs = context.syncDataStore.data.first()
        prefs[KEY_DEVICE_ID]?.let { return it }
        val newId = UUID.randomUUID().toString()
        context.syncDataStore.edit { it[KEY_DEVICE_ID] = newId }
        return newId
    }

    /** Fetch farm settings from Firestore and apply locally so all devices converge. Call before upload. */
    private suspend fun downloadFarmSettingsFirst(userRef: com.google.firebase.firestore.DocumentReference) {
        val settingsSnap = userRef.collection(COL_SETTINGS).document(DOC_FARM).get().await()
        settingsSnap.data?.let { data ->
            val calving = (data["calvingAlertDays"] as? Number)?.toInt() ?: FarmSettings.DEFAULT_CALVING_ALERT_DAYS
            val pregnancy = (data["pregnancyCheckDaysAfterBreeding"] as? Number)?.toInt() ?: FarmSettings.DEFAULT_PREGNANCY_CHECK_DAYS
            val gestation = (data["gestationDays"] as? Number)?.toInt() ?: FarmSettings.DEFAULT_GESTATION_DAYS
            val weaning = (data["weaningAgeDays"] as? Number)?.toInt() ?: FarmSettings.DEFAULT_WEANING_AGE_DAYS
            val contacts = parseContactsFromFirestore(data["contacts"]).ifEmpty {
                val p = data["contactPhone"] as? String ?: ""
                val e = data["contactEmail"] as? String ?: ""
                if (p.isNotBlank() || e.isNotBlank()) listOf(FarmContact("", p, e)) else emptyList()
            }
            farmSettingsRepository.updateFarmSettings(
                FarmSettings(
                    id = data["id"] as? String ?: FarmSettings.DEFAULT_FARM_ID,
                    name = data["name"] as? String ?: "",
                    address = data["address"] as? String ?: "",
                    contacts = contacts,
                    calvingAlertDays = calving.coerceIn(FarmSettings.CALVING_ALERT_DAYS_MIN, FarmSettings.CALVING_ALERT_DAYS_MAX),
                    pregnancyCheckDaysAfterBreeding = pregnancy.coerceIn(FarmSettings.PREGNANCY_CHECK_DAYS_MIN, FarmSettings.PREGNANCY_CHECK_DAYS_MAX),
                    gestationDays = gestation.coerceIn(FarmSettings.GESTATION_DAYS_MIN, FarmSettings.GESTATION_DAYS_MAX),
                    weaningAgeDays = weaning.coerceIn(FarmSettings.WEANING_AGE_DAYS_MIN, FarmSettings.WEANING_AGE_DAYS_MAX)
                )
            )
        }
    }

    private fun parseContactsFromFirestore(value: Any?): List<FarmContact> {
        if (value == null) return emptyList()
        val list = value as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            FarmContact(
                name = (map["name"] as? String).orEmpty(),
                phone = (map["phone"] as? String).orEmpty(),
                email = (map["email"] as? String).orEmpty()
            )
        }
    }

    private suspend fun uploadFarmSettings(userRef: com.google.firebase.firestore.DocumentReference) {
        val settings = farmSettingsRepository.farmSettings().first()
        val contactsPayload = settings.contacts.map { c ->
            mapOf(
                "name" to c.name,
                "phone" to c.phone,
                "email" to c.email
            )
        }
        userRef.collection(COL_SETTINGS).document(DOC_FARM).set(
            mapOf(
                "id" to settings.id,
                "name" to settings.name,
                "address" to settings.address,
                "contacts" to contactsPayload,
                "calvingAlertDays" to settings.calvingAlertDays,
                "pregnancyCheckDaysAfterBreeding" to settings.pregnancyCheckDaysAfterBreeding,
                "gestationDays" to settings.gestationDays,
                "weaningAgeDays" to settings.weaningAgeDays,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    private suspend fun uploadAnimals(userRef: com.google.firebase.firestore.DocumentReference) {
        val list = animalDao.getAll()
        list.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { e ->
                val doc = userRef.collection(COL_ANIMALS).document(e.id)
                batch.set(doc, mapOf(
                    "earTagNumber" to e.earTagNumber,
                    "rfid" to e.rfid,
                    "name" to e.name,
                    "sex" to e.sex,
                    "breed" to e.breed,
                    "dateOfBirth" to e.dateOfBirth,
                    "farmId" to e.farmId,
                    "currentHerdId" to e.currentHerdId,
                    "coatColor" to e.coatColor,
                    "hornStatus" to e.hornStatus,
                    "isCastrated" to (e.isCastrated == true),
                    "avatarPhotoId" to e.avatarPhotoId,
                    "status" to e.status,
                    "sireId" to e.sireId,
                    "damId" to e.damId,
                    "createdAt" to e.createdAt,
                    "updatedAt" to e.updatedAt,
                    "syncStatus" to e.syncStatus
                ))
            }
            batch.commit().await()
        }
    }

    private suspend fun uploadHerds(userRef: com.google.firebase.firestore.DocumentReference) {
        val list = herdDao.getAll()
        list.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { e ->
                batch.set(userRef.collection(COL_HERDS).document(e.id), mapOf(
                    "name" to e.name,
                    "farmId" to e.farmId,
                    "description" to e.description,
                    "sortOrder" to e.sortOrder,
                    "createdAt" to e.createdAt,
                    "updatedAt" to e.createdAt
                ))
            }
            batch.commit().await()
        }
    }

    private suspend fun uploadHerdAssignments(userRef: com.google.firebase.firestore.DocumentReference) {
        val list = herdAssignmentDao.getAll()
        list.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { e ->
                batch.set(userRef.collection(COL_HERD_ASSIGNMENTS).document(e.id), mapOf(
                    "animalId" to e.animalId,
                    "herdId" to e.herdId,
                    "assignedAt" to e.assignedAt,
                    "removedAt" to e.removedAt,
                    "reason" to e.reason,
                    "createdAt" to e.assignedAt,
                    "updatedAt" to (e.removedAt ?: e.assignedAt)
                ))
            }
            batch.commit().await()
        }
    }

    private suspend fun uploadBreedingEvents(userRef: com.google.firebase.firestore.DocumentReference) {
        val list = breedingEventDao.getAll()
        list.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { e ->
                batch.set(userRef.collection(COL_BREEDING_EVENTS).document(e.id), mapOf(
                    "animalId" to e.animalId,
                    "sireIds" to e.sireIds,
                    "eventType" to e.eventType,
                    "serviceDate" to e.serviceDate,
                    "notes" to e.notes,
                    "createdAt" to e.createdAt,
                    "updatedAt" to e.createdAt,
                    "pregnancyCheckDateEpochDay" to e.pregnancyCheckDateEpochDay,
                    "pregnancyCheckResult" to e.pregnancyCheckResult
                ))
            }
            batch.commit().await()
        }
    }

    private suspend fun uploadCalvingEvents(userRef: com.google.firebase.firestore.DocumentReference) {
        val list = calvingEventDao.getAll()
        list.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { e ->
                val ts = if (e.actualDate > 1e12) e.actualDate else e.actualDate * 86400_000
                batch.set(userRef.collection(COL_CALVING_EVENTS).document(e.id), mapOf(
                    "damId" to e.damId,
                    "calfId" to e.calfId,
                    "breedingEventId" to e.breedingEventId,
                    "actualDate" to e.actualDate,
                    "assistanceRequired" to e.assistanceRequired,
                    "calfSex" to e.calfSex,
                    "calfWeight" to e.calfWeight,
                    "notes" to e.notes,
                    "createdAt" to ts,
                    "updatedAt" to ts
                ))
            }
            batch.commit().await()
        }
    }

    private suspend fun uploadHealthEvents(userRef: com.google.firebase.firestore.DocumentReference) {
        val list = healthEventDao.getAll()
        list.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { e ->
                val ts = if (e.date > 1e12) e.date else e.date * 86400_000
                batch.set(userRef.collection(COL_HEALTH_EVENTS).document(e.id), mapOf(
                    "animalId" to e.animalId,
                    "eventType" to e.eventType,
                    "date" to e.date,
                    "product" to e.product,
                    "dosage" to e.dosage,
                    "withdrawalPeriodEnd" to e.withdrawalPeriodEnd,
                    "notes" to e.notes,
                    "createdAt" to ts,
                    "updatedAt" to ts
                ))
            }
            batch.commit().await()
        }
    }

    private suspend fun uploadWeightRecords(userRef: com.google.firebase.firestore.DocumentReference) {
        val list = weightRecordDao.getAll()
        list.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { e ->
                val ts = if (e.date > 1e12) e.date else e.date * 86400_000
                batch.set(userRef.collection(COL_WEIGHT_RECORDS).document(e.id), mapOf(
                    "animalId" to e.animalId,
                    "date" to e.date,
                    "weightKg" to e.weightKg,
                    "note" to e.note,
                    "createdAt" to ts,
                    "updatedAt" to ts
                ))
            }
            batch.commit().await()
        }
    }

    private suspend fun uploadPhotos(userRef: com.google.firebase.firestore.DocumentReference, uid: String) {
        val list = photoDao.getAll()
        val photosRef = storage.reference.child("users").child(uid).child("photos")
        for (e in list) {
            val storageUrl = uploadPhotoFileIfLocal(e.id, e.uri, photosRef)
            val doc = mutableMapOf<String, Any?>(
                "animalId" to e.animalId,
                "angle" to e.angle,
                "uri" to e.uri,
                "capturedAt" to e.capturedAt,
                "latitude" to e.latitude,
                "longitude" to e.longitude,
                "createdAt" to e.capturedAt,
                "updatedAt" to e.capturedAt
            )
            if (storageUrl != null) doc["storageUrl"] = storageUrl
            userRef.collection(COL_PHOTOS).document(e.id).set(doc).await()
        }
    }

    /** Upload local photo file to Storage and return download URL, or null if not a local file or upload fails. */
    private suspend fun uploadPhotoFileIfLocal(
        photoId: String,
        uri: String,
        photosRef: com.google.firebase.storage.StorageReference
    ): String? {
        val path = when {
            uri.startsWith("file://") -> uri.removePrefix("file://")
            uri.isNotEmpty() && !uri.startsWith("http") -> uri
            else -> return null
        }
        val file = File(path)
        if (!file.exists() || !file.canRead()) {
            Log.w(TAG, "Photo file missing or unreadable: $path")
            return null
        }
        val contentUri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Log.e(TAG, "FileProvider failed for $path", e)
            return null
        }
        return runCatching {
            val ref = photosRef.child("$photoId.jpg")
            ref.putFile(contentUri).await()
            ref.downloadUrl.await().toString()
        }.onFailure { e ->
            Log.e(TAG, "Storage upload failed for photo $photoId: ${e.message}", e)
        }.getOrNull()
    }

    private companion object {
        private const val TAG = "SyncRepository"
    }

    /** Download remote data and merge into local by document ID: apply remote if missing locally or if remote is newer (by updatedAt/createdAt). */
    private suspend fun downloadAndReplaceLocal(userRef: com.google.firebase.firestore.DocumentReference) {
        // Farm settings (single doc) – always apply
        val settingsSnap = userRef.collection(COL_SETTINGS).document(DOC_FARM).get().await()
        settingsSnap.data?.let { data ->
            val calving = (data["calvingAlertDays"] as? Number)?.toInt() ?: FarmSettings.DEFAULT_CALVING_ALERT_DAYS
            val pregnancy = (data["pregnancyCheckDaysAfterBreeding"] as? Number)?.toInt() ?: FarmSettings.DEFAULT_PREGNANCY_CHECK_DAYS
            val gestation = (data["gestationDays"] as? Number)?.toInt() ?: FarmSettings.DEFAULT_GESTATION_DAYS
            val weaning = (data["weaningAgeDays"] as? Number)?.toInt() ?: FarmSettings.DEFAULT_WEANING_AGE_DAYS
            val contacts = parseContactsFromFirestore(data["contacts"]).ifEmpty {
                val p = data["contactPhone"] as? String ?: ""
                val e = data["contactEmail"] as? String ?: ""
                if (p.isNotBlank() || e.isNotBlank()) listOf(FarmContact("", p, e)) else emptyList()
            }
            farmSettingsRepository.updateFarmSettings(
                FarmSettings(
                    id = data["id"] as? String ?: FarmSettings.DEFAULT_FARM_ID,
                    name = data["name"] as? String ?: "",
                    address = data["address"] as? String ?: "",
                    contacts = contacts,
                    calvingAlertDays = calving.coerceIn(FarmSettings.CALVING_ALERT_DAYS_MIN, FarmSettings.CALVING_ALERT_DAYS_MAX),
                    pregnancyCheckDaysAfterBreeding = pregnancy.coerceIn(FarmSettings.PREGNANCY_CHECK_DAYS_MIN, FarmSettings.PREGNANCY_CHECK_DAYS_MAX),
                    gestationDays = gestation.coerceIn(FarmSettings.GESTATION_DAYS_MIN, FarmSettings.GESTATION_DAYS_MAX),
                    weaningAgeDays = weaning.coerceIn(FarmSettings.WEANING_AGE_DAYS_MIN, FarmSettings.WEANING_AGE_DAYS_MAX)
                )
            )
        }

        // Herds – merge by document: apply if missing or remote.updatedAt > local.createdAt; else keep local
        val herdsSnap = userRef.collection(COL_HERDS).get().await()
        val herdsRemoteIds = herdsSnap.documents.map { it.id }.toSet()
        val herds = mutableListOf<HerdEntity>()
        for (doc in herdsSnap.documents) {
            val d = doc.data ?: continue
            val remoteUpdatedAt = (d["updatedAt"] as? Number)?.toLong() ?: (d["createdAt"] as? Number)?.toLong() ?: 0L
            val local = herdDao.getById(doc.id)
            if (local != null && remoteUpdatedAt <= local.createdAt) {
                herds.add(local)
            } else {
                herds.add(
                    HerdEntity(
                        id = doc.id,
                        name = d["name"] as? String ?: "",
                        farmId = d["farmId"] as? String ?: FarmSettings.DEFAULT_FARM_ID,
                        description = (d["description"] as? String).takeIf { !it.isNullOrBlank() },
                        sortOrder = (d["sortOrder"] as? Number)?.toInt() ?: 0,
                        createdAt = (d["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                )
            }
        }
        herdDao.getAll().filter { it.id !in herdsRemoteIds }.let { extra -> herds.addAll(extra) }

        // Animals – merge by document: apply if missing or remote.updatedAt >= local.updatedAt; else keep local
        val animalsSnap = userRef.collection(COL_ANIMALS).get().await()
        val animalsRemoteIds = animalsSnap.documents.map { it.id }.toSet()
        val animals = mutableListOf<AnimalEntity>()
        for (doc in animalsSnap.documents) {
            val d = doc.data ?: continue
            val remoteUpdatedAt = (d["updatedAt"] as? Number)?.toLong() ?: (d["createdAt"] as? Number)?.toLong() ?: 0L
            val local = animalDao.getById(doc.id)
            if (local != null && remoteUpdatedAt < local.updatedAt) {
                animals.add(local)
            } else {
                animals.add(
                    AnimalEntity(
                        id = doc.id,
                        earTagNumber = d["earTagNumber"] as? String ?: "",
                        rfid = (d["rfid"] as? String).takeIf { !it.isNullOrBlank() },
                        name = (d["name"] as? String).takeIf { !it.isNullOrBlank() },
                        sex = d["sex"] as? String ?: "FEMALE",
                        breed = d["breed"] as? String ?: "",
                        dateOfBirth = (d["dateOfBirth"] as? Number)?.toLong() ?: 0L,
                        farmId = d["farmId"] as? String ?: FarmSettings.DEFAULT_FARM_ID,
                        currentHerdId = (d["currentHerdId"] as? String).takeIf { !it.isNullOrBlank() },
                        coatColor = (d["coatColor"] as? String).takeIf { !it.isNullOrBlank() },
                        hornStatus = (d["hornStatus"] as? String).takeIf { !it.isNullOrBlank() },
                        isCastrated = when (val v = d["isCastrated"]) {
                            is Boolean -> v
                            else -> null
                        },
                        avatarPhotoId = (d["avatarPhotoId"] as? String).takeIf { !it.isNullOrBlank() },
                        status = d["status"] as? String ?: "ACTIVE",
                        sireId = (d["sireId"] as? String).takeIf { !it.isNullOrBlank() },
                        damId = (d["damId"] as? String).takeIf { !it.isNullOrBlank() },
                        createdAt = (d["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        updatedAt = (d["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        syncStatus = d["syncStatus"] as? String ?: "SYNCED"
                    )
                )
            }
        }
        animalDao.getAll().filter { it.id !in animalsRemoteIds }.let { extra -> animals.addAll(extra) }

        // Herd assignments – apply all remote (no local timestamp; last-writer-wins per doc)
        val assignmentsSnap = userRef.collection(COL_HERD_ASSIGNMENTS).get().await()
        val assignments = assignmentsSnap.documents.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            HerdAssignmentEntity(
                id = doc.id,
                animalId = d["animalId"] as? String ?: return@mapNotNull null,
                herdId = d["herdId"] as? String ?: return@mapNotNull null,
                assignedAt = (d["assignedAt"] as? Number)?.toLong() ?: return@mapNotNull null,
                removedAt = (d["removedAt"] as? Number)?.toLong()?.takeIf { it > 0 },
                reason = (d["reason"] as? String).takeIf { !it.isNullOrBlank() }
            )
        }

        // Breeding events – merge: apply if missing or remote.updatedAt > local.createdAt; else keep local
        val breedingSnap = userRef.collection(COL_BREEDING_EVENTS).get().await()
        val breedingRemoteIds = breedingSnap.documents.map { it.id }.toSet()
        val breedingEvents = mutableListOf<BreedingEventEntity>()
        for (doc in breedingSnap.documents) {
            val d = doc.data ?: continue
            val remoteUpdatedAt = (d["updatedAt"] as? Number)?.toLong() ?: (d["createdAt"] as? Number)?.toLong() ?: 0L
            val local = breedingEventDao.getById(doc.id)
            if (local != null && remoteUpdatedAt <= local.createdAt) {
                breedingEvents.add(local)
            } else {
                val animalId = d["animalId"] as? String ?: continue
                val serviceDate = (d["serviceDate"] as? Number)?.toLong() ?: continue
                val sireIds = (d["sireIds"] as? List<*>)?.map { it.toString() }?.filter { it.isNotBlank() } ?: emptyList()
                breedingEvents.add(
                    BreedingEventEntity(
                        id = doc.id,
                        animalId = animalId,
                        sireIds = sireIds,
                        eventType = d["eventType"] as? String ?: "AI",
                        serviceDate = serviceDate,
                        notes = (d["notes"] as? String).takeIf { !it.isNullOrBlank() },
                        createdAt = (d["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        pregnancyCheckDateEpochDay = (d["pregnancyCheckDateEpochDay"] as? Number)?.toLong()?.takeIf { it > 0 },
                        pregnancyCheckResult = (d["pregnancyCheckResult"] as? String).takeIf { !it.isNullOrBlank() }
                    )
                )
            }
        }
        breedingEventDao.getAll().filter { it.id !in breedingRemoteIds }.let { extra -> breedingEvents.addAll(extra) }

        // Calving events – apply all remote
        val calvingSnap = userRef.collection(COL_CALVING_EVENTS).get().await()
        val calvingEvents = calvingSnap.documents.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            CalvingEventEntity(
                id = doc.id,
                damId = d["damId"] as? String ?: return@mapNotNull null,
                calfId = (d["calfId"] as? String).takeIf { !it.isNullOrBlank() },
                breedingEventId = d["breedingEventId"] as? String ?: return@mapNotNull null,
                actualDate = (d["actualDate"] as? Number)?.toLong() ?: return@mapNotNull null,
                assistanceRequired = d["assistanceRequired"] as? Boolean ?: false,
                calfSex = (d["calfSex"] as? String).takeIf { !it.isNullOrBlank() },
                calfWeight = (d["calfWeight"] as? Number)?.toDouble()?.takeIf { !it.isNaN() },
                notes = (d["notes"] as? String).takeIf { !it.isNullOrBlank() }
            )
        }

        // Health events – apply all remote
        val healthSnap = userRef.collection(COL_HEALTH_EVENTS).get().await()
        val healthEvents = healthSnap.documents.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            HealthEventEntity(
                id = doc.id,
                animalId = d["animalId"] as? String ?: return@mapNotNull null,
                eventType = d["eventType"] as? String ?: "",
                date = (d["date"] as? Number)?.toLong() ?: return@mapNotNull null,
                product = (d["product"] as? String).takeIf { !it.isNullOrBlank() },
                dosage = (d["dosage"] as? String).takeIf { !it.isNullOrBlank() },
                withdrawalPeriodEnd = (d["withdrawalPeriodEnd"] as? Number)?.toLong()?.takeIf { it > 0 },
                notes = (d["notes"] as? String).takeIf { !it.isNullOrBlank() }
            )
        }

        // Weight records – apply all remote
        val weightSnap = userRef.collection(COL_WEIGHT_RECORDS).get().await()
        val weightRecords = weightSnap.documents.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            WeightRecordEntity(
                id = doc.id,
                animalId = d["animalId"] as? String ?: return@mapNotNull null,
                date = (d["date"] as? Number)?.toLong() ?: return@mapNotNull null,
                weightKg = (d["weightKg"] as? Number)?.toDouble() ?: return@mapNotNull null,
                note = (d["note"] as? String).takeIf { !it.isNullOrBlank() }
            )
        }

        // Photos – apply all remote; prefer storageUrl
        val photosSnap = userRef.collection(COL_PHOTOS).get().await()
        val photos = photosSnap.documents.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            val uri = (d["storageUrl"] as? String)?.takeIf { it.isNotBlank() }
                ?: (d["uri"] as? String) ?: return@mapNotNull null
            val capturedAt = (d["capturedAt"] as? Number)?.toLong() ?: return@mapNotNull null
            PhotoEntity(
                id = doc.id,
                animalId = d["animalId"] as? String ?: return@mapNotNull null,
                angle = d["angle"] as? String ?: "LEFT_SIDE",
                uri = uri,
                capturedAt = capturedAt,
                latitude = (d["latitude"] as? Number)?.toDouble()?.takeIf { !it.isNaN() },
                longitude = (d["longitude"] as? Number)?.toDouble()?.takeIf { !it.isNaN() }
            )
        }

        // Replace local with merged sets (FK-safe order: delete children first, then parents; insert parents first)
        photoDao.deleteAll()
        weightRecordDao.deleteAll()
        healthEventDao.deleteAll()
        calvingEventDao.deleteAll()
        breedingEventDao.deleteAll()
        herdAssignmentDao.deleteAll()
        animalDao.deleteAll()
        herdDao.deleteAll()

        if (herds.isNotEmpty()) herdDao.insertAll(herds)
        if (animals.isNotEmpty()) animalDao.insertAll(animals)
        if (assignments.isNotEmpty()) herdAssignmentDao.insertAll(assignments)
        if (breedingEvents.isNotEmpty()) breedingEventDao.insertAll(breedingEvents)
        if (calvingEvents.isNotEmpty()) calvingEventDao.insertAll(calvingEvents)
        if (healthEvents.isNotEmpty()) healthEventDao.insertAll(healthEvents)
        if (weightRecords.isNotEmpty()) weightRecordDao.insertAll(weightRecords)
        if (photos.isNotEmpty()) photoDao.insertAll(photos)
    }
}
