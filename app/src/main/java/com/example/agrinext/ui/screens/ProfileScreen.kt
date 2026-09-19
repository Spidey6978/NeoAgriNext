package com.example.agrinext.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agrinext.util.LanguageManager
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    isDarkMode: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current

    // State for the custom expandable language menu
    var isLanguageExpanded by remember { mutableStateOf(false) }
    // Animation for the arrow rotation
    val arrowRotation by animateFloatAsState(targetValue = if (isLanguageExpanded) 180f else 0f, label = "arrowRotation")

    // Hardcoded list to ensure Gujarati is included as requested
    val languages = listOf("English", "Hindi", "Punjabi", "Marathi", "Gujarati", "Spanish")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        LanguageManager.get("Profile Settings"),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
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
                .verticalScroll(rememberScrollState()) // Enable scrolling for smaller screens
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // User Avatar
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // User Name
            Text(
                text = user?.displayName ?: "Farmer",
                style = MaterialTheme.typography.headlineSmall
            )

            // Email ID
            Text(
                text = user?.email ?: "",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Preferences Label
            Text(
                text = LanguageManager.get("Preferences"),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start),
                color = MaterialTheme.colorScheme.outline
            )

            // Dark Mode Switch Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            LanguageManager.get("Dark Mode"),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            if (isDarkMode) LanguageManager.get("Dark theme enabled") else LanguageManager.get("Light theme enabled"),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onThemeToggle
                    )
                }
            }

            // Custom Expandable Language Card
            // Interaction source allows us to capture clicks without showing a ripple
            val interactionSource = remember { MutableInteractionSource() }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    // Toggle expansion on click with NO ripple indication
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { isLanguageExpanded = !isLanguageExpanded },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column {
                    // Header Row (Always visible)
                    Row(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                LanguageManager.get("Language"),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                LanguageManager.currentLanguage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select Language",
                            modifier = Modifier.rotate(arrowRotation)
                        )
                    }

                    // Expanded List Section
                    AnimatedVisibility(visible = isLanguageExpanded) {
                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            )

                            languages.forEach { language ->
                                val isSelected = LanguageManager.currentLanguage == language
                                val itemShape = RoundedCornerShape(8.dp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 4.dp) // Outer padding for spacing between items
                                        .clip(itemShape) // Ensure ripple respects the shape
                                        .background(
                                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                                            shape = itemShape
                                        )
                                        .clickable {
                                            LanguageManager.changeLanguage(context, language)
                                        }
                                        .padding(vertical = 12.dp, horizontal = 20.dp), // Inner padding for content
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = language,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                    )

                                    // Radio Button Circle (Filled if selected)
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = if (isSelected) "Selected" else "Unselected",
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Logout
            Button(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    LanguageManager.get("Logout"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onError
                )
            }
        }
    }
}