package com.example.agrinext.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.agrinext.Config
import com.example.agrinext.data.AgriNextApi
import com.example.agrinext.data.CropInput
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@Composable
fun BackendTestScreen(onBack: () -> Unit) {
    var statusText by remember { mutableStateOf("🔴 Ready to test...") }
    val scope = rememberCoroutineScope()

    // Scroll state for the TEXT BOX only
    val textScrollState = rememberScrollState()

    // Setup Retrofit with 60s Timeout
    val retrofit = remember {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(Config.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AgriNextApi::class.java)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween, // Pushes buttons to bottom
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. HEADER (Fixed)
        Text("Backend Logic Lab", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // 2. OUTPUT BOX (Takes all middle space & Scrolls)
        Card(
            modifier = Modifier
                .weight(1f) // <--- FILL ALL AVAILABLE SPACE
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // Internal Scroll Container
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(textScrollState) // <--- SCROLLS HERE
            ) {
                // Allows you to copy-paste the text!
                SelectionContainer {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // 3. BUTTONS (Fixed at Bottom)
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    scope.launch {
                        try {
                            statusText = "🟡 Pinging..."
                            val response = retrofit.getStatus()
                            if (response.isSuccessful) {
                                statusText = "🟢 CONNECTION OK!\n${response.body()?.message}"
                            } else {
                                statusText = "🔴 Server Error: ${response.code()}"
                            }
                        } catch (e: Exception) {
                            statusText = "🔴 Failed: ${e.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("1. Ping Connection")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    scope.launch {
                        try {
                            statusText = "🟡 AI is Thinking... (Wait 20s)"
                            // Test Data (Nashik)
                            val dummyInput = CropInput(
                                lat = 20.0059,
                                lon = 73.7898,
                                crop = "Tomato"
                            )
                            val response = retrofit.analyzeCrop(dummyInput)

                            if (response.isSuccessful) {
                                statusText = "🟢 GOD MODE SUCCESS!\n\n${response.body()?.gemini_explanation}"
                            } else {
                                statusText = "🔴 Logic Failed: ${response.code()}\nCheck Python Terminal!"
                            }
                        } catch (e: Exception) {
                            statusText = "🔴 Failed: ${e.message}"
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("2. Test Weather Logic")
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Go Back") }
        }

        // 4. BOTTOM BAR SPACER (Lifts content above the custom bar)
        Spacer(modifier = Modifier.height(100.dp))
    }
}