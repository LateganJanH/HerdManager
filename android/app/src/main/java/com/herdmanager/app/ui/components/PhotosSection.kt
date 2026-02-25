package com.herdmanager.app.ui.components

import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.herdmanager.app.domain.model.Photo
import com.herdmanager.app.domain.model.PhotoAngle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

private fun getLastLocation(context: Context): Pair<Double, Double>? {
    return try {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val location = manager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: manager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (location != null) Pair(location.latitude, location.longitude) else null
    } catch (_: Exception) { null }
}

@Composable
fun PhotosSection(
    photos: List<Photo>,
    animalId: String,
    avatarPhotoId: String? = null,
    onPhotoAdded: (String, PhotoAngle, Double?, Double?) -> Unit,
    onPhotoDeleted: (Photo) -> Unit,
    onSetAvatar: (Photo) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingAngle by remember { mutableStateOf<PhotoAngle?>(null) }
    var pendingFile by remember { mutableStateOf<File?>(null) }
    var showAddPhotoOptions by remember { mutableStateOf(false) }
    var showAnglePicker by remember { mutableStateOf(false) }
    var anglePickerForGallery by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<Photo?>(null) }
    /** After location permission result: "camera" -> request camera then take picture; "gallery" -> pick image. */
    var pendingStepAfterLocation by remember { mutableStateOf<String?>(null) }

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    val photosDir = remember(animalId) {
        File(context.filesDir, "photos/$animalId").also { it.mkdirs() }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val angle = pendingAngle
        val file = pendingFile
        if (success && angle != null && file != null && file.exists()) {
            scope.launch(Dispatchers.IO) {
                val loc = getLastLocation(context)
                withContext(Dispatchers.Main) {
                    onPhotoAdded(file.absolutePath, angle, loc?.first, loc?.second)
                }
            }
        }
        pendingAngle = null
        pendingFile = null
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingAngle != null) {
            val file = File(photosDir, "img_${System.currentTimeMillis()}.jpg")
            pendingFile = file
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            takePictureLauncher.launch(uri)
        } else {
            pendingAngle = null
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val angle = pendingAngle
        if (uri != null && angle != null) {
            val file = File(photosDir, "img_${System.currentTimeMillis()}.jpg")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                if (file.exists()) {
                    scope.launch(Dispatchers.IO) {
                        val loc = getLastLocation(context)
                        withContext(Dispatchers.Main) {
                            onPhotoAdded(file.absolutePath, angle, loc?.first, loc?.second)
                        }
                    }
                }
            } catch (_: Exception) { }
        }
        pendingAngle = null
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        when (pendingStepAfterLocation) {
            "camera" -> permissionLauncher.launch(android.Manifest.permission.CAMERA)
            "gallery" -> pickImageLauncher.launch("image/*")
            else -> { }
        }
        pendingStepAfterLocation = null
    }

    val launchCamera: (PhotoAngle) -> Unit = { angle ->
        pendingAngle = angle
        if (hasLocationPermission()) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        } else {
            pendingStepAfterLocation = "camera"
            locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val launchGallery: (PhotoAngle) -> Unit = { angle ->
        pendingAngle = angle
        if (hasLocationPermission()) {
            pickImageLauncher.launch("image/*")
        } else {
            pendingStepAfterLocation = "gallery"
            locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    if (showAddPhotoOptions) {
        AlertDialog(
            onDismissRequest = { showAddPhotoOptions = false },
            title = { Text("Add photo from") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = false,
                        onClick = {
                            showAddPhotoOptions = false
                            anglePickerForGallery = false
                            showAnglePicker = true
                        },
                        label = { Text("Camera") },
                        leadingIcon = {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Camera", Modifier.size(18.dp))
                        }
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            showAddPhotoOptions = false
                            anglePickerForGallery = true
                            showAnglePicker = true
                        },
                        label = { Text("Gallery") },
                        leadingIcon = {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", Modifier.size(18.dp))
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddPhotoOptions = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAnglePicker) {
        AlertDialog(
            onDismissRequest = { showAnglePicker = false },
            title = { Text("Photo angle") },
            text = {
                Column {
                    PhotoAngle.entries.forEach { angle ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                showAnglePicker = false
                                if (anglePickerForGallery) {
                                    launchGallery(angle)
                                } else {
                                    launchCamera(angle)
                                }
                            },
                            label = { Text(angle.name.replace('_', ' ')) },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAnglePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    showDeleteConfirm?.let { photo ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete photo?") },
            text = { Text("This photo will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    onPhotoDeleted(photo)
                    showDeleteConfirm = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Photos", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { showAddPhotoOptions = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add photo", modifier = Modifier.size(18.dp))
                Text(" Add photo", modifier = Modifier.padding(start = 4.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (photos.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
                onClick = { showAddPhotoOptions = true }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add photo", modifier = Modifier.size(48.dp))
                    Text("Tap to add photo", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(photos, key = { it.id }) { photo ->
                    Column(
                        modifier = Modifier.width(120.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.size(120.dp)) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { }
                            ) {
                                val photoUri = if (photo.uri.startsWith("http")) android.net.Uri.parse(photo.uri) else android.net.Uri.parse("file://${photo.uri}")
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = photo.angle.name,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            IconButton(
                                onClick = { showDeleteConfirm = photo },
                                modifier = Modifier.align(Alignment.TopEnd).size(32.dp)
                            ) {
                                Text("×", style = MaterialTheme.typography.titleMedium)
                            }
                            if (photo.id == avatarPhotoId) {
                                androidx.compose.material3.Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        "Profile",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        if (photo.id != avatarPhotoId) {
                            TextButton(
                                onClick = { onSetAvatar(photo) },
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text("Set as avatar", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                item {
                    Card(
                        modifier = Modifier
                            .height(120.dp)
                            .aspectRatio(1f),
                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
                        onClick = { showAddPhotoOptions = true }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add photo")
                        }
                    }
                }
            }
        }
    }
}
