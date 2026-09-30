package com.example.agrinext.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agrinext.data.MockAdvisoryRepository
import com.example.agrinext.util.LanguageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvisoryDashboardScreen(onBack: () -> Unit) {
    val soil = MockAdvisoryRepository.sampleSoil
    val satellite = MockAdvisoryRepository.sampleSatellite
    val alerts = MockAdvisoryRepository.sampleAlerts

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LanguageManager.get("Agro-Advisory Hub")) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Alerts Section
            Text("Active Agricultural Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            alerts.forEach { alert ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (alert.severity == "Warning") Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = if (alert.severity == "Warning") Icons.Default.Warning else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (alert.severity == "Warning") Color(0xFFE65100) else Color(0xFF2E7D32),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(alert.title, fontWeight = FontWeight.Bold, color = Color.Black)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(alert.description, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                        }
                    }
                }
            }

            // 2. Satellite Telemetry Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🛰️ Satellite Crop Telemetry", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Current NDVI", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("${satellite.ndviScore}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                        Column {
                            Text("Vigor Status", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(satellite.status, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Trend", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(satellite.trend, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Telemetry Feed: ${satellite.satelliteSource}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }

            // 3. Soil N-P-K Health Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🌱 Soil Health Matrix", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        NutrientBadge("N (Nitrogen)", "${soil.nitrogen} kg/ha", Color(0xFF1565C0))
                        NutrientBadge("P (Phosphorus)", "${soil.phosphorus} kg/ha", Color(0xFFC62828))
                        NutrientBadge("K (Potassium)", "${soil.potassium} kg/ha", Color(0xFF6A1B9A))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Soil pH: ${soil.ph} (Optimal 6.5-7.5)", style = MaterialTheme.typography.bodySmall)
                        Text("Moisture: ${soil.moisturePercent}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun NutrientBadge(label: String, value: String, accentColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(accentColor.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = accentColor, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = Color.Black)
        }
    }
}