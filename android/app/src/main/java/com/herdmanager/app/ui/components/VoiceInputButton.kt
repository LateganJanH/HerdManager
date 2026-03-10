package com.herdmanager.app.ui.components

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * A microphone button that launches the system speech recognizer and passes
 * the recognized text to [onResult]. Requests RECORD_AUDIO at runtime if needed.
 * Use as trailing icon in text fields (e.g. ear tag, breed) for voice input.
 */
@Composable
fun VoiceInputButton(
    onResult: (String) -> Unit,
    onError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pendingLaunch by remember { mutableStateOf(false) }
    var hasRecordAudio by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val text = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
            if (!text.isNullOrEmpty()) {
                onResult(text)
            } else {
                onError()
            }
        } else {
            onError()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasRecordAudio = granted
        if (granted && pendingLaunch) {
            pendingLaunch = false
            launchSpeech(context, speechLauncher::launch, onResult, onError)
        } else {
            pendingLaunch = false
            if (!granted) onError()
        }
    }

    fun tryLaunch() {
        if (hasRecordAudio) {
            launchSpeech(context, speechLauncher::launch, onResult, onError)
        } else {
            pendingLaunch = true
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    IconButton(
        onClick = { tryLaunch() },
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Voice input"
        )
    }
}

private fun launchSpeech(
    context: android.content.Context,
    launch: (Intent) -> Unit,
    onResult: (String) -> Unit,
    onError: () -> Unit
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak ear tag or name")
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }
    try {
        launch(intent)
    } catch (_: Exception) {
        onError()
    }
}
