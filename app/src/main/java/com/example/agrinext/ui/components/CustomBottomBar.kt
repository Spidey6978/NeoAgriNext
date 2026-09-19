package com.example.agrinext.ui.components

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.RecognizerIntent
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.agrinext.data.ChatMessage
import com.example.agrinext.data.Content
import com.example.agrinext.data.GeminiRequest
import com.example.agrinext.data.GeminiService
import com.example.agrinext.data.InlineData
import com.example.agrinext.data.Part
import com.example.agrinext.data.Screen
import com.example.agrinext.util.LanguageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream

// --- Component ---

@Composable
fun CustomBottomBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    focusManager: FocusManager? = null,
    onChatBoxClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // --- Screen Metrics & State ---
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeight = configuration.screenHeightDp.dp

    // Dynamic Min Height Calculation
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val minHeight = 96.dp + navBarHeight

    val heightAnimatable = remember { Animatable(minHeight.value) }
    val scope = rememberCoroutineScope()

    val isChatExpanded = heightAnimatable.value > 150f

    var chatText by remember { mutableStateOf("") }

    BackHandler(enabled = isChatExpanded) {
        scope.launch {
            heightAnimatable.animateTo(minHeight.value)
            focusManager?.clearFocus()
        }
    }

    val context = LocalContext.current

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0)
            if (!spokenText.isNullOrEmpty()) {
                chatText = spokenText
                scope.launch { heightAnimatable.animateTo(screenHeight.value) }
            }
        }
    }

    val onMicClick: () -> Unit = {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, LanguageManager.get("Type your question..."))
            }
            try {
                speechLauncher.launch(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Speech recognition not available", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
        }
    }

    val barColor = MaterialTheme.colorScheme.surfaceVariant
    val expandedBackgroundColor = MaterialTheme.colorScheme.background
    val glowColor = MaterialTheme.colorScheme.primary

    val insets = if (isChatExpanded) {
        WindowInsets.navigationBars.union(WindowInsets.ime)
    } else {
        WindowInsets.navigationBars
    }

    Surface(
        color = if (isChatExpanded) expandedBackgroundColor else barColor,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(heightAnimatable.value.dp)
            .windowInsetsPadding(insets)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(glowColor.copy(alpha = 0.3f), Color.Transparent)
                ),
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            val snapThreshold = screenHeight.value * 0.4f
                            val targetHeight = if (heightAnimatable.value > snapThreshold) {
                                screenHeight.value
                            } else {
                                minHeight.value
                            }

                            heightAnimatable.animateTo(
                                targetValue = targetHeight,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )

                            if (targetHeight == minHeight.value) {
                                focusManager?.clearFocus()
                            }
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            val newHeight = (heightAnimatable.value - (dragAmount / density.density))
                                .coerceIn(minHeight.value, screenHeight.value)

                            heightAnimatable.snapTo(newHeight)
                        }
                    }
                )
            }
    ) {
        Column {
            if (isChatExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .height(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.Gray.copy(alpha = 0.4f))
                    )
                }
            }

            AnimatedContent(
                targetState = isChatExpanded,
                label = "BottomBarContent"
            ) { expanded ->
                if (expanded) {
                    AiChatOverlay(
                        text = chatText,
                        onTextChange = { chatText = it },
                        onCollapse = {
                            scope.launch { heightAnimatable.animateTo(minHeight.value) }
                            focusManager?.clearFocus()
                        },
                        onMicClick = onMicClick
                    )
                } else {
                    BottomNavigationRow(
                        selectedIndex = selectedIndex,
                        onItemSelected = onItemSelected,
                        focusManager = focusManager,
                        onChatClick = {
                            scope.launch { heightAnimatable.animateTo(screenHeight.value) }
                        },
                        onMicClick = onMicClick
                    )
                }
            }
        }
    }
}

@Composable
fun AiChatOverlay(
    text: String,
    onTextChange: (String) -> Unit,
    onCollapse: () -> Unit,
    onMicClick: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    val currentLanguage = LanguageManager.currentLanguage
    val systemInstruction = """
        You are Elio, an expert agricultural AI assistant for the AgriNext app.
        Your goal is to help farmers with crop health, weather interpretation, soil management, and farming best practices.
        If an image is provided, analyze it for crop diseases, pests, or growth stages.
        Keep your answers concise, practical, and easy to understand.
        
        IMPORTANT: The user's preferred language is $currentLanguage. 
        You MUST reply in $currentLanguage, regardless of the language of the user's question.
    """.trimIndent()

    suspend fun uriToBase64(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) {
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank() && selectedImageUri == null) return

        val currentImageUri = selectedImageUri
        val hasImage = currentImageUri != null

        val displayMessage = if(hasImage) "$userMessage [Image Attached]" else userMessage
        val newMessages = messages + ChatMessage(displayMessage, true, hasImage)
        messages = newMessages

        onTextChange("")
        selectedImageUri = null
        isLoading = true

        scope.launch {
            if (newMessages.isNotEmpty()) {
                listState.animateScrollToItem(newMessages.size - 1)
            }

            try {
                val parts = mutableListOf<Part>()
                val fullPrompt = "$systemInstruction\n\nUser: $userMessage"
                parts.add(Part(text = fullPrompt))

                if (hasImage) {
                    val base64Image = uriToBase64(currentImageUri!!)
                    if (base64Image != null) {
                        parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image)))
                    }
                }

                val request = GeminiRequest(listOf(Content(parts)))
                val response = GeminiService.api.generateContent(GeminiService.API_KEY, request)

                val aiResponseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Sorry, I couldn't understand that."

                messages = messages + ChatMessage(aiResponseText, false)
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            } catch (e: Exception) {
                messages = messages + ChatMessage("Error: ${e.localizedMessage}", false)
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        // focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onCollapse() }) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = LanguageManager.get("AgriNext AI"),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Box(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = LanguageManager.get("How can I help your farm today?"),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = LanguageManager.get("Try asking about weather, crop health, or soil status."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(messages) { msg ->
                        ChatBubble(message = msg)
                    }
                    if (isLoading) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedImageUri != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Image, contentDescription = "Image", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Image attached", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                IconButton(onClick = { selectedImageUri = null }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Gray)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(30.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Attach Image",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (text.isEmpty()) {
                    Text(
                        text = LanguageManager.get("Type your question..."),
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (text.isNotEmpty() || selectedImageUri != null) {
                        sendMessage(text)
                    } else {
                        onMicClick()
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(
                    imageVector = if (text.isNotEmpty() || selectedImageUri != null) Icons.Default.Send else Icons.Default.Mic,
                    contentDescription = "Action",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val backgroundColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val align = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (message.isUser)
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    else
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = align) {
        Surface(
            color = backgroundColor,
            shape = shape,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.hasImage) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp), tint = contentColor.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("[Image Sent]", color = contentColor.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
                Text(
                    text = message.text,
                    color = contentColor,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun BottomNavigationRow(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    focusManager: FocusManager?,
    onChatClick: () -> Unit,
    onMicClick: () -> Unit
) {
    val iconColor = Color.Gray
    val selectedIconColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 4.dp, top = 10.dp, end = 4.dp, bottom = 25.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        NavBarIcon(Screen.Home, selectedIndex, onItemSelected, iconColor, selectedIconColor, focusManager, Modifier.weight(0.5f))
        NavBarIcon(Screen.MyFarm, selectedIndex, onItemSelected, iconColor, selectedIconColor, focusManager, Modifier.weight(0.5f))

        NavbarChatTrigger(onChatClick = { onChatClick() }, onMicClick = { onMicClick() }, modifier = Modifier.weight(1.5f))

        NavBarIcon(Screen.BackendTest, selectedIndex, onItemSelected, iconColor, selectedIconColor, focusManager, Modifier.weight(0.5f))
        // REMOVED Screen.Community to fix compilation error and keep 4 icons + chat
        // Replaced with Screen.Marketplace as the 4th icon (rightmost)
        NavBarIcon(Screen.Marketplace, selectedIndex, onItemSelected, iconColor, selectedIconColor, focusManager, Modifier.weight(0.5f))
    }
}

@Composable
fun NavbarChatTrigger(
    onChatClick: () -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val boxBackground = MaterialTheme.colorScheme.background
    val hintColor = Color.Gray
    val activeColor = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "boxScale"
    )

    Row(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .height(44.dp)
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .border(BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)), RoundedCornerShape(22.dp))
            .background(boxBackground)
            .padding(end = 4.dp)
            .clickable(indication = null, interactionSource = interactionSource) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onChatClick()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
            Text(
                text = LanguageManager.get("Ask Elio"),
                color = hintColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(start = 20.dp)
            )
        }

        val micInteractionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).clickable(indication = null, interactionSource = micInteractionSource) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onMicClick()
            },
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Mic, contentDescription = "Voice", tint = activeColor, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun RowScope.NavBarIcon(
    screen: Screen,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    defaultColor: Color,
    selectedColor: Color,
    focusManager: FocusManager?,
    modifier: Modifier = Modifier
) {
    val isSelected = selectedIndex == screen.index
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.8f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "iconScale")

    Box(modifier = modifier.height(44.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.size(44.dp).scale(scale).clip(CircleShape).clickable(indication = null, interactionSource = interactionSource) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                focusManager?.clearFocus()
                onItemSelected(screen.index)
            },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = screen.icon,
                contentDescription = LanguageManager.get(screen.title),
                tint = if (isSelected) selectedColor else defaultColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}