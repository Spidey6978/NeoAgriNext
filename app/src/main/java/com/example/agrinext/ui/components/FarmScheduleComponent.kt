package com.example.agrinext.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.agrinext.data.FarmTask
import com.example.agrinext.util.LanguageManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * Optimized Farm Schedule Component
 * Categorizes tasks from the Backend into Ongoing, Upcoming, and Completed sections.
 */
@Composable
fun FarmScheduleComponent(
    tasks: List<FarmTask>,
    onAcknowledge: (Int) -> Unit,
    onAddPhoto: (Int) -> Unit
) {
    val formatter = remember { SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()) }
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.time

    // Filter logic to separate tasks based on completion and date
    val completed = tasks.filter { it.isCompleted }
    val ongoing = tasks.filter {
        !it.isCompleted && (
                formatter.parse(it.date)?.before(today) == true ||
                        formatter.format(formatter.parse(it.date)) == formatter.format(today)
                )
    }
    val upcoming = tasks.filter {
        !it.isCompleted && formatter.parse(it.date)?.after(today) == true
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScheduleSection("Ongoing", ongoing, true, onAcknowledge, onAddPhoto, true)
        ScheduleSection("Upcoming", upcoming, false, onAcknowledge, onAddPhoto, false)
        ScheduleSection("Completed", completed, false, onAcknowledge, onAddPhoto, false)
    }
}

@Composable
private fun ScheduleSection(
    title: String,
    tasks: List<FarmTask>,
    initiallyExpanded: Boolean,
    onAck: (Int) -> Unit,
    onPhoto: (Int) -> Unit,
    isInteractive: Boolean
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "rotation")

    if (tasks.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = LanguageManager.get(title),
                        fontWeight = FontWeight.Bold,
                        color = if(isInteractive) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                    Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.rotate(rotation))
                }
                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        tasks.forEach { task ->
                            TaskItem(task, onAck, onPhoto, isInteractive)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskItem(
    task: FarmTask,
    onAck: (Int) -> Unit,
    onPhoto: (Int) -> Unit,
    isInteractive: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if(isInteractive) 1f else 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(task.stage, fontWeight = FontWeight.Bold)
            Text(task.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)

            if (!task.actionSubtitle.isNullOrBlank()) {
                Text(
                    text = task.actionSubtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isInteractive) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onPhoto(task.id) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Photo", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = { onAck(task.id) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Done", style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else if (task.isCompleted) { // FIXED: Changed typo isCompletead to isCompleted
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Completed", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                }
            }
        }
    }
}