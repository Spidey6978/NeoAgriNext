package com.example.agrinext.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object NewsStorage {
    private const val FILE_NAME = "news_cache.json"
    private val gson = Gson()

    fun saveNews(context: Context, items: List<NewsItem>) {
        try {
            val jsonString = gson.toJson(items)
            val file = File(context.filesDir, FILE_NAME)
            file.writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadNews(context: Context): List<NewsItem>? {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return null

        return try {
            val jsonString = file.readText()
            val type = object : TypeToken<List<NewsItem>>() {}.type
            gson.fromJson(jsonString, type)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}