package com.example.agrinext.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

// --- Gemini Data Models ---

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val hasImage: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class GeminiRequest(val contents: List<Content>)
data class Content(val parts: List<Part>)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

data class InlineData(
    val mimeType: String,
    val data: String // Base64 encoded string
)

data class GeminiResponse(val candidates: List<Candidate>?)
data class Candidate(val content: Content?)

// --- Gemini API Interface ---

interface GeminiApi {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// --- Gemini Service Singleton ---

object GeminiService {
    // API Key for Chat
    val API_KEY = com.example.agrinext.BuildConfig.GEMINI_API_KEY

    // API Key for Crop Recommendations (Separate Key)
    val RECOMMENDATION_API_KEY = com.example.agrinext.BuildConfig.GEMINI_REC_KEY

    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val gson = Gson()

    val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApi::class.java)
    }

    suspend fun getRecommendedCrops(lat: Double, lng: Double): List<RecommendedCrop> {
        // UPDATED PROMPT: More relaxed and robust regional inference
        val prompt = """
            You are an Agricultural Intelligence AI.

            Task: Identify the region for coordinates ($lat, $lng) and recommend 5-7 suitable crops.

            Instructions:
            1. First, identify the District, State, or General Region for these coordinates.
            2. Based on that region's typical climate (rainfall, temperature) and soil profile, recommend crops.
            3. Include a diverse mix: Grains, Vegetables, Fruits, and Cash Crops.
            4. Even if the specific point is not a farm (e.g., a city), suggest crops suitable for the surrounding agricultural area.
            5. STRICTLY output valid JSON. Do not use markdown code blocks (```json).

            Required JSON Format:
            [
              {
                "name": "Crop Name",
                "season": "e.g., Kharif, Rabi, Summer",
                "sowing_months": "Month-Month",
                "harvest_months": "Month-Month",
                "duration": "e.g., 90-120 days",
                "fertilizer": "Brief recommendation",
                "tool": "Required machinery",
                "market_potential": "High/Medium/Low"
              }
            ]
        """.trimIndent()

        val request = GeminiRequest(listOf(Content(listOf(Part(text = prompt)))))

        try {
            val response = api.generateContent(RECOMMENDATION_API_KEY, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (jsonText != null) {
                // Clean up any markdown code blocks if Gemini ignores the instruction
                val cleanJson = jsonText.replace("```json", "").replace("```", "").trim()
                val type = object : TypeToken<List<RecommendedCrop>>() {}.type
                return gson.fromJson(cleanJson, type)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return emptyList()
    }
}