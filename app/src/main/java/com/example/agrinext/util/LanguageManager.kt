package com.example.agrinext.util

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.File
import java.util.concurrent.TimeUnit

// --- Google Translate (GTX) API Interface ---
interface GoogleTranslateApi {
    @GET("translate_a/single?client=gtx&dt=t")
    suspend fun translate(
        @Query("sl") source: String = "en",
        @Query("tl") target: String,
        @Query("q") query: String
    ): JsonArray
}

object LanguageManager {
    // Added Gujarati to the list
    val supportedLanguages = listOf("English", "Hindi", "Punjabi", "Marathi", "Gujarati", "Spanish")

    var currentLanguage by mutableStateOf("English")
        private set

    private val _translations = mutableStateMapOf<String, String>()
    private val pendingSet = mutableSetOf<String>()

    private val translationQueue = Channel<String>(Channel.UNLIMITED)

    private var translationService: GoogleTranslateApi? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var appContext: Context? = null

    private val appStrings = listOf(
        "Home", "My Farm", "Test", "Community", "Ask Elio",
        "AgriNext AI", "How can I help your farm today?",
        "Try asking about weather, crop health, or soil status.",
        "Type your question...", "Profile Settings", "Preferences",
        "Dark Mode", "Dark theme enabled", "Light theme enabled",
        "Language", "Select App Language", "Logout",
        "Add New Farm", "Locate your farm", "Search location",
        "Confirm Location", "Mark Farm Boundary", "Add Point",
        "Undo", "Save Farm", "Name Your Farm", "Farm Name",
        "Live Weather", "Local Forecast", "Soil Health", "Stable & Moist",
        "Backend Test", "Backend Connection Test", "PING SERVER", "Go Back",
        "Hello", "Analyze Farm Data", "Analyzing...", "Centroid:", "Farm Analytics",
        "Edit Farm", "Farm Details", "Coordinates:", "Farm ID:", "Loc:",
        "North Field", "South Pasture", "East Orchard",
        "Lat:", "Lng:", "AI Analysis", "Ready to analyze farm data...",
        "(Placeholder for graphs, soil data, etc.)", "Connection Error:", "Analysis Failed:", "ANALYSIS RECEIVED:", "Sending farm coordinates to AI..."
    )

    fun get(text: String): String {
        if (currentLanguage == "English") return text
        if (text.isBlank()) return text

        val translated = _translations[text]
        if (translated.isNullOrBlank()) {
            if (!pendingSet.contains(text)) {
                pendingSet.add(text)
                scope.launch { translationQueue.send(text) }
            }
            return text
        }
        return translated
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
        setupApi()

        scope.launch {
            for (text in translationQueue) {
                if (currentLanguage != "English") {
                    fetchTranslationSuspend(text, currentLanguage)
                    delay(200)
                }
                pendingSet.remove(text)
            }
        }

        val sharedPref = context.getSharedPreferences("AgriNextSettings", Context.MODE_PRIVATE)
        val savedLang = sharedPref.getString("language", "English") ?: "English"

        if (savedLang != "English") {
            changeLanguage(context, savedLang)
        }
    }

    private fun setupApi() {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val gson = GsonBuilder().setLenient().create()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://translate.googleapis.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

            translationService = retrofit.create(GoogleTranslateApi::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun changeLanguage(context: Context, language: String) {
        currentLanguage = language

        val sharedPref = context.getSharedPreferences("AgriNextSettings", Context.MODE_PRIVATE)
        sharedPref.edit().putString("language", language).apply()

        if (language == "English") {
            _translations.clear()
            return
        }

        val cached = loadFromCache(context, language)
        _translations.clear()
        _translations.putAll(cached)

        appStrings.filter { !_translations.containsKey(it) }.forEach { text ->
            if (!pendingSet.contains(text)) {
                pendingSet.add(text)
                scope.launch { translationQueue.send(text) }
            }
        }
    }

    private suspend fun fetchTranslationSuspend(text: String, language: String) {
        try {
            val langCode = when(language) {
                "Hindi" -> "hi"
                "Punjabi" -> "pa"
                "Marathi" -> "mr"
                "Gujarati" -> "gu" // Added Gujarati code
                "Spanish" -> "es"
                else -> "en"
            }

            val responseArray = translationService?.translate(target = langCode, query = text)

            if (responseArray != null && responseArray.size() > 0) {
                val sentences = responseArray.get(0).asJsonArray
                if (sentences.size() > 0) {
                    val firstSentence = sentences.get(0).asJsonArray
                    val translatedText = firstSentence.get(0).asString

                    if (!translatedText.isNullOrEmpty()) {
                        _translations[text] = translatedText
                        appContext?.let { saveToCache(it, language) }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LanguageManager", "API Error: ${e.message}")
        }
    }

    private fun loadFromCache(context: Context, language: String): Map<String, String> {
        val file = File(context.filesDir, "translations_$language.json")
        if (!file.exists()) return emptyMap()

        return try {
            val json = file.readText()
            val type = object : TypeToken<Map<String, String>>() {}.type
            Gson().fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun saveToCache(context: Context, language: String) {
        try {
            val file = File(context.filesDir, "translations_$language.json")
            val mapCopy = _translations.toMap()
            val json = Gson().toJson(mapCopy)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}