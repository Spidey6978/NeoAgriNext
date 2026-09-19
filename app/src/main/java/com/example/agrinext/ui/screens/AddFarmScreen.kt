package com.example.agrinext.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFarmScreen(
    onBack: () -> Unit,
    onFarmSaved: (String, List<Pair<Float, Float>>, Double, Double) -> Unit // Updated Callback
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val farmBoundaryPoints = remember { mutableStateListOf<LatLng>() }
    var showSaveDialog by remember { mutableStateOf(false) }
    var farmNameInput by remember { mutableStateOf("") }

    val defaultLocation = LatLng(36.9741, -122.0308)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasLocationPermission = isGranted
        if (isGranted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val userLatLng = LatLng(it.latitude, it.longitude)
                    scope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                    }
                }
            }
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    val searchSuggestions = listOf("Central Valley, CA", "Salinas Valley, CA", "Napa Valley, CA").filter {
        it.contains(searchQuery, ignoreCase = true) && searchQuery.isNotEmpty()
    }

    fun performSearch(query: String) {
        if (query.isBlank()) return
        focusManager.clearFocus()
        scope.launch {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(query, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val latLng = LatLng(address.latitude, address.longitude)
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                } else {
                    Toast.makeText(context, "Location not found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                Toast.makeText(context, "Search failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun normalizePoints(points: List<LatLng>): List<Pair<Float, Float>> {
        if (points.isEmpty()) return emptyList()
        val minLat = points.minOf { it.latitude }
        val maxLat = points.maxOf { it.latitude }
        val minLng = points.minOf { it.longitude }
        val maxLng = points.maxOf { it.longitude }
        val width = maxLng - minLng
        val height = maxLat - minLat
        return points.map { point ->
            val x = ((point.longitude - minLng) / width).toFloat()
            val y = 1f - ((point.latitude - minLat) / height).toFloat()
            x to y
        }
    }

    // Function to calculate Centroid of Polygon
    fun calculateCentroid(points: List<LatLng>): LatLng {
        if (points.isEmpty()) return LatLng(0.0, 0.0)

        var area = 0.0
        var cx = 0.0
        var cy = 0.0

        for (i in points.indices) {
            val p1 = points[i]
            val p2 = points[(i + 1) % points.size]

            // Calculate cross product for area and centroid moments
            val cross = (p1.latitude * p2.longitude) - (p2.latitude * p1.longitude)
            area += cross
            cx += (p1.latitude + p2.latitude) * cross
            cy += (p1.longitude + p2.longitude) * cross
        }

        area *= 0.5

        // Handle collinear points or zero area by falling back to average
        if (abs(area) < 1e-9) {
            val avgLat = points.map { it.latitude }.average()
            val avgLng = points.map { it.longitude }.average()
            return LatLng(avgLat, avgLng)
        }

        cx /= (6 * area)
        cy /= (6 * area)

        return LatLng(cx, cy)
    }

    Scaffold(contentWindowInsets = WindowInsets.systemBars) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text("Mark Farm Boundary", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.Center))
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                placeholder = { Text("Search location") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, contentDescription = "Clear") } },
                singleLine = true,
                shape = CircleShape,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { performSearch(searchQuery) }),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = Color.LightGray,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (searchSuggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().zIndex(2f).padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(searchSuggestions) { suggestion ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { searchQuery = suggestion; performSearch(suggestion) }.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(suggestion, style = MaterialTheme.typography.bodyMedium)
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxSize().zIndex(1f).clip(RoundedCornerShape(24.dp)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            properties = MapProperties(mapType = MapType.HYBRID, isMyLocationEnabled = hasLocationPermission),
                            uiSettings = MapUiSettings(zoomControlsEnabled = false, compassEnabled = true, myLocationButtonEnabled = false)
                        ) {
                            Marker(state = MarkerState(position = cameraPositionState.position.target), title = "Target", snippet = "Press 'Add Point'")
                            if (farmBoundaryPoints.isNotEmpty()) {
                                Polygon(
                                    points = farmBoundaryPoints,
                                    fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    strokeColor = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 5f
                                )
                                farmBoundaryPoints.forEachIndexed { index, point ->
                                    Marker(state = MarkerState(position = point), title = "Corner ${index + 1}", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                                }
                            }
                        }
                        SmallFloatingActionButton(
                            onClick = { if (hasLocationPermission) fusedLocationClient.lastLocation.addOnSuccessListener { loc -> loc?.let { scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 15f)) } } } else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) { Icon(Icons.Default.MyLocation, "My Location") }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { farmBoundaryPoints.add(cameraPositionState.position.target) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) { Text("Add Point") }

                if (farmBoundaryPoints.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = { if (farmBoundaryPoints.isNotEmpty()) farmBoundaryPoints.removeLast() },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error)
                    ) { Icon(Icons.Default.Undo, "Undo") }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { showSaveDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = farmBoundaryPoints.size >= 3
            ) {
                Text(if (farmBoundaryPoints.size < 3) "Add at least 3 corners" else "Save Farm Boundary (${farmBoundaryPoints.size} pts)")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("Name Your Farm") },
                text = {
                    Column {
                        Text("Give this farm plot a name to save it.")
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = farmNameInput,
                            onValueChange = { farmNameInput = it },
                            label = { Text("Farm Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (farmNameInput.isNotBlank()) {
                                val normalizedPoints = normalizePoints(farmBoundaryPoints)
                                val centroid = calculateCentroid(farmBoundaryPoints)
                                onFarmSaved(farmNameInput, normalizedPoints, centroid.latitude, centroid.longitude)
                                showSaveDialog = false
                            }
                        }
                    ) { Text("Save Farm") }
                },
                dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") } },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}