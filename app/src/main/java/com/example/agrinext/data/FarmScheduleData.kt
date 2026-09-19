package com.example.agrinext.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class FarmTask(
    val id: Int,
    val stage: String,
    val triggerInfo: String,
    val actionTitle: String,
    val actionSubtitle: String? = null,
    val daysOffset: Long, // Days relative to sowing date
    var date: String = "", // Calculated actual date
    var isCompleted: Boolean = false,
    var isPhotoUploaded: Boolean = false
)

object FarmScheduleRepository {
    // Generate schedule based on a reference Sowing Date
    fun getSchedule(sowingDate: LocalDate = LocalDate.now()): List<FarmTask> {
        val formatter = DateTimeFormatter.ofPattern("dd MMM, yyyy")

        val tasks = listOf(
            FarmTask(1, "Pre-Season Planning", "10–20 days before sowing", "Field condition", "OR Soil Health Card", -15),
            FarmTask(2, "Land Preparation", "7–10 days before sowing", "Ploughed / prepared field", null, -7),
            FarmTask(3, "Seed Selection", "3–5 days before sowing", "Seed packet", "OR Seed treatment process", -3),
            FarmTask(4, "Sowing / Transplanting", "Sowing day", "Sowing in progress", "OR Transplanted seedlings", 0),
            FarmTask(5, "Germination", "7–10 days after sowing", "Seedling emergence", null, 10),
            FarmTask(6, "Nutrient Management", "15–25 days after sowing", "Fertilizer application", "OR Crop leaf condition", 20),
            FarmTask(7, "Irrigation Management", "Multiple (auto reminders)", "Irrigation method used", "OR Soil moisture condition", 25),
            FarmTask(8, "Weed Control", "20–30 days after sowing", "Weed presence in field", null, 30),
            FarmTask(9, "Pest & Disease", "On detection / AI alert", "Affected plant / pest", "(Strongest AI feature)", 40),
            FarmTask(10, "Crop Growth Monitoring", "Mid-season", "Full field view", null, 60),
            FarmTask(11, "Flowering & Fruiting", "Crop-stage specific", "Flowers / fruits", null, 80),
            FarmTask(12, "Harvest Planning", "7–10 days before harvest", "Mature crop readiness", null, 110),
            FarmTask(13, "Harvesting", "Harvest window", "Harvested produce", null, 120),
            FarmTask(14, "Post-Harvest Handling", "Immediately after harvest", "Grading / packing", null, 121),
            FarmTask(15, "Marketing & Selling", "After post-harvest", "Crop lot for sale", "(Feeds into marketplace)", 125),
            FarmTask(16, "Post-Season Review", "After sale completion", "Field after harvest", "OR Yield summary screenshot", 130)
        )

        // Calculate and assign actual dates
        return tasks.map { task ->
            val taskDate = sowingDate.plusDays(task.daysOffset)
            task.copy(date = taskDate.format(formatter))
        }
    }
}