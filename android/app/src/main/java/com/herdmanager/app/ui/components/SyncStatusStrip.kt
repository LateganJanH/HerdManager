package com.herdmanager.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.Instant

/**
 * Compact sync status strip for persistent "Synced" / "Syncing…" / "Saved offline" indicator.
 * Shown at the top of Home, Herd list, Analytics, and Settings so the user always sees sync state.
 */
@Composable
fun SyncStatusStrip(
    lastSyncedAt: Instant?,
    isSyncing: Boolean,
    syncError: String?,
    formatLastSynced: (Instant?) -> String,
    onSync: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (statusLabel, icon, contentDesc) = when {
        syncError != null -> Triple(
            "Sync failed",
            Icons.Default.CloudOff,
            "Sync failed. $syncError. Tap to try again or dismiss."
        )
        isSyncing -> Triple(
            "Syncing…",
            Icons.Default.Sync,
            "Syncing"
        )
        lastSyncedAt != null -> Triple(
            "Synced · ${formatLastSynced(lastSyncedAt)}",
            Icons.Default.CloudDone,
            "Synced ${formatLastSynced(lastSyncedAt)}. Tap to sync now."
        )
        else -> Triple(
            "Saved offline",
            Icons.Default.Cloud,
            "Saved offline. Tap to sync."
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .then(
                if (syncError != null) Modifier
                else Modifier.clickable(enabled = !isSyncing, onClick = onSync)
            )
            .semantics { contentDescription = contentDesc },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = when {
                    syncError != null -> MaterialTheme.colorScheme.error
                    isSyncing -> MaterialTheme.colorScheme.primary
                    lastSyncedAt != null -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    syncError != null -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        if (syncError != null) {
            Text(
                text = "Dismiss",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onDismissError)
            )
        }
    }
}
