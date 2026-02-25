package com.herdmanager.app.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herdmanager.app.domain.model.PhotoAngle
import com.herdmanager.app.domain.model.FarmSettings
import com.herdmanager.app.domain.repository.AnimalRepository
import com.herdmanager.app.domain.repository.BreedingEventRepository
import com.herdmanager.app.domain.repository.CalvingEventRepository
import com.herdmanager.app.domain.repository.FarmSettingsRepository
import com.herdmanager.app.domain.repository.HealthEventRepository
import com.herdmanager.app.domain.repository.HerdRepository
import com.herdmanager.app.domain.repository.PhotoRepository
import com.herdmanager.app.domain.repository.SyncRepository
import com.herdmanager.app.ui.util.PdfPageInfoFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.FormatStyle
import java.util.Locale
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class CalvingAlert(
    val damEarTag: String,
    val dueDate: LocalDate,
    val daysUntilDue: Long
)

data class PregnancyCheckAlert(
    val damEarTag: String,
    val checkDueDate: LocalDate,
    val daysUntilDue: Long
)

data class WithdrawalAlert(
    val earTag: String,
    val endDate: LocalDate,
    val daysUntilDue: Long,
    val animalId: String
)

private const val PREGNANCY_CHECK_ALERT_WINDOW_DAYS = 7L
private const val WITHDRAWAL_ALERT_WINDOW_DAYS = 14L

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HerdListViewModel @Inject constructor(
    private val animalRepository: AnimalRepository,
    private val herdRepository: HerdRepository,
    private val breedingEventRepository: BreedingEventRepository,
    private val calvingEventRepository: CalvingEventRepository,
    private val farmSettingsRepository: FarmSettingsRepository,
    private val healthEventRepository: HealthEventRepository,
    private val photoRepository: PhotoRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0)

    val lastSyncedAt = syncRepository.lastSyncedAt().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError = _syncError.asStateFlow()

    fun syncNow() {
        viewModelScope.launch {
            _syncError.value = null
            _isSyncing.value = true
            syncRepository.syncNow()
                .onSuccess {
                    _isSyncing.value = false
                    refreshTrigger.value++
                }
                .onFailure { e ->
                    _isSyncing.value = false
                    _syncError.value = e.message ?: "Sync failed"
                }
        }
    }

    fun clearSyncError() {
        _syncError.value = null
    }

    fun formatLastSynced(instant: Instant?): String =
        if (instant == null) "Never"
        else java.time.format.DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())
            .withLocale(Locale.getDefault())
            .format(instant)
    val selectedHerdId = MutableStateFlow<String?>(null)

    val farmSettings = farmSettingsRepository.farmSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = com.herdmanager.app.domain.model.FarmSettings()
        )

    val farmDisplayName = farmSettingsRepository.farmSettings()
        .map { it.displayName }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "Herd"
        )

    val herds = herdRepository.observeHerdsByFarm(FarmSettings.DEFAULT_FARM_ID)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun setSelectedHerd(herdId: String?) {
        selectedHerdId.value = herdId
    }

    private val _isListLoading = MutableStateFlow(true)
    val isListLoading = _isListLoading.asStateFlow()

    val animals = combine(
        refreshTrigger,
        selectedHerdId.flatMapLatest { herdId ->
            animalRepository.observeAnimalsByFarmAndHerd(FarmSettings.DEFAULT_FARM_ID, herdId)
        }
    ) { _, list -> list }
        .onEach { _isListLoading.value = false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val displayPhotoUriByAnimalId = combine(
        refreshTrigger,
        animalRepository.observeAnimalsByFarm(FarmSettings.DEFAULT_FARM_ID),
        photoRepository.observeAllPhotos()
    ) { _, animalsList, allPhotos ->
        animalsList.associate { animal ->
            val animalPhotos = allPhotos.filter { it.animalId == animal.id }
            val displayPhoto = animal.avatarPhotoId?.let { id ->
                animalPhotos.find { it.id == id }
            } ?: animalPhotos.find { it.angle == PhotoAngle.FACE }
                ?: animalPhotos.firstOrNull()
            animal.id to (displayPhoto?.uri)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyMap()
    )

    val pregnancyCheckAlerts = combine(
        farmSettingsRepository.farmSettings(),
        breedingEventRepository.observeAllBreedingEvents(),
        animalRepository.observeAnimalsByFarm(FarmSettings.DEFAULT_FARM_ID),
        calvingEventRepository.observeCalvedBreedingEventIds()
    ) { settings, events, animals, calvedIds ->
        val checkDays = settings.pregnancyCheckDaysClamped().toLong()
        val calvedSet = calvedIds.toSet()
        val animalMap = animals.associateBy { it.id }
        events
            .filter { it.id !in calvedSet }
            .filter { !it.hasPregnancyCheck }
            .map { event ->
                val checkDue = event.serviceDate.plusDays(checkDays)
                event to checkDue
            }
            .filter { (_, checkDue) ->
                checkDue in LocalDate.now()..LocalDate.now().plusDays(PREGNANCY_CHECK_ALERT_WINDOW_DAYS)
            }
            .sortedBy { (_, checkDue) -> checkDue }
            .map { (event, checkDue) ->
                val dam = animalMap[event.animalId]
                PregnancyCheckAlert(
                    damEarTag = dam?.earTagNumber ?: "Unknown",
                    checkDueDate = checkDue,
                    daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), checkDue)
                )
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val upcomingCalvingAlerts = combine(
        farmSettingsRepository.farmSettings(),
        breedingEventRepository.observeAllBreedingEvents(),
        animalRepository.observeAnimalsByFarm(FarmSettings.DEFAULT_FARM_ID),
        calvingEventRepository.observeCalvedBreedingEventIds()
    ) { settings, events, animals, calvedIds ->
        val alertDays = settings.calvingAlertDaysClamped().toLong()
        val calvedSet = calvedIds.toSet()
        val animalMap = animals.associateBy { it.id }
        val gestation = settings.gestationDaysClamped()
        events
            .filter { it.id !in calvedSet }
            .filter { run { val d = it.dueDate(gestation); d in LocalDate.now()..LocalDate.now().plusDays(alertDays) } }
            .sortedBy { it.dueDate(gestation) }
            .map { event ->
                val dam = animalMap[event.animalId]
                val due = event.dueDate(gestation)
                CalvingAlert(
                    damEarTag = dam?.earTagNumber ?: "Unknown",
                    dueDate = due,
                    daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), due)
                )
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val withdrawalAlerts = combine(
        refreshTrigger,
        healthEventRepository.observeAllHealthEvents(),
        animalRepository.observeAnimalsByFarm(FarmSettings.DEFAULT_FARM_ID)
    ) { _, healthEvents, animals ->
        val now = LocalDate.now()
        val endRange = now.plusDays(WITHDRAWAL_ALERT_WINDOW_DAYS)
        val animalMap = animals.associateBy { it.id }
        healthEvents
            .filter { it.withdrawalPeriodEnd != null && it.withdrawalPeriodEnd!! in now..endRange }
            .sortedBy { it.withdrawalPeriodEnd }
            .map { event ->
                val end = event.withdrawalPeriodEnd!!
                WithdrawalAlert(
                    earTag = animalMap[event.animalId]?.earTagNumber ?: "Unknown",
                    endDate = end,
                    daysUntilDue = ChronoUnit.DAYS.between(now, end),
                    animalId = event.animalId
                )
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun exportAnimalsCsv(animals: List<com.herdmanager.app.domain.model.Animal>): String {
        val header = "Ear Tag,Name,Sex,Breed,Date of Birth,Status,Coat Color"
        val rows = animals.map { a ->
            listOf(
                a.earTagNumber,
                a.name ?: "",
                a.sex.name,
                a.breed,
                a.dateOfBirth.toString(),
                a.status.name,
                a.coatColor ?: ""
            ).joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    /**
     * Generates a PDF of the herd list (same columns as CSV). Call from a background thread.
     */
    fun exportAnimalsPdf(animals: List<com.herdmanager.app.domain.model.Animal>): ByteArray {
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40
        val lineHeight = 14
        val titlePaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
            typeface = Typeface.DEFAULT
        }
        val headerPaint = Paint().apply {
            textSize = 10f
            isFakeBoldText = true
            typeface = Typeface.DEFAULT
        }
        val cellPaint = Paint().apply {
            textSize = 10f
            typeface = Typeface.DEFAULT
        }
        val col0 = margin.toFloat()
        val col1 = 120f
        val col2 = 200f
        val col3 = 240f
        val col4 = 320f
        val col5 = 410f
        val col6 = 470f

        var y = margin + 20f
        var pageNum: Int = 1
        var pageInfo = PdfPageInfoFactory.create(pageWidth, pageHeight, pageNum)
        var page = doc.startPage(pageInfo)
        var canvas = page.canvas

        fun newPage() {
            doc.finishPage(page)
            pageNum += 1
            pageInfo = PdfPageInfoFactory.create(pageWidth, pageHeight, pageNum)
            page = doc.startPage(pageInfo)
            canvas = page.canvas
            y = margin.toFloat()
        }

        val title = "Herd export – ${java.time.LocalDate.now()}"
        canvas.drawText(title, col0, y, titlePaint)
        y += lineHeight + 8

        canvas.drawText("Ear Tag", col0, y, headerPaint)
        canvas.drawText("Name", col1, y, headerPaint)
        canvas.drawText("Sex", col2, y, headerPaint)
        canvas.drawText("Breed", col3, y, headerPaint)
        canvas.drawText("DOB", col4, y, headerPaint)
        canvas.drawText("Status", col5, y, headerPaint)
        canvas.drawText("Coat", col6, y, headerPaint)
        y += lineHeight

        for (a in animals) {
            if (y > pageHeight - margin - lineHeight) newPage()
            canvas.drawText(a.earTagNumber.take(12), col0, y, cellPaint)
            canvas.drawText((a.name ?: "").take(12), col1, y, cellPaint)
            canvas.drawText(a.sex.name.take(4), col2, y, cellPaint)
            canvas.drawText(a.breed.take(12), col3, y, cellPaint)
            canvas.drawText(a.dateOfBirth.toString(), col4, y, cellPaint)
            canvas.drawText(a.status.name.take(8), col5, y, cellPaint)
            canvas.drawText((a.coatColor ?: "").take(10), col6, y, cellPaint)
            y += lineHeight
        }

        doc.finishPage(page)
        val out = java.io.ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    fun refresh() {
        refreshTrigger.value++
    }

    fun deleteAnimal(animalId: String) {
        viewModelScope.launch {
            animalRepository.deleteAnimal(animalId)
            refreshTrigger.value++
        }
    }
}
