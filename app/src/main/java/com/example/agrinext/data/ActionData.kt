package com.example.agrinext.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Grass
import androidx.compose.ui.graphics.vector.ImageVector

data class ActionItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val time: String,
    val isUrgent: Boolean = false
)

// Sample Data Provider
object ActionRepository {
    val upcomingActions = listOf(
        ActionItem(
            id = 1,
            title = "Fertilize Corn Field",
            subtitle = "Apply Nitrogen-based fertilizer",
            icon = Icons.Default.Grass,
            time = "Today, 10:00 AM",
            isUrgent = true
        ),
        ActionItem(
            id = 2,
            title = "Irrigation Check",
            subtitle = "Inspect North Field sprinklers",
            icon = Icons.Default.WaterDrop,
            time = "Tomorrow, 08:00 AM",
            isUrgent = false
        ),
        ActionItem(
            id = 3,
            title = "Pest Control",
            subtitle = "Check for aphid activity",
            icon = Icons.Default.BugReport,
            time = "Wed, 09:00 AM",
            isUrgent = false
        )
    )
}