package com.herdmanager.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Central place for shared UI shapes, paddings and text styles.
 * Use these from screens/components instead of hard-coding values.
 */
object UiDefaults {

    // Cards (list items, alerts, summaries)
    val CardShape: Shape = RoundedCornerShape(12.dp)
    val CardInnerPadding: Dp = 18.dp

    // Small meta chips / banners
    val PillShape: Shape = RoundedCornerShape(16.dp)

    // Section headers (e.g. Alerts sections, small strip labels)
    val SectionHeaderPaddingVertical: Dp = 4.dp
    val SectionHeaderIconSize: Dp = 18.dp
}

/**
 * Helpers for typography so callers don't depend on concrete styles.
 */
object UiTextStyles {
    val sectionHeader
        @Composable get() = MaterialTheme.typography.titleSmall

    val meta
        @Composable get() = MaterialTheme.typography.bodySmall
}

