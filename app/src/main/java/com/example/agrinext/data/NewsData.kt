package com.example.agrinext.data

// Uses NewsItem from AgriNextApi.kt

object NewsRepository {
    // Fallback data matching your Python backend's FALLBACK_NEWS
    val newsList = listOf(
        NewsItem(
            id = 1,
            title = "New Subsidy for Organic Fertilizers Announced",
            source = "AgriNews Daily",
            timeAgo = "2h ago",
            category = "Policy",
            imageUrl = "https://images.unsplash.com/photo-1625246333195-5848c4281413?auto=format&fit=crop&w=600&q=80",
            content = "The government has announced a new subsidy scheme aimed at promoting organic farming across the state. Under this new initiative, farmers will receive a 50% subsidy on the purchase of certified organic fertilizers and bio-pesticides.\n\nThe goal is to reduce dependency on chemical fertilizers which degrade soil health over time. Applications for the subsidy can be submitted through the local Krishi Kendra starting next month.\n\nExperts believe this move will not only improve soil quality but also fetch premium prices for organic produce in international markets."
        ),
        NewsItem(
            id = 2,
            title = "Monsoon Forecast: Heavy Rains Expected in Nashik",
            source = "Weather Bureau",
            timeAgo = "5h ago",
            category = "Weather",
            imageUrl = "https://images.unsplash.com/photo-1515694346937-94d85e41e6f0?auto=format&fit=crop&w=600&q=80",
            content = "Farmers in the Nashik district are advised to take precautionary measures as heavy rainfall is predicted over the next 48 hours. The meteorological department has issued an orange alert for the region.\n\nEnsure drainage channels are clear to prevent waterlogging in fields, especially for crops like onions and grapes which are sensitive to excess moisture. It is also recommended to delay any planned spraying of pesticides until the weather clears up."
        ),
        NewsItem(
            id = 3,
            title = "Tomato Prices Surge by 20% in Local Markets",
            source = "Market Watch",
            timeAgo = "1d ago",
            category = "Market",
            imageUrl = "https://images.unsplash.com/photo-1592924357228-91a4daadcfea?auto=format&fit=crop&w=600&q=80",
            content = "Tomato prices have seen a sharp increase of 20% in local APMC markets due to supply shortages caused by unseasonal rains in major growing belts.\n\nTraders expect prices to remain high for the next two weeks until fresh arrivals from neighboring states stabilize the supply. Farmers with ready stock are advised to bring their produce to market gradually to maximize returns."
        )
    )
}