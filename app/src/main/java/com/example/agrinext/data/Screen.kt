package com.example.agrinext.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

// A sealed class to define the navigation items
sealed class Screen(val route: String, val title: String, val icon: ImageVector, val index: Int) {
    object Home : Screen("home", "Home", Icons.Default.Home, 0)
    object MyFarm : Screen("my_farm", "My Farm", Icons.Default.LocalFlorist, 1)
    object BackendTest : Screen("backend_test", "Test", Icons.Default.Build, 2)
    object Marketplace : Screen("marketplace", "Market", Icons.Default.ShoppingCart, 3)

    // Other routes...
    object AddFarm : Screen("add_farm", "Add Farm", Icons.Default.Add, -1)

    object IndividualFarm : Screen("individual_farm/{farmId}", "Farm Details", Icons.Default.Place, -1) {
        fun createRoute(farmId: Long) = "individual_farm/$farmId"
    }

    object EditFarm : Screen("edit_farm/{farmId}", "Edit Farm", Icons.Default.Edit, -1) {
        fun createRoute(farmId: Long) = "edit_farm/$farmId"
    }

    // Added Missing Route for Individual News
    object IndividualNews : Screen("news/{newsId}", "News", Icons.Default.Description, -1) {
        fun createRoute(newsId: Int) = "news/$newsId"
    }
}