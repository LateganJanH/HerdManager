package com.herdmanager.app.domain.model

/** A single contact for the farm (name, phone, email). */
data class FarmContact(
    val name: String = "",
    val phone: String = "",
    val email: String = ""
) {
    val hasAny: Boolean get() = name.isNotBlank() || phone.isNotBlank() || email.isNotBlank()
}

data class FarmSettings(
    val id: String = DEFAULT_FARM_ID,
    val name: String = "",
    val address: String = "",
    /** Multiple contacts; replaces legacy contactPhone/contactEmail. */
    val contacts: List<FarmContact> = emptyList(),
    val calvingAlertDays: Int = DEFAULT_CALVING_ALERT_DAYS,
    val pregnancyCheckDaysAfterBreeding: Int = DEFAULT_PREGNANCY_CHECK_DAYS,
    val gestationDays: Int = DEFAULT_GESTATION_DAYS
) {
    companion object {
        const val DEFAULT_FARM_ID = "default-farm-mvp"
        const val DEFAULT_CALVING_ALERT_DAYS = 14
        const val CALVING_ALERT_DAYS_MIN = 1
        const val CALVING_ALERT_DAYS_MAX = 60
        const val DEFAULT_PREGNANCY_CHECK_DAYS = 45
        const val PREGNANCY_CHECK_DAYS_MIN = 28
        const val PREGNANCY_CHECK_DAYS_MAX = 90
        const val DEFAULT_GESTATION_DAYS = 283
        const val GESTATION_DAYS_MIN = 250
        const val GESTATION_DAYS_MAX = 320
    }
    val displayName: String get() = name.ifBlank { "My Farm" }

    fun calvingAlertDaysClamped(): Int =
        calvingAlertDays.coerceIn(CALVING_ALERT_DAYS_MIN, CALVING_ALERT_DAYS_MAX)

    fun pregnancyCheckDaysClamped(): Int =
        pregnancyCheckDaysAfterBreeding.coerceIn(PREGNANCY_CHECK_DAYS_MIN, PREGNANCY_CHECK_DAYS_MAX)

    fun gestationDaysClamped(): Int =
        gestationDays.coerceIn(GESTATION_DAYS_MIN, GESTATION_DAYS_MAX)
}
