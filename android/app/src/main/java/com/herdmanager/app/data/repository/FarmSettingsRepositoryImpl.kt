package com.herdmanager.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.herdmanager.app.domain.model.FarmContact
import com.herdmanager.app.domain.model.FarmSettings
import com.herdmanager.app.domain.repository.FarmSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.farmSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "farm_settings"
)

private val KEY_FARM_NAME = stringPreferencesKey("farm_name")
private val KEY_FARM_ADDRESS = stringPreferencesKey("farm_address")
private val KEY_FARM_CONTACTS = stringPreferencesKey("farm_contacts")
private val KEY_FARM_PHONE = stringPreferencesKey("farm_phone")
private val KEY_FARM_EMAIL = stringPreferencesKey("farm_email")
private val KEY_CALVING_ALERT_DAYS = intPreferencesKey("calving_alert_days")
private val KEY_PREGNANCY_CHECK_DAYS = intPreferencesKey("pregnancy_check_days")
private val KEY_GESTATION_DAYS = intPreferencesKey("gestation_days")
private val KEY_WEANING_AGE_DAYS = intPreferencesKey("weaning_age_days")

class FarmSettingsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : FarmSettingsRepository {

    override fun farmSettings(): Flow<FarmSettings> =
        context.farmSettingsDataStore.data.map { prefs ->
            val contactsJson = prefs[KEY_FARM_CONTACTS]
            val contacts = if (contactsJson.isNullOrBlank()) {
                emptyList()
            } else {
                parseContactsJson(contactsJson)
            }
            val contactsOrLegacy = if (contacts.isEmpty()) {
                val phone = prefs[KEY_FARM_PHONE] ?: ""
                val email = prefs[KEY_FARM_EMAIL] ?: ""
                if (phone.isNotBlank() || email.isNotBlank()) listOf(FarmContact("", phone, email))
                else emptyList()
            } else contacts
            FarmSettings(
                id = FarmSettings.DEFAULT_FARM_ID,
                name = prefs[KEY_FARM_NAME] ?: "",
                address = prefs[KEY_FARM_ADDRESS] ?: "",
                contacts = contactsOrLegacy,
                calvingAlertDays = prefs[KEY_CALVING_ALERT_DAYS] ?: FarmSettings.DEFAULT_CALVING_ALERT_DAYS,
                pregnancyCheckDaysAfterBreeding = prefs[KEY_PREGNANCY_CHECK_DAYS]
                    ?: FarmSettings.DEFAULT_PREGNANCY_CHECK_DAYS,
                gestationDays = prefs[KEY_GESTATION_DAYS] ?: FarmSettings.DEFAULT_GESTATION_DAYS,
                weaningAgeDays = prefs[KEY_WEANING_AGE_DAYS] ?: FarmSettings.DEFAULT_WEANING_AGE_DAYS
            )
        }

    override suspend fun updateFarmSettings(settings: FarmSettings) {
        context.farmSettingsDataStore.edit { prefs ->
            prefs[KEY_FARM_NAME] = settings.name
            prefs[KEY_FARM_ADDRESS] = settings.address
            prefs[KEY_FARM_CONTACTS] = contactsToJson(settings.contacts)
            prefs[KEY_CALVING_ALERT_DAYS] = settings.calvingAlertDaysClamped()
            prefs[KEY_PREGNANCY_CHECK_DAYS] = settings.pregnancyCheckDaysClamped()
            prefs[KEY_GESTATION_DAYS] = settings.gestationDaysClamped()
            prefs[KEY_WEANING_AGE_DAYS] = settings.weaningAgeDaysClamped()
        }
    }

    private fun contactsToJson(contacts: List<FarmContact>): String {
        if (contacts.isEmpty()) return "[]"
        val sb = StringBuilder().append('[')
        contacts.forEachIndexed { i, c ->
            if (i > 0) sb.append(',')
            sb.append("{\"name\":").append(escape(c.name))
                .append(",\"phone\":").append(escape(c.phone))
                .append(",\"email\":").append(escape(c.email))
                .append('}')
        }
        return sb.append(']').toString()
    }

    private fun escape(s: String): String {
        val out = StringBuilder("\"")
        for (ch in s) {
            when (ch) {
                '\\' -> out.append("\\\\")
                '"' -> out.append("\\\"")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                else -> out.append(ch)
            }
        }
        return out.append('"').toString()
    }

    private fun parseContactsJson(json: String): List<FarmContact> {
        return try {
            org.json.JSONArray(json).let { arr ->
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    FarmContact(
                        name = o.optString("name", ""),
                        phone = o.optString("phone", ""),
                        email = o.optString("email", "")
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
