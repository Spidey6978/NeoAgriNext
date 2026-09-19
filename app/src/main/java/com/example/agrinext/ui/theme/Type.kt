package com.example.agrinext.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.agrinext.R

val ProductSans = FontFamily(
    Font(R.font.product_sans_regular, FontWeight.Normal),
    Font(R.font.product_sans_bold, FontWeight.Bold)
)

// Mapping every role ensures components like TopAppBar and Buttons use the font
val Typography = Typography(
    displayLarge = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold, fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold, fontSize = 45.sp),
    displaySmall = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold, fontSize = 36.sp),
    headlineLarge = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleLarge = TextStyle(
        fontFamily = ProductSans,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp // Large enough for the main headings
    ),
    titleMedium = TextStyle(
        fontFamily = ProductSans,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp // Good size for the email ID
    ),
    titleSmall = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Bold, fontSize = 14.sp),
    bodyLarge = TextStyle(
        fontFamily = ProductSans,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp // Increased from 16sp
    ),
    bodyMedium = TextStyle(
        fontFamily = ProductSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp // Increased from 14sp
    ),
    bodySmall = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = ProductSans, fontWeight = FontWeight.Normal, fontSize = 11.sp)
)