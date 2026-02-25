package com.herdmanager.app.data.repository

import com.herdmanager.app.data.local.dao.AnimalDao
import com.herdmanager.app.data.local.dao.BreedingEventDao
import com.herdmanager.app.data.local.dao.HerdAssignmentDao
import com.herdmanager.app.data.local.dao.HerdDao
import com.herdmanager.app.data.local.dao.CalvingEventDao
import com.herdmanager.app.data.local.dao.HealthEventDao
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
import com.herdmanager.app.domain.repository.BackupRepository
import com.herdmanager.app.domain.repository.FarmSettingsRepository
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

class BackupRepositoryImpl(
    private val animalDao: AnimalDao,
    private val herdDao: HerdDao,
    private val herdAssignmentDao: HerdAssignmentDao,
    private val breedingEventDao: BreedingEventDao,
    private val calvingEventDao: CalvingEventDao,
    private val healthEventDao: HealthEventDao,
    private val weightRecordDao: WeightRecordDao,
    private val photoDao: PhotoDao,
    private val farmSettingsRepository: FarmSettingsRepository
) : BackupRepository {

    override suspend fun exportToJson(): String {
        val settings = farmSettingsRepository.farmSettings().first()
        val animals = animalDao.getAll()
        val herds = herdDao.getAll()
        val herdAssignments = herdAssignmentDao.getAll()
        val breedingEvents = breedingEventDao.getAll()
        val calvingEvents = calvingEventDao.getAll()
        val healthEvents = healthEventDao.getAll()
        val weightRecords = weightRecordDao.getAll()
        val photos = photoDao.getAll()

        val root = JSONObject().apply {
            put("version", 1)
            put("exportedAt", Instant.now().toString())
            put("farmSettings", JSONObject().apply {
                put("id", settings.id)
                put("name", settings.name)
                put("address", settings.address)
                put("contacts", JSONArray().apply {
                    settings.contacts.forEach { c ->
                        put(JSONObject().apply {
                            put("name", c.name)
                            put("phone", c.phone)
                            put("email", c.email)
                        })
                    }
                })
                put("calvingAlertDays", settings.calvingAlertDays)
                put("pregnancyCheckDaysAfterBreeding", settings.pregnancyCheckDaysAfterBreeding)
                put("gestationDays", settings.gestationDays)
            })
            put("animals", JSONArray().apply {
                animals.forEach { e ->
                    put(JSONObject().apply {
                        put("id", e.id)
                        put("earTagNumber", e.earTagNumber)
                        put("rfid", e.rfid)
                        put("name", e.name)
                        put("sex", e.sex)
                        put("breed", e.breed)
                        put("dateOfBirth", e.dateOfBirth)
                        put("farmId", e.farmId)
                        e.currentHerdId?.let { put("currentHerdId", it) }
                        put("coatColor", e.coatColor)
                        put("hornStatus", e.hornStatus)
                        e.isCastrated?.let { put("isCastrated", it) }
                        e.avatarPhotoId?.let { put("avatarPhotoId", it) }
                        put("status", e.status)
                    })
                }
            })
            put("herds", JSONArray().apply {
                herds.forEach { e ->
                    put(JSONObject().apply {
                        put("id", e.id)
                        put("name", e.name)
                        put("farmId", e.farmId)
                        e.description?.let { put("description", it) }
                        put("sortOrder", e.sortOrder)
                    })
                }
            })
            put("herdAssignments", JSONArray().apply {
                herdAssignments.forEach { e ->
                    put(JSONObject().apply {
                        put("id", e.id)
                        put("animalId", e.animalId)
                        put("herdId", e.herdId)
                        put("assignedAt", e.assignedAt)
                        e.removedAt?.let { put("removedAt", it) }
                        e.reason?.let { put("reason", it) }
                    })
                }
            })
            put("breedingEvents", JSONArray().apply {
                breedingEvents.forEach { e ->
                    put(JSONObject().apply {
                        put("id", e.id)
                        put("animalId", e.animalId)
                        put("sireIds", org.json.JSONArray(e.sireIds))
                        put("eventType", e.eventType)
                        put("serviceDate", e.serviceDate)
                        put("notes", e.notes)
                        put("createdAt", e.createdAt)
                        e.pregnancyCheckDateEpochDay?.let { put("pregnancyCheckDateEpochDay", it) }
                        e.pregnancyCheckResult?.let { put("pregnancyCheckResult", it) }
                    })
                }
            })
            put("calvingEvents", JSONArray().apply {
                calvingEvents.forEach { e ->
                    put(JSONObject().apply {
                        put("id", e.id)
                        put("damId", e.damId)
                        put("calfId", e.calfId)
                        put("breedingEventId", e.breedingEventId)
                        put("actualDate", e.actualDate)
                        put("assistanceRequired", e.assistanceRequired)
                        put("calfSex", e.calfSex)
                        put("calfWeight", e.calfWeight)
                        put("notes", e.notes)
                    })
                }
            })
            put("healthEvents", JSONArray().apply {
                healthEvents.forEach { e ->
                    put(JSONObject().apply {
                        put("id", e.id)
                        put("animalId", e.animalId)
                        put("eventType", e.eventType)
                        put("date", e.date)
                        put("product", e.product)
                        put("dosage", e.dosage)
                        put("withdrawalPeriodEnd", e.withdrawalPeriodEnd)
                        put("notes", e.notes)
                    })
                }
            })
            put("weightRecords", JSONArray().apply {
                weightRecords.forEach { e ->
                    put(JSONObject().apply {
                        put("id", e.id)
                        put("animalId", e.animalId)
                        put("date", e.date)
                        put("weightKg", e.weightKg)
                        put("note", e.note)
                    })
                }
            })
            put("photos", JSONArray().apply {
                photos.forEach { e ->
                    put(JSONObject().apply {
                        put("id", e.id)
                        put("animalId", e.animalId)
                        put("angle", e.angle)
                        put("uri", e.uri)
                        put("capturedAt", e.capturedAt)
                        e.latitude?.let { put("latitude", it) }
                        e.longitude?.let { put("longitude", it) }
                    })
                }
            })
        }
        return root.toString(2)
    }

    override suspend fun importFromJson(json: String): Result<Unit> = runCatching {
        val root = JSONObject(json)
        val now = System.currentTimeMillis()

        // Clear in FK-safe order (children first)
        photoDao.deleteAll()
        weightRecordDao.deleteAll()
        healthEventDao.deleteAll()
        calvingEventDao.deleteAll()
        breedingEventDao.deleteAll()
        animalDao.deleteAll()
        herdAssignmentDao.deleteAll()
        herdDao.deleteAll()

        // Farm settings
        root.optJSONObject("farmSettings")?.let { o ->
            val contacts = if (o.has("contacts")) {
                o.getJSONArray("contacts").let { arr ->
                    (0 until arr.length()).map { i ->
                        val c = arr.getJSONObject(i)
                        FarmContact(
                            name = c.optString("name", ""),
                            phone = c.optString("phone", ""),
                            email = c.optString("email", "")
                        )
                    }
                }
            } else {
                val p = o.optString("contactPhone", "")
                val e = o.optString("contactEmail", "")
                if (p.isNotBlank() || e.isNotBlank()) listOf(FarmContact("", p, e)) else emptyList()
            }
            farmSettingsRepository.updateFarmSettings(
                FarmSettings(
                    id = o.optString("id", FarmSettings.DEFAULT_FARM_ID),
                    name = o.optString("name", ""),
                    address = o.optString("address", ""),
                    contacts = contacts,
                    calvingAlertDays = o.optInt("calvingAlertDays", FarmSettings.DEFAULT_CALVING_ALERT_DAYS)
                        .coerceIn(FarmSettings.CALVING_ALERT_DAYS_MIN, FarmSettings.CALVING_ALERT_DAYS_MAX),
                    pregnancyCheckDaysAfterBreeding = o.optInt("pregnancyCheckDaysAfterBreeding", FarmSettings.DEFAULT_PREGNANCY_CHECK_DAYS)
                        .coerceIn(FarmSettings.PREGNANCY_CHECK_DAYS_MIN, FarmSettings.PREGNANCY_CHECK_DAYS_MAX),
                    gestationDays = o.optInt("gestationDays", FarmSettings.DEFAULT_GESTATION_DAYS)
                        .coerceIn(FarmSettings.GESTATION_DAYS_MIN, FarmSettings.GESTATION_DAYS_MAX)
                )
            )
        }

        // Animals
        root.optJSONArray("animals")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                animalDao.insert(
                    AnimalEntity(
                        id = o.getString("id"),
                        earTagNumber = o.getString("earTagNumber"),
                        rfid = o.optString("rfid").takeIf { it.isNotEmpty() },
                        name = o.optString("name").takeIf { it.isNotEmpty() },
                        sex = o.getString("sex"),
                        breed = o.getString("breed"),
                        dateOfBirth = o.getLong("dateOfBirth"),
                        farmId = o.optString("farmId", FarmSettings.DEFAULT_FARM_ID),
                        currentHerdId = o.optString("currentHerdId").takeIf { it.isNotEmpty() },
                        coatColor = o.optString("coatColor").takeIf { it.isNotEmpty() },
                        hornStatus = o.optString("hornStatus").takeIf { it.isNotEmpty() },
                        isCastrated = if (o.has("isCastrated")) o.getBoolean("isCastrated") else null,
                        avatarPhotoId = o.optString("avatarPhotoId").takeIf { it.isNotEmpty() },
                        status = o.getString("status"),
                        createdAt = now,
                        updatedAt = now,
                        syncStatus = "PENDING"
                    )
                )
            }
        }

        // Herds
        root.optJSONArray("herds")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                herdDao.insert(
                    HerdEntity(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        farmId = o.optString("farmId", FarmSettings.DEFAULT_FARM_ID),
                        description = o.optString("description").takeIf { it.isNotEmpty() },
                        sortOrder = o.optInt("sortOrder", 0)
                    )
                )
            }
        }

        // Herd assignments
        root.optJSONArray("herdAssignments")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                herdAssignmentDao.insert(
                    HerdAssignmentEntity(
                        id = o.getString("id"),
                        animalId = o.getString("animalId"),
                        herdId = o.getString("herdId"),
                        assignedAt = o.getLong("assignedAt"),
                        removedAt = if (o.has("removedAt") && !o.isNull("removedAt")) o.getLong("removedAt") else null,
                        reason = o.optString("reason").takeIf { it.isNotEmpty() }
                    )
                )
            }
        }

        // Breeding events
        root.optJSONArray("breedingEvents")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val sireIds = if (o.has("sireIds")) {
                    o.getJSONArray("sireIds").let { arr -> (0 until arr.length()).map { arr.getString(it) } }
                } else {
                    o.optString("sireId").takeIf { it.isNotEmpty() }?.let { listOf(it) } ?: emptyList()
                }
                breedingEventDao.insert(
                    BreedingEventEntity(
                        id = o.getString("id"),
                        animalId = o.getString("animalId"),
                        sireIds = sireIds,
                        eventType = o.getString("eventType"),
                        serviceDate = o.getLong("serviceDate"),
                        notes = o.optString("notes").takeIf { it.isNotEmpty() },
                        createdAt = o.optLong("createdAt", now),
                        pregnancyCheckDateEpochDay = if (o.has("pregnancyCheckDateEpochDay") && !o.isNull("pregnancyCheckDateEpochDay")) o.getLong("pregnancyCheckDateEpochDay") else null,
                        pregnancyCheckResult = o.optString("pregnancyCheckResult").takeIf { it.isNotEmpty() }
                    )
                )
            }
        }

        // Calving events
        root.optJSONArray("calvingEvents")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                calvingEventDao.insert(
                    CalvingEventEntity(
                        id = o.getString("id"),
                        damId = o.getString("damId"),
                        calfId = o.optString("calfId").takeIf { it.isNotEmpty() },
                        breedingEventId = o.getString("breedingEventId"),
                        actualDate = o.getLong("actualDate"),
                        assistanceRequired = o.optBoolean("assistanceRequired", false),
                        calfSex = o.optString("calfSex").takeIf { it.isNotEmpty() },
                        calfWeight = if (o.has("calfWeight") && !o.isNull("calfWeight")) o.getDouble("calfWeight") else null,
                        notes = o.optString("notes").takeIf { it.isNotEmpty() }
                    )
                )
            }
        }

        // Health events
        root.optJSONArray("healthEvents")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                healthEventDao.insert(
                    HealthEventEntity(
                        id = o.getString("id"),
                        animalId = o.getString("animalId"),
                        eventType = o.getString("eventType"),
                        date = o.getLong("date"),
                        product = o.optString("product").takeIf { it.isNotEmpty() },
                        dosage = o.optString("dosage").takeIf { it.isNotEmpty() },
                        withdrawalPeriodEnd = o.optLong("withdrawalPeriodEnd", -1).takeIf { it >= 0 },
                        notes = o.optString("notes").takeIf { it.isNotEmpty() }
                    )
                )
            }
        }

        // Weight records
        root.optJSONArray("weightRecords")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                weightRecordDao.insert(
                    WeightRecordEntity(
                        id = o.getString("id"),
                        animalId = o.getString("animalId"),
                        date = o.getLong("date"),
                        weightKg = o.getDouble("weightKg"),
                        note = o.optString("note").takeIf { it.isNotEmpty() }
                    )
                )
            }
        }

        // Photos
        root.optJSONArray("photos")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                photoDao.insert(
                    PhotoEntity(
                        id = o.getString("id"),
                        animalId = o.getString("animalId"),
                        angle = o.getString("angle"),
                        uri = o.getString("uri"),
                        capturedAt = o.getLong("capturedAt"),
                        latitude = o.optDouble("latitude", Double.NaN).takeIf { !it.isNaN() },
                        longitude = o.optDouble("longitude", Double.NaN).takeIf { !it.isNaN() }
                    )
                )
            }
        }
    }
}
