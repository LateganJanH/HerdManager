package com.herdmanager.app.ui.util

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File

/**
 * Runs ML Kit text recognition on an image file. Calls [onResult] with the detected
 * text (or null if recognition fails or no text). Safe to call from a background thread;
 * invoke [onResult] on the main thread if updating UI.
 */
fun runTextRecognition(context: Context, filePath: String, onResult: (String?) -> Unit) {
    val file = File(filePath)
    if (!file.exists()) {
        onResult(null)
        return
    }
    val uri = Uri.fromFile(file)
    val image = try {
        InputImage.fromFilePath(context, uri)
    } catch (_: Exception) {
        onResult(null)
        return
    }
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())
    recognizer.process(image)
        .addOnSuccessListener { text ->
            recognizer.close()
            val s = text.text.trim().takeIf { it.isNotEmpty() }
            onResult(s)
        }
        .addOnFailureListener {
            recognizer.close()
            onResult(null)
        }
}
