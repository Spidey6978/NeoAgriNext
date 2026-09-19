package com.example.agrinext.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.agrinext.data.FarmRepository
import com.example.agrinext.util.LanguageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFarmScreen(
    farmId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { FarmRepository(context) }

    var farmName by remember { mutableStateOf("") }
    var farmImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            farmImageUri = uri
            // Persist permission to read this URI later
            try {
                val flag = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flag)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Load the existing farm data
    LaunchedEffect(farmId) {
        val farm = repository.getFarms().find { it.id == farmId }
        farmName = farm?.name ?: ""
        farm?.imageUri?.let { farmImageUri = Uri.parse(it) }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LanguageManager.get("Edit Farm")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isLoading) {
                // --- Image Upload Section ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = rememberBitmapFromUri(farmImageUri)

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Farm Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Remove Image Button
                        IconButton(
                            onClick = { farmImageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove Image",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Add Image",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = LanguageManager.get("Tap to add farm photo"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Name Section ---
                Text(
                    text = LanguageManager.get("Rename Your Farm"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = farmName,
                    onValueChange = { farmName = it },
                    label = { Text(LanguageManager.get("Farm Name")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // --- Save Button ---
                Button(
                    onClick = {
                        if (farmName.isNotBlank()) {
                            val allFarms = repository.getFarms().toMutableList()
                            val index = allFarms.indexOfFirst { it.id == farmId }

                            if (index != -1) {
                                val updatedFarm = allFarms[index].copy(
                                    name = farmName,
                                    imageUri = farmImageUri?.toString()
                                )
                                allFarms[index] = updatedFarm
                                repository.saveFarms(allFarms)
                                onBack()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(LanguageManager.get("Save Changes"))
                }
            } else {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun rememberBitmapFromUri(uri: Uri?): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(uri) {
        if (uri != null) {
            withContext(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val androidBitmap = BitmapFactory.decodeStream(inputStream)
                    bitmap = androidBitmap?.asImageBitmap()
                } catch (e: Exception) {
                    e.printStackTrace()
                    bitmap = null
                }
            }
        } else {
            bitmap = null
        }
    }
    return bitmap
}