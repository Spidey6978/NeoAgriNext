package com.example.agrinext.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.agrinext.Config
import com.example.agrinext.data.AgriNextApi
import com.example.agrinext.data.CropInput
import com.example.agrinext.data.CropRoadmap
import com.example.agrinext.data.FarmRepository
import com.example.agrinext.data.FarmScheduleRepository
import com.example.agrinext.data.FarmTask
import com.example.agrinext.data.MarketInfo
import com.example.agrinext.data.RecommendedCrop
import com.example.agrinext.data.SatelliteData
import com.example.agrinext.ui.components.FarmLand
import com.example.agrinext.ui.components.FarmScheduleComponent
import com.example.agrinext.util.LanguageManager
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndividualFarmScreen(
    farmId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { FarmRepository(context) }
    var farm by remember { mutableStateOf<FarmLand?>(null) }

    // --- UI STATES ---
    var statusText by remember { mutableStateOf("Select a crop below to analyze...") }
    var marketInfo by remember { mutableStateOf<MarketInfo?>(null) }
    var roadmapData by remember { mutableStateOf<CropRoadmap?>(null) }
    var satelliteData by remember { mutableStateOf<SatelliteData?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Recommendation States
    var recommendedCrops by remember { mutableStateOf<List<RecommendedCrop>>(emptyList()) }
    var selectedCrop by remember { mutableStateOf<RecommendedCrop?>(null) }
    var locationName by remember { mutableStateOf("Detecting Location...") }
    var isRecommending by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // --- FARMING STATUS ---
    var isFarmingActive by remember { mutableStateOf(false) }

    // --- SCHEDULE STATE ---
    // Initialize tasks based on either now or stored start date
    var farmTasks by remember { mutableStateOf(listOf<FarmTask>()) }

    val scope = rememberCoroutineScope()
    val pageScrollState = rememberScrollState()

    // Setup Retrofit
    val retrofit = remember {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(Config.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AgriNextApi::class.java)
    }

    // Function to Fetch Recommendations
    fun fetchRecommendations(currentFarm: FarmLand) {
        if (!currentFarm.savedRecommendedCrops.isNullOrEmpty()) {
            recommendedCrops = currentFarm.savedRecommendedCrops!!
            locationName = currentFarm.savedLocationName ?: "Saved Location"
            if (recommendedCrops.isNotEmpty() && selectedCrop == null) {
                selectedCrop = recommendedCrops[0]
            }
            isRecommending = false
            return
        }

        scope.launch {
            isRecommending = true
            errorMessage = null
            try {
                val response = retrofit.getRecommendedCrops(currentFarm.centroidLat, currentFarm.centroidLng)
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    recommendedCrops = data.crops ?: emptyList()
                    locationName = data.location ?: "Unknown Location"

                    if (recommendedCrops.isNotEmpty()) {
                        selectedCrop = recommendedCrops[0]

                        val updatedFarm = currentFarm.copy(
                            savedRecommendedCrops = recommendedCrops,
                            savedLocationName = locationName
                        )
                        farm = updatedFarm

                        val allFarms = repository.getFarms()
                        val updatedList = allFarms.map { if (it.id == updatedFarm.id) updatedFarm else it }
                        repository.saveFarms(updatedList)

                    } else {
                        errorMessage = "No suitable crops found for this area."
                    }
                } else {
                    locationName = "Location Not Found"
                    errorMessage = "Server Error: ${response.code()}"
                }
            } catch (e: Exception) {
                locationName = "Connection Error"
                errorMessage = "Net Error: ${e.message}"
            } finally {
                isRecommending = false
            }
        }
    }

    // 1. Fetch Farm Data
    LaunchedEffect(farmId) {
        val allFarms = repository.getFarms()
        val currentFarm = allFarms.find { it.id == farmId }
        farm = currentFarm

        currentFarm?.let { f ->
            if (f.savedMarketInfo != null) marketInfo = f.savedMarketInfo
            if (f.savedRoadmap != null) roadmapData = f.savedRoadmap
            if (f.savedSatelliteData != null) satelliteData = f.savedSatelliteData
            if (f.savedAnalysisText != null) statusText = f.savedAnalysisText!!

            isFarmingActive = f.isFarming
            if (f.isFarming && f.farmingCropName != null) {
                // If already farming, set the selected crop to the farming one
                selectedCrop = f.savedRecommendedCrops?.find { it.name == f.farmingCropName }

                // Load tasks relative to the stored start date
                val startDate = f.farmingStartDate ?: System.currentTimeMillis()
                // Convert Long millis to LocalDate for repository
                val startLocalDate = LocalDate.ofEpochDay(startDate / (24 * 60 * 60 * 1000))
                farmTasks = FarmScheduleRepository.getSchedule(startLocalDate)
            }

            fetchRecommendations(f)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(farm?.name ?: "Farm Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = {
                        farm?.let {
                            val clearedFarm = it.copy(savedRecommendedCrops = null)
                            farm = clearedFarm
                            fetchRecommendations(clearedFarm)
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(pageScrollState)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // ITEM 1: MAP CARD
                farm?.let { currentFarm ->
                    Card(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                if (currentFarm.polygonPoints.isNotEmpty()) {
                                    val path = Path()
                                    val w = size.width
                                    val h = size.height
                                    val start = currentFarm.polygonPoints.first()
                                    path.moveTo(start.first * w, start.second * h)
                                    for (i in 1 until currentFarm.polygonPoints.size) {
                                        val point = currentFarm.polygonPoints[i]
                                        path.lineTo(point.first * w, point.second * h)
                                    }
                                    path.close()
                                    drawPath(path, Color(0xFF4CAF50).copy(alpha = 0.4f))
                                    drawPath(path, Color(0xFF2E7D32), style = Stroke(width = 3.dp.toPx()))
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Text("📍 $locationName", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ITEM 2: CROP SCHEDULE (Conditional)
                if (isFarmingActive) {
                    FarmScheduleComponent(
                        tasks = farmTasks,
                        onAcknowledge = { id ->
                            farmTasks = farmTasks.map { if (it.id == id) it.copy(isCompleted = true) else it }
                        },
                        onAddPhoto = { id ->
                            farmTasks = farmTasks.map { if (it.id == id) it.copy(isPhotoUploaded = true) else it }
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                } else if (selectedCrop != null) {
                    // Start Farming Confirmation Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Agriculture,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "${LanguageManager.get("Ready to start farming")} ${LanguageManager.get(selectedCrop!!.name ?: "")}?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = LanguageManager.get("Confirming this crop will lock your farm and generate a personalized cultivation schedule."),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        farm?.let { currentFarm ->
                                            val startTime = System.currentTimeMillis()
                                            val updatedFarm = currentFarm.copy(
                                                isFarming = true,
                                                farmingCropName = selectedCrop?.name,
                                                farmingStartDate = startTime
                                            )

                                            // Persist Change
                                            val allFarms = repository.getFarms()
                                            val updatedList = allFarms.map { if (it.id == updatedFarm.id) updatedFarm else it }
                                            repository.saveFarms(updatedList)

                                            // Update UI State
                                            farm = updatedFarm
                                            isFarmingActive = true

                                            // Generate Tasks
                                            val startLocalDate = LocalDate.now()
                                            farmTasks = FarmScheduleRepository.getSchedule(startLocalDate)

                                            Toast.makeText(context, "Farm locked with ${selectedCrop?.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(LanguageManager.get("Confirm & Start Schedule"))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // ITEM 3: EXPANDABLE CROP RECOMMENDATIONS
                // Only allow changing selection if not already farming
                if (!isFarmingActive) {
                    ExpandableCropListLocal(
                        recommendedCrops = recommendedCrops,
                        selectedCrop = selectedCrop,
                        onCropSelected = { selectedCrop = it },
                        isLoading = isRecommending
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(LanguageManager.get("Suggestion Error"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                Text(LanguageManager.get(errorMessage!!), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // ITEM 4: SELECTED CROP QUICK GUIDE
                selectedCrop?.let { crop ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${LanguageManager.get("Quick Guide")}: ${LanguageManager.get(crop.name ?: "")}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("${LanguageManager.get("Sowing")}: ${LanguageManager.get(crop.sowing_months ?: "-")}", style = MaterialTheme.typography.bodyMedium)
                            Text("${LanguageManager.get("Harvest")}: ${LanguageManager.get(crop.harvest_months ?: "-")}", style = MaterialTheme.typography.bodyMedium)
                            Text("${LanguageManager.get("Fertilizer")}: ${LanguageManager.get(crop.fertilizer ?: "-")}", style = MaterialTheme.typography.bodyMedium)
                            Text("${LanguageManager.get("Tools")}: ${LanguageManager.get(crop.tool ?: "-")}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // ITEM 5: SATELLITE DATA
                satelliteData?.let { sat ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(LanguageManager.get("Satellite Health Check"), style = MaterialTheme.typography.titleSmall, color = Color(0xFF00695C), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                InfoItem(LanguageManager.get("NDVI Score"), String.format("%.2f", sat.ndvi_current ?: 0.0), Modifier.weight(1f))
                                InfoItem(LanguageManager.get("Status"), LanguageManager.get(sat.status ?: "Unknown"), Modifier.weight(1f))
                                InfoItem(LanguageManager.get("Trend"), LanguageManager.get(sat.trend ?: "-"), Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("${LanguageManager.get("Cloud Coverage")}: ${sat.cloud_coverage ?: "0%"}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }

                // ITEM 6: MARKET INSIGHT
                marketInfo?.let { market ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(LanguageManager.get("Market Insight"), style = MaterialTheme.typography.titleSmall, color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(LanguageManager.get(market.action ?: "-"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0D47A1))
                            Text(LanguageManager.get(market.reason ?: "-"), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1976D2))
                        }
                    }
                }

                // ITEM 7: ROADMAP CARD
                roadmapData?.let { data ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("${LanguageManager.get("Crop Roadmap")}: ${LanguageManager.get(data.name ?: "Crop")}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                RoadmapDetailItem(LanguageManager.get("Temp"), data.temp_range ?: "-", Modifier.weight(1f))
                                RoadmapDetailItem(LanguageManager.get("Water"), data.water_requirement ?: "-", Modifier.weight(1f))
                                RoadmapDetailItem(LanguageManager.get("pH"), data.ph_range ?: "-", Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                RoadmapDetailItem(LanguageManager.get("Sow"), data.sowing_window ?: "-", Modifier.weight(1f))
                                RoadmapDetailItem(LanguageManager.get("Harvest"), data.harvest_window ?: "-", Modifier.weight(1f))
                                RoadmapDetailItem(LanguageManager.get("Days"), "${data.duration_days ?: 0}", Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            if (!data.pests.isNullOrEmpty()) {
                                Text("${LanguageManager.get("Pests")}: ${data.pests.joinToString(", ") { LanguageManager.get(it) }}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF558B2F))
                            }
                        }
                    }
                }

                // ITEM 8: DETAILED ANALYSIS TEXT
                Text(LanguageManager.get("Detailed Analysis"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (isLoading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        SelectionContainer {
                            Text(LanguageManager.get(statusText), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ITEM 9: ACTION BUTTON (Only if not farming yet)
                if (!isFarmingActive) {
                    Button(
                        onClick = {
                            scope.launch {
                                if (selectedCrop == null) return@launch

                                isLoading = true
                                statusText = LanguageManager.get("Analyzing...") + " " + (selectedCrop!!.name ?: "")
                                marketInfo = null
                                roadmapData = null
                                satelliteData = null

                                try {
                                    farm?.let { currentFarm ->
                                        val cropInput = CropInput(
                                            lat = currentFarm.centroidLat,
                                            lon = currentFarm.centroidLng,
                                            crop = selectedCrop!!.name ?: "Crop",
                                            state = "Maharashtra",
                                            district = "Nashik"
                                        )
                                        val response = retrofit.analyzeCrop(cropInput)
                                        if (response.isSuccessful && response.body() != null) {
                                            val body = response.body()!!
                                            marketInfo = body.market
                                            statusText = body.gemini_explanation ?: LanguageManager.get("No details available.")
                                            roadmapData = body.roadmap
                                            satelliteData = body.satellite

                                            // Save locally
                                            val updatedFarm = currentFarm.copy(
                                                savedMarketInfo = body.market,
                                                savedRoadmap = body.roadmap,
                                                savedSatelliteData = body.satellite,
                                                savedAnalysisText = body.gemini_explanation
                                            )
                                            farm = updatedFarm
                                            val currentList = repository.getFarms()
                                            val updatedList = currentList.map { if (it.id == updatedFarm.id) updatedFarm else it }
                                            repository.saveFarms(updatedList)

                                        } else {
                                            statusText = "${LanguageManager.get("Error")}: ${response.code()}"
                                        }
                                    }
                                } catch (e: Exception) {
                                    statusText = "${LanguageManager.get("Failed")}: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = !isLoading && selectedCrop != null,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(LanguageManager.get("Analyze") + " ${selectedCrop?.name ?: ""}")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(120.dp))
            } ?: run {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

// --- LOCAL COMPONENT DEFINITIONS ---

@Composable
fun ExpandableCropListLocal(
    recommendedCrops: List<RecommendedCrop>,
    selectedCrop: RecommendedCrop?,
    onCropSelected: (RecommendedCrop) -> Unit,
    isLoading: Boolean
) {
    var isExpanded by remember { mutableStateOf(true) }
    val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)

    val cropsBySeason = remember(recommendedCrops) {
        recommendedCrops.groupBy { it.season ?: "Other" }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = LanguageManager.get("🌱 Best Crops for this Soil"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle",
                    modifier = Modifier.rotate(rotationState),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    if (isLoading) {
                        Column(modifier = Modifier.padding(bottom = 16.dp)) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = LanguageManager.get("AI is analyzing soil & climate..."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else if (recommendedCrops.isEmpty()) {
                        Text(
                            text = LanguageManager.get("No recommendations available. Check internet."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            cropsBySeason.forEach { (season, crops) ->
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = LanguageManager.get(season),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                    )
                                    crops.forEach { crop ->
                                        CropItemRowLocal(
                                            crop = crop,
                                            isSelected = selectedCrop?.name == crop.name,
                                            onSelect = { onCropSelected(crop) }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CropItemRowLocal(
    crop: RecommendedCrop,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = LanguageManager.get(crop.name ?: "Unknown Crop"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${LanguageManager.get("Duration")}: ${crop.duration ?: "N/A"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!crop.market_potential.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = LanguageManager.get(crop.market_potential),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RoadmapDetailItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(LanguageManager.get(label), style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(2.dp))
        Text(LanguageManager.get(value), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B5E20))
    }
}

@Composable
fun InfoItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(LanguageManager.get(label), style = MaterialTheme.typography.labelSmall, color = Color.Gray, textAlign = TextAlign.Center)
        Text(LanguageManager.get(value), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}