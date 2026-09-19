package com.example.agrinext.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.agrinext.data.FarmScheduleRepository
import com.example.agrinext.ui.components.NewsCarouselComponent
import com.example.agrinext.ui.components.OngoingScheduleComponent
import com.example.agrinext.ui.components.Speedometer
import com.example.agrinext.util.LanguageManager
import java.time.LocalDate

@Composable
fun HomeScreen(
    onWeatherClick: () -> Unit,
    onNewsClick: (Int) -> Unit
) {
    // Local state for tasks (demonstration using dummy schedule)
    var homeTasks by remember {
        mutableStateOf(FarmScheduleRepository.getSchedule(LocalDate.now()))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 120.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 1. Speedometer Card
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = LanguageManager.get("Farm Health"),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Speedometer(
                                currentValue = 85f,
                                maxValue = 100f,
                                modifier = Modifier
                                    .height(250.dp)
                                    .fillMaxWidth(),
                                label = LanguageManager.get("Health Score")
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = LanguageManager.get("Your farm is in excellent condition!"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            // 2. News Carousel (Edge-to-Edge)
            item {
                NewsCarouselComponent(onNewsClick = onNewsClick)
            }

            // 3. Ongoing Crop Schedule (Edge-to-Edge for optimal visibility)
            item {
                OngoingScheduleComponent(
                    tasks = homeTasks,
                    onAcknowledge = { id ->
                        homeTasks = homeTasks.map { if (it.id == id) it.copy(isCompleted = true) else it }
                    },
                    onAddPhoto = { id ->
                        homeTasks = homeTasks.map { if (it.id == id) it.copy(isPhotoUploaded = true) else it }
                    }
                )
            }
        }
    }
}