package com.herdmanager.app.data.repository

import com.herdmanager.app.data.local.dao.AnimalDao
import com.herdmanager.app.data.local.dao.BreedingEventDao
import com.herdmanager.app.data.local.dao.HerdAssignmentDao
import com.herdmanager.app.data.local.dao.HerdDao
import com.herdmanager.app.data.local.dao.CalvingEventDao
import com.herdmanager.app.data.local.dao.HealthEventDao
import com.herdmanager.app.data.local.dao.ConditionRecordDao
import com.herdmanager.app.data.local.dao.ExpenseCategoryDao
import com.herdmanager.app.data.local.dao.PhotoDao
import com.herdmanager.app.data.local.dao.FarmTaskDao
import com.herdmanager.app.data.local.dao.TransactionDao
import com.herdmanager.app.data.local.dao.WeightRecordDao
import com.herdmanager.app.data.local.entity.AnimalEntity
import com.herdmanager.app.data.local.entity.BreedingEventEntity
import com.herdmanager.app.data.local.entity.CalvingEventEntity
import com.herdmanager.app.data.local.entity.HealthEventEntity
import com.herdmanager.app.data.local.entity.ConditionRecordEntity
import com.herdmanager.app.data.local.entity.HerdAssignmentEntity
import com.herdmanager.app.data.local.entity.HerdEntity
import com.herdmanager.app.data.local.entity.ExpenseCategoryEntity
import com.herdmanager.app.data.local.entity.PhotoEntity
import com.herdmanager.app.data.local.entity.FarmTaskEntity
import com.herdmanager.app.data.local.entity.TransactionEntity
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
    private val conditionRecordDao: ConditionRecordDao,
    private val weightRecordDao: WeightRecordDao,
    private val photoDao: PhotoDao,
    private val transactionDao: TransactionDao,
    private val expenseCategoryDao: ExpenseCategoryDao,
    private val farmTaskDao: FarmTaskDao,
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
        val conditionRecords = conditionRecordDao.getAll()
        val weightRecords = weightRecordDao.getAll()
        val photos = photoDao.getAll()
        val transactions = transactionDao.getAll()
        val expenseCategories = expenseCategoryDao.getAll()
        val farmTasks = farmTaskDao.getAll()

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
                put("weaningAgeDays", settings.weaningAgeDays)
                put("currencyCode", settings.currencyCode.ifBlank { FarmSettings.DEFAULT_CURRENCY_CODE })
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
                        e.sireId?.let { put("sireId", it) }
                        e.damId?.let { put("damId", it) }
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
                        put("createdAt", e.createdAt)
                        put("updatedAt", if (e.updatedAt > 0L) e.updatedAt else e.createdAt)
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
                        put("updatedAt", if (e.updatedAt > 0L) e.updatedAt else (e.removedAt ?: e.assignedAt))
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
                        put("updatedAt", if (e.updatedAt > 0L) e.updatedAt else e.createdAt)
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
                        put("updatedAt", if (e.updatedAt > 0L) e.updatedAt else e.actualDate * 86400_000)
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
                        put("updatedAt", if (e.updatedAt > 0L) e.updatedAt else e.date * 86400_000)
                    })
                }
            })
            put("conditionRecords", JSONArray().apply {
                conditionRecords.forEach { e ->
                    put(JSONObject().apply {
                        put("id", e.id)
                        put("animalId", e.animalId)
                        put("dateEpochDay", e.dateEpochDay)
                        put("score", e.score)
                        put("notes", e.notes)
                        put("updatedAt", if (e.updatedAt > 0L) e.updatedAt else e.dateEpochDay * 86400_000)
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
                        put("updatedAt", if (e.updatedAt > 0L) e.updatedAt else e.date * 86400_000)
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
                        put("updatedAt", if (e.updatedAt > 0L) e.updatedAt else e.capturedAt)
                        e.latitude?.let { put("latitude", it) }
                        e.longitude?.let { put("longitude", it) }
                    })
                }
            })
            put("transactions", JSONArray().apply {
                transactions.forEach { e ->
                    put(JSONObject().apply {
                        put("id", e.id)
                        put("type", e.type)
                        put("amountCents", e.amountCents)
                        put("dateEpochDay", e.dateEpochDay)
                        put("farmId", e.farmId)
                        e.notes?.let { put("notes", it) }
                        put("createdAt", e.createdAt)
                        put("updatedAt", if (e.updatedAt > 0L) e.updatedAt else e.createdAt)
                        e.weightKg?.let { put("weightKg", it) }
                        e.pricePerKgCents?.let { put("pricePerKgCents", it) }
                        e.animalId?.let { put("animalId", it) }
                        e.contactName?.let { put("contactName", it) }
                        e.contactPhone?.let { put("contactPhone", it) }
                        e.contactEmail?.let { put("contactEmail", it) }
                        e.categoryId?.let { put("categoryId", it) }
                        e.description?.let { put("description", it) }
                    })
                }
            })
            put("expenseCategories", JSONArray().apply {
                expenseCategories.forEach { e ->
                    put(JSONObject().apply {
                        put("id", e.id)
                        put("name", e.name)
                        put("farmId", e.farmId)
                        put("sortOrder", e.sortOrder)
                        put("createdAt", e.createdAt)
                        put("updatedAt", if (e.updatedAt > 0L) e.updatedAt else e.createdAt)
                    })
                }
            })
            put("farmTasks", JSONArray().apply {
                farmTasks.forEach { e ->
                    put(JSONObject().apply {
                        put("id", e.id)
                        put("title", e.title)
                        e.notes?.let { put("notes", it) }
                        e.dueDateEpochDay?.let { put("dueDateEpochDay", it) }
                        put("status", e.status)
                        e.animalId?.let { put("animalId", it) }
                        e.priority?.let { put("priority", it) }
                        put("createdAt", e.createdAt)
                        put("updatedAt", e.updatedAt)
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
        conditionRecordDao.deleteAll()
        calvingEventDao.deleteAll()
        breedingEventDao.deleteAll()
        herdAssignmentDao.deleteAll()
        transactionDao.deleteAll()
        animalDao.deleteAll()
        herdDao.deleteAll()
        expenseCategoryDao.deleteAll()
        farmTaskDao.deleteAll()

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
                        .coerceIn(FarmSettings.GESTATION_DAYS_MIN, FarmSettings.GESTATION_DAYS_MAX),
                    weaningAgeDays = o.optInt("weaningAgeDays", FarmSettings.DEFAULT_WEANING_AGE_DAYS)
                        .coerceIn(FarmSettings.WEANING_AGE_DAYS_MIN, FarmSettings.WEANING_AGE_DAYS_MAX),
                    currencyCode = o.optString("currencyCode", FarmSettings.DEFAULT_CURRENCY_CODE).ifBlank { FarmSettings.DEFAULT_CURRENCY_CODE }
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
                        sireId = o.optString("sireId").takeIf { it.isNotEmpty() },
                        damId = o.optString("damId").takeIf { it.isNotEmpty() },
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
                val createdAt = o.optLong("createdAt", now)
                val updatedAt = o.optLong("updatedAt", createdAt)
                herdDao.insert(
                    HerdEntity(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        farmId = o.optString("farmId", FarmSettings.DEFAULT_FARM_ID),
                        description = o.optString("description").takeIf { it.isNotEmpty() },
                        sortOrder = o.optInt("sortOrder", 0),
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )
                )
            }
        }

        // Herd assignments
        root.optJSONArray("herdAssignments")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val assignedAt = o.getLong("assignedAt")
                val removedAt = if (o.has("removedAt") && !o.isNull("removedAt")) o.getLong("removedAt") else null
                val updatedAt = o.optLong("updatedAt", -1).takeIf { it >= 0 } ?: (removedAt ?: assignedAt)
                herdAssignmentDao.insert(
                    HerdAssignmentEntity(
                        id = o.getString("id"),
                        animalId = o.getString("animalId"),
                        herdId = o.getString("herdId"),
                        assignedAt = assignedAt,
                        removedAt = removedAt,
                        reason = o.optString("reason").takeIf { it.isNotEmpty() },
                        updatedAt = updatedAt
                    )
                )
            }
        }

        // Breeding events
        root.optJSONArray("breedingEvents")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val sireIds = if (o.has("sireIds")) {
                    o.getJSONArray("sireIds").let { a -> (0 until a.length()).map { a.getString(it) } }
                } else {
                    o.optString("sireId").takeIf { it.isNotEmpty() }?.let { listOf(it) } ?: emptyList()
                }
                val createdAt = o.optLong("createdAt", now)
                val updatedAt = o.optLong("updatedAt", createdAt)
                breedingEventDao.insert(
                    BreedingEventEntity(
                        id = o.getString("id"),
                        animalId = o.getString("animalId"),
                        sireIds = sireIds,
                        eventType = o.getString("eventType"),
                        serviceDate = o.getLong("serviceDate"),
                        notes = o.optString("notes").takeIf { it.isNotEmpty() },
                        createdAt = createdAt,
                        updatedAt = updatedAt,
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
                val actualDate = o.getLong("actualDate")
                val updatedAt = o.optLong("updatedAt", -1).takeIf { it >= 0 } ?: (actualDate * 86400_000)
                calvingEventDao.insert(
                    CalvingEventEntity(
                        id = o.getString("id"),
                        damId = o.getString("damId"),
                        calfId = o.optString("calfId").takeIf { it.isNotEmpty() },
                        breedingEventId = o.getString("breedingEventId"),
                        actualDate = actualDate,
                        assistanceRequired = o.optBoolean("assistanceRequired", false),
                        calfSex = o.optString("calfSex").takeIf { it.isNotEmpty() },
                        calfWeight = if (o.has("calfWeight") && !o.isNull("calfWeight")) o.getDouble("calfWeight") else null,
                        notes = o.optString("notes").takeIf { it.isNotEmpty() },
                        updatedAt = updatedAt
                    )
                )
            }
        }

        // Health events
        root.optJSONArray("healthEvents")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val date = o.getLong("date")
                val updatedAt = o.optLong("updatedAt", -1).takeIf { it >= 0 } ?: (date * 86400_000)
                healthEventDao.insert(
                    HealthEventEntity(
                        id = o.getString("id"),
                        animalId = o.getString("animalId"),
                        eventType = o.getString("eventType"),
                        date = date,
                        product = o.optString("product").takeIf { it.isNotEmpty() },
                        dosage = o.optString("dosage").takeIf { it.isNotEmpty() },
                        withdrawalPeriodEnd = o.optLong("withdrawalPeriodEnd", -1).takeIf { it >= 0 },
                        notes = o.optString("notes").takeIf { it.isNotEmpty() },
                        updatedAt = updatedAt
                    )
                )
            }
        }

        // Weight records
        root.optJSONArray("weightRecords")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val date = o.getLong("date")
                val updatedAt = o.optLong("updatedAt", -1).takeIf { it >= 0 } ?: (date * 86400_000)
                weightRecordDao.insert(
                    WeightRecordEntity(
                        id = o.getString("id"),
                        animalId = o.getString("animalId"),
                        date = date,
                        weightKg = o.getDouble("weightKg"),
                        note = o.optString("note").takeIf { it.isNotEmpty() },
                        updatedAt = updatedAt
                    )
                )
            }
        }

        // Condition records
        root.optJSONArray("conditionRecords")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val dateEpochDay = o.getLong("dateEpochDay")
                val updatedAt = o.optLong("updatedAt", -1).takeIf { it >= 0 } ?: (dateEpochDay * 86400_000)
                conditionRecordDao.insert(
                    ConditionRecordEntity(
                        id = o.getString("id"),
                        animalId = o.getString("animalId"),
                        dateEpochDay = dateEpochDay,
                        score = o.getInt("score"),
                        notes = o.optString("notes").takeIf { it.isNotEmpty() },
                        updatedAt = updatedAt
                    )
                )
            }
        }

        // Photos
        root.optJSONArray("photos")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val capturedAt = o.getLong("capturedAt")
                val updatedAt = o.optLong("updatedAt", -1).takeIf { it >= 0 } ?: capturedAt
                photoDao.insert(
                    PhotoEntity(
                        id = o.getString("id"),
                        animalId = o.getString("animalId"),
                        angle = o.getString("angle"),
                        uri = o.getString("uri"),
                        capturedAt = capturedAt,
                        latitude = o.optDouble("latitude", Double.NaN).takeIf { !it.isNaN() },
                        longitude = o.optDouble("longitude", Double.NaN).takeIf { !it.isNaN() },
                        updatedAt = updatedAt
                    )
                )
            }
        }

        // Expense categories (before transactions so categoryId refs exist)
        root.optJSONArray("expenseCategories")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val createdAt = o.optLong("createdAt", now)
                val updatedAt = o.optLong("updatedAt", createdAt)
                expenseCategoryDao.insert(
                    ExpenseCategoryEntity(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        farmId = o.optString("farmId", FarmSettings.DEFAULT_FARM_ID),
                        sortOrder = o.optInt("sortOrder", 0),
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )
                )
            }
        }

        // Farm tasks
        root.optJSONArray("farmTasks")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                farmTaskDao.insert(
                    FarmTaskEntity(
                        id = o.getString("id"),
                        title = o.getString("title"),
                        notes = o.optString("notes").takeIf { it.isNotEmpty() },
                        dueDateEpochDay = if (o.has("dueDateEpochDay") && !o.isNull("dueDateEpochDay")) o.getLong("dueDateEpochDay") else null,
                        status = o.optString("status", "PENDING"),
                        animalId = o.optString("animalId").takeIf { it.isNotEmpty() },
                        priority = o.optString("priority").takeIf { it.isNotEmpty() },
                        createdAt = o.optLong("createdAt", now),
                        updatedAt = o.optLong("updatedAt", now)
                    )
                )
            }
        }

        // Transactions
        root.optJSONArray("transactions")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val createdAt = o.optLong("createdAt", now)
                val updatedAt = o.optLong("updatedAt", createdAt)
                transactionDao.insert(
                    TransactionEntity(
                        id = o.getString("id"),
                        type = o.optString("type", "EXPENSE"),
                        amountCents = o.optLong("amountCents", 0L),
                        dateEpochDay = o.optLong("dateEpochDay", 0L),
                        farmId = o.optString("farmId", FarmSettings.DEFAULT_FARM_ID),
                        notes = o.optString("notes").takeIf { it.isNotEmpty() },
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        weightKg = if (o.has("weightKg") && !o.isNull("weightKg")) o.getDouble("weightKg") else null,
                        pricePerKgCents = if (o.has("pricePerKgCents") && !o.isNull("pricePerKgCents")) o.getLong("pricePerKgCents") else null,
                        animalId = o.optString("animalId").takeIf { it.isNotEmpty() },
                        contactName = o.optString("contactName").takeIf { it.isNotEmpty() },
                        contactPhone = o.optString("contactPhone").takeIf { it.isNotEmpty() },
                        contactEmail = o.optString("contactEmail").takeIf { it.isNotEmpty() },
                        categoryId = o.optString("categoryId").takeIf { it.isNotEmpty() },
                        description = o.optString("description").takeIf { it.isNotEmpty() }
                    )
                )
            }
        }
    }
}
