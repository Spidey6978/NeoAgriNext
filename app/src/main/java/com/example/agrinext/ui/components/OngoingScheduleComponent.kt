package com.example.agrinext.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.agrinext.data.FarmTask
import com.example.agrinext.util.LanguageManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * A dedicated component for the Home Screen that displays only the active "Ongoing"
 * tasks in an expandable/collapsible card format.
 */
@Composable
fun OngoingScheduleComponent(
    tasks: List<FarmTask>,
    onAcknowledge: (Int) -> Unit,
    onAddPhoto: (Int) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "arrow")

    // Filter logic for Ongoing tasks (API 24 Compatible)
    val formatter = remember { SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()) }
    val todayDate = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.time
    }

    val ongoingTasks = remember(tasks) {
        tasks.filter { task ->
            if (task.isCompleted) return@filter false
            val taskDate = try { formatter.parse(task.date) ?: todayDate } catch (e: Exception) { todayDate }
            val isSameDay = formatter.format(taskDate) == formatter.format(todayDate)
            val isOverdue = taskDate.before(todayDate)
            isSameDay || isOverdue
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = LanguageManager.get("Ongoing Crop Schedule"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Expandable Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = ongoingTasks.size.toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = LanguageManager.get("Tasks needing attention"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle",
                        modifier = Modifier.rotate(rotationState),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (ongoingTasks.isEmpty()) {
                            Text(
                                text = LanguageManager.get("Your schedule is clear for now!"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        } else {
                            ongoingTasks.forEach { task ->
                                OngoingTaskItem(task, onAcknowledge, onAddPhoto)
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
private fun OngoingTaskItem(
    task: FarmTask,
    onAcknowledge: (Int) -> Unit,
    onAddPhoto: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = LanguageManager.get(task.stage),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = task.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            Text(
                text = "📸 ${LanguageManager.get(task.actionTitle)}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onAddPhoto(task.id) },
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(if (task.isPhotoUploaded) Icons.Default.Check else Icons.Default.AddAPhoto, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(LanguageManager.get(if (task.isPhotoUploaded) "Photo Added" else "Add Photo"), style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = { onAcknowledge(task.id) },
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(LanguageManager.get("Acknowledge"), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}