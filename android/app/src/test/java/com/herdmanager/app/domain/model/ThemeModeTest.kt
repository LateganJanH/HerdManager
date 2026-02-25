package com.herdmanager.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun themeMode_entries_hasThreeValues() {
        assertEquals(3, ThemeMode.entries.size)
    }

    @Test
    fun themeMode_labels_areNonBlank() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(true, mode.label.isNotBlank())
        }
    }

    @Test
    fun themeMode_system_hasCorrectLabel() {
        assertEquals("System", ThemeMode.SYSTEM.label)
    }

    @Test
    fun themeMode_light_hasCorrectLabel() {
        assertEquals("Light", ThemeMode.LIGHT.label)
    }

    @Test
    fun themeMode_dark_hasCorrectLabel() {
        assertEquals("Dark", ThemeMode.DARK.label)
    }
}
