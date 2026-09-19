package com.example.agrinext.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.example.agrinext.data.FarmRepository
import com.example.agrinext.data.Screen
import com.example.agrinext.ui.components.FarmLand
import com.example.agrinext.ui.components.FarmLandComponent

@Composable
fun MyFarmScreen(
    navController: NavController? = null,
    onAddFarmClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { FarmRepository(context) }

    // 1. Local State for Farm Lands
    val farmLands = remember { mutableStateListOf<FarmLand>() }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Helper to load from Repo
    fun loadFarms() {
        val savedFarms = repository.getFarms()
        farmLands.clear()
        if (savedFarms.isNotEmpty()) {
            farmLands.addAll(savedFarms)
        } else {
            // Default dummy data if empty
            farmLands.add(FarmLand(1, "Example Field", listOf(0.1f to 0.1f, 0.9f to 0.1f, 0.8f to 0.8f, 0.2f to 0.9f), 36.97, -122.03))
        }
    }

    // 2. RELOAD LOGIC: Refresh data whenever screen becomes visible
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            loadFarms()
        }
    }

    // 3. Handle New Data from AddFarmScreen
    if (navController != null) {
        val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle

        LaunchedEffect(savedStateHandle) {
            if (savedStateHandle != null) {
                if (savedStateHandle.contains("newFarmName")) {
                    val newFarmName = savedStateHandle.get<String>("newFarmName")

                    if (newFarmName != null) {
                        val points = savedStateHandle.get<List<Pair<Float, Float>>>("newFarmPoints") ?: emptyList()
                        val lat = savedStateHandle.get<Double>("newFarmLat") ?: 0.0
                        val lng = savedStateHandle.get<Double>("newFarmLng") ?: 0.0

                        val newId = System.currentTimeMillis()

                        val currentList = repository.getFarms().toMutableList()

                        if (currentList.none { it.name == newFarmName }) {
                            val newFarm = FarmLand(newId, newFarmName, points, lat, lng)
                            currentList.add(newFarm)
                            repository.saveFarms(currentList)

                            farmLands.clear()
                            farmLands.addAll(currentList)
                        }

                        savedStateHandle.remove<String>("newFarmName")
                        savedStateHandle.remove<List<Pair<Float, Float>>>("newFarmPoints")
                        savedStateHandle.remove<Double>("newFarmLat")
                        savedStateHandle.remove<Double>("newFarmLng")
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(farmLands) { farm ->
                FarmLandComponent(
                    farmLand = farm,
                    onClick = {
                        navController?.navigate(Screen.IndividualFarm.createRoute(farm.id))
                    },
                    // UPDATED: Re-added Edit Navigation
                    onEditClick = {
                        navController?.navigate(Screen.EditFarm.createRoute(farm.id))
                    }
                )
            }
        }

        FloatingActionButton(
            onClick = onAddFarmClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 120.dp, end = 20.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add New Farm")
        }
    }
}