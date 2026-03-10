package com.herdmanager.app.ui.screens

import com.herdmanager.app.domain.model.WeightRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class AnimalDetailViewModelTest {

    @Test
    fun computeGrowthSummary_increasingWeights_hasPositiveGain() {
        val day1 = LocalDate.of(2026, 3, 1)
        val day2 = LocalDate.of(2026, 3, 31)
        val records = listOf(
            WeightRecord(id = "w1", animalId = "a1", date = day1, weightKg = 350.0),
            WeightRecord(id = "w2", animalId = "a1", date = day2, weightKg = 380.0)
        )

        val summary = computeGrowthSummary(records)

        requireNotNull(summary)
        assertEquals(380.0, summary.latestWeightKg, 0.0)
        assertEquals(30L, summary.daysBetween)
        assertEquals(30.0, summary.gainKg, 0.0)
        assertEquals(1.0, summary.gainPerDayKg, 1e-6)
    }

    @Test
    fun computeGrowthSummary_equalWeights_zeroGain() {
        val day1 = LocalDate.of(2026, 3, 1)
        val day2 = LocalDate.of(2026, 3, 31)
        val records = listOf(
            WeightRecord(id = "w1", animalId = "a1", date = day1, weightKg = 400.0),
            WeightRecord(id = "w2", animalId = "a1", date = day2, weightKg = 400.0)
        )

        val summary = computeGrowthSummary(records)

        requireNotNull(summary)
        assertEquals(0.0, summary.gainKg, 0.0)
        assertEquals(0.0, summary.gainPerDayKg, 1e-6)
    }

    @Test
    fun computeGrowthSummary_sameDate_coercesDaysToOne() {
        val day = LocalDate.of(2026, 3, 1)
        val records = listOf(
            WeightRecord(id = "w1", animalId = "a1", date = day, weightKg = 390.0),
            WeightRecord(id = "w2", animalId = "a1", date = day, weightKg = 395.0)
        )

        val summary = computeGrowthSummary(records)

        requireNotNull(summary)
        assertEquals(1L, summary.daysBetween)
        assertEquals(5.0, summary.gainKg, 0.0)
        assertEquals(5.0, summary.gainPerDayKg, 1e-6)
    }

    @Test
    fun computeGrowthSummary_insufficientRecords_returnsNull() {
        val records = listOf(
            WeightRecord(id = "w1", animalId = "a1", date = LocalDate.of(2026, 3, 1), weightKg = 350.0)
        )

        val summary = computeGrowthSummary(records)

        assertNull(summary)
    }
}

