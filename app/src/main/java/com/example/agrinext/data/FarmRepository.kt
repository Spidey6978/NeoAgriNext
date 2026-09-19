package com.example.agrinext.data

import android.content.Context
import com.example.agrinext.ui.components.FarmLand
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class FarmRepository(private val context: Context) {
    private val gson = Gson()
    private val fileName = "my_farms_data.json"

    // Save the list of farms to local JSON file
    fun saveFarms(farms: List<FarmLand>) {
        try {
            val jsonString = gson.toJson(farms)
            val file = File(context.filesDir, fileName)
            file.writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Load the list of farms from local JSON file
    fun getFarms(): List<FarmLand> {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) {
            return emptyList()
        }

        return try {
            val jsonString = file.readText()
            val type = object : TypeToken<List<FarmLand>>() {}.type
            gson.fromJson(jsonString, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}