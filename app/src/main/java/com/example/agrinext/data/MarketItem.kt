package com.example.agrinext.data

import androidx.compose.ui.graphics.Color

data class MarketItem(
    val id: Int,
    val name: String,
    val pricePerUnit: String,
    val quantityAvailable: String,
    val sellerName: String,
    val location: String,
    val imageUrl: String? = null,
    val category: String,
    val categoryColor: Color
)

object MarketRepository {

    val items = listOf(

        // 🌱 Fertilizers
        MarketItem(
            id = 1,
            name = "Urea (46% N)",
            pricePerUnit = "₹266/bag (45 kg)",
            quantityAvailable = "120 bags",
            sellerName = "Mahindra Agro Store",
            location = "Nashik, MH",
            category = "Fertilizer",
            categoryColor = Color(0xFF2E7D32) // Dark Green
        ),
        MarketItem(
            id = 2,
            name = "DAP (18-46-0)",
            pricePerUnit = "₹1,350/bag",
            quantityAvailable = "80 bags",
            sellerName = "Krushi Seva Kendra",
            location = "Pune, MH",
            category = "Fertilizer",
            categoryColor = Color(0xFF2E7D32)
        ),

        // 🧪 Pesticides
        MarketItem(
            id = 3,
            name = "Chlorpyrifos 20% EC",
            pricePerUnit = "₹620/litre",
            quantityAvailable = "60 litres",
            sellerName = "AgroVet Solutions",
            location = "Kolhapur, MH",
            category = "Pesticide",
            categoryColor = Color(0xFFC62828) // Red
        ),

        // 🍄 Fungicides
        MarketItem(
            id = 4,
            name = "Mancozeb 75% WP",
            pricePerUnit = "₹280/kg",
            quantityAvailable = "90 kg",
            sellerName = "CropCare Agro",
            location = "Satara, MH",
            category = "Fungicide",
            categoryColor = Color(0xFF6A1B9A) // Purple
        ),

        // 🐛 Insecticides
        MarketItem(
            id = 5,
            name = "Imidacloprid 17.8% SL",
            pricePerUnit = "₹540/250 ml",
            quantityAvailable = "150 bottles",
            sellerName = "Bharat Agri Store",
            location = "Ahmednagar, MH",
            category = "Insecticide",
            categoryColor = Color(0xFFEF6C00) // Orange
        ),

        // 🌿 Herbicides
        MarketItem(
            id = 6,
            name = "Glyphosate 41% SL",
            pricePerUnit = "₹420/litre",
            quantityAvailable = "70 litres",
            sellerName = "FarmGrow Solutions",
            location = "Solapur, MH",
            category = "Herbicide",
            categoryColor = Color(0xFF33691E) // Olive Green
        ),

        // 🧬 Micronutrients
        MarketItem(
            id = 7,
            name = "Zinc Sulphate (21%)",
            pricePerUnit = "₹190/kg",
            quantityAvailable = "110 kg",
            sellerName = "Soil Health Agro",
            location = "Latur, MH",
            category = "Micronutrient",
            categoryColor = Color(0xFF0277BD) // Blue
        ),

        // 🌾 Bio-fertilizers
        MarketItem(
            id = 8,
            name = "Rhizobium Bio Fertilizer",
            pricePerUnit = "₹120/packet",
            quantityAvailable = "200 packets",
            sellerName = "Organic Roots",
            location = "Nagpur, MH",
            category = "Bio-fertilizer",
            categoryColor = Color(0xFF558B2F) // Light Green
        ),

        // 🌱 Seeds
        MarketItem(
            id = 9,
            name = "Hybrid Cotton Seeds (BG-II)",
            pricePerUnit = "₹850/packet",
            quantityAvailable = "65 packets",
            sellerName = "Tata Rallis Seeds",
            location = "Yavatmal, MH",
            category = "Seed",
            categoryColor = Color(0xFF5D4037) // Brown
        ),
        MarketItem(
            id = 10,
            name = "Paddy Seeds (IR-64)",
            pricePerUnit = "₹48/kg",
            quantityAvailable = "500 kg",
            sellerName = "State Seed Corporation",
            location = "Raigad, MH",
            category = "Seed",
            categoryColor = Color(0xFF5D4037)
        )
    )
}