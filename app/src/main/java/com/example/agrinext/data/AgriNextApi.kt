package com.example.agrinext.data

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

// --- 1. Recommendation Models ---
data class RecommendedCrop(
    val name: String?,
    val season: String?,
    val sowing_months: String?,
    val harvest_months: String?,
    val duration: String?,
    val fertilizer: String?,
    val tool: String?,
    val market_potential: String?,
    val imageUrl: String? = null
)

data class CropRecommendationResponse(
    val location: String?,
    val crops: List<RecommendedCrop>?
)

// --- 2. Analysis Models ---
data class MarketInfo(
    val action: String?,
    val reason: String?
)

// --- 3. Input Models ---
data class CropInput(
    val lat: Double,
    val lon: Double,
    val crop: String,
    val state: String = "Maharashtra",
    val district: String = "Nashik"
)

// --- 4. Status Model ---
data class StatusResponse(val status: String?, val message: String?)

// --- 5. User Profile Model ---
data class UserProfileResponse(
    val name: String?,
    val email: String?
)

// --- 6. Crop Roadmap ---
data class CropRoadmap(
    val name: String?,
    val temp_range: String?,
    val ph_range: String?,
    val water_requirement: String?,
    val sowing_window: String?,
    val harvest_window: String?,
    val duration_days: Int?,
    val fertilizer_plan: String?,
    val pests: List<String>?
)

// --- 7. Satellite Data ---
data class SatelliteData(
    val ndvi_current: Double?,
    val ndvi_last_week: Double?,
    val trend: String?,
    val status: String?,
    val satellite: String?,
    val cloud_coverage: String?
)

// --- 8. Advisory Response ---
data class AdvisoryResponse(
    val market: MarketInfo?,
    val gemini_explanation: String?,
    val roadmap: CropRoadmap? = null,
    val satellite: SatelliteData? = null
)

// --- 9. News Item Model ---
data class NewsItem(
    val id: Int,
    val title: String,
    val source: String,
    val timeAgo: String,
    val category: String,
    val imageUrl: String?,
    val content: String?
)

// --- 10. Dynamic Schedule Models ---
data class FarmScheduleResponse(
    val tasks: List<FarmTask>,
    val farm_score: Int
)

// --- THE INTERFACE ---
interface AgriNextApi {
    @GET("/status")
    suspend fun getStatus(): Response<StatusResponse>

    @GET("user/profile")
    suspend fun getUserProfile(): Response<UserProfileResponse>

    @GET("/recommend_crops")
    suspend fun getRecommendedCrops(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): Response<CropRecommendationResponse>

    @POST("/analyze")
    suspend fun analyzeCrop(@Body input: CropInput): Response<AdvisoryResponse>

    @GET("/news")
    suspend fun getAgriNews(): Response<List<NewsItem>>

    // Real-time dynamic schedule based on sowing date
    @GET("/schedule")
    suspend fun getFarmSchedule(
        @Query("crop") crop: String,
        @Query("sowing_date") sowingDate: String? = null // Format: YYYY-MM-DD
    ): Response<FarmScheduleResponse>

    // Task interaction endpoints for "full-proof" operation
    @POST("/schedule/acknowledge")
    suspend fun acknowledgeTask(
        @Query("task_id") taskId: Int,
        @Query("farm_id") farmId: Long
    ): Response<StatusResponse>

    @Multipart
    @POST("/schedule/upload_photo")
    suspend fun uploadTaskPhoto(
        @Part("task_id") taskId: Int,
        @Part("farm_id") farmId: Long,
        @Part image: MultipartBody.Part
    ): Response<StatusResponse>
}