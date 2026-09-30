package com.example.agrinext

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.agrinext.data.Screen
import com.example.agrinext.ui.components.CustomBottomBar
import com.example.agrinext.ui.screens.*
import com.example.agrinext.ui.theme.AgriNextTheme
import com.example.agrinext.util.LanguageManager
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the multi-language system
        LanguageManager.initialize(this)

        setContent {
            var isDarkMode by remember { mutableStateOf(false) }

            AgriNextTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    val focusManager = LocalFocusManager.current

                    // Initial destination logic
                    val startDest = if (auth.currentUser != null) "home" else "auth"

                    // Define which screens show the persistent header and bottom bar
                    val showChrome = currentRoute == "home" ||
                            currentRoute == Screen.MyFarm.route ||
                            currentRoute == Screen.BackendTest.route ||
                            currentRoute == Screen.Marketplace.route

                    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    val headerHeight = 64.dp

                    // Padding for the main content area
                    val topPadding = if (showChrome) statusBarHeight + headerHeight else 0.dp

                    val user = auth.currentUser
                    val greetingName = remember(user?.displayName) {
                        val name = user?.displayName ?: ""
                        if (name.isNotBlank()) name.split(" ").first()
                        else user?.email?.substringBefore("@") ?: "Farmer"
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = startDest,
                            modifier = Modifier.padding(top = topPadding)
                        ) {
                            composable("auth") {
                                AuthScreen(onAuthSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                })
                            }

                            composable("home") {
                                HomeScreen(
                                    onWeatherClick = { navController.navigate("weather") },
                                    onNewsClick = { newsId ->
                                        navController.navigate(Screen.IndividualNews.createRoute(newsId))
                                    },
                                    onDiagnosticClick = { navController.navigate(Screen.DiseaseDiagnostic.route) },
                                    onAdvisoryClick = { navController.navigate(Screen.AdvisoryDashboard.route) }
                                )
                            }

                            composable(Screen.MyFarm.route) {
                                MyFarmScreen(
                                    navController = navController,
                                    onAddFarmClick = { navController.navigate(Screen.AddFarm.route) }
                                )
                            }

                            composable(Screen.AddFarm.route) {
                                AddFarmScreen(
                                    onBack = { navController.popBackStack() },
                                    onFarmSaved = { name, points, lat, lng ->
                                        // Pass data back to MyFarm screen via savedStateHandle
                                        navController.previousBackStackEntry?.savedStateHandle?.set("newFarmName", name)
                                        navController.previousBackStackEntry?.savedStateHandle?.set("newFarmPoints", points)
                                        navController.previousBackStackEntry?.savedStateHandle?.set("newFarmLat", lat)
                                        navController.previousBackStackEntry?.savedStateHandle?.set("newFarmLng", lng)
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable(
                                route = Screen.IndividualFarm.route,
                                arguments = listOf(navArgument("farmId") { type = NavType.LongType })
                            ) { backStackEntry ->
                                val farmId = backStackEntry.arguments?.getLong("farmId") ?: 0L
                                IndividualFarmScreen(
                                    farmId = farmId,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = Screen.EditFarm.route,
                                arguments = listOf(navArgument("farmId") { type = NavType.LongType })
                            ) { backStackEntry ->
                                val farmId = backStackEntry.arguments?.getLong("farmId") ?: 0L
                                EditFarmScreen(
                                    farmId = farmId,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.Marketplace.route) {
                                MarketplaceScreen()
                            }

                            composable(Screen.BackendTest.route) {
                                BackendTestScreen(onBack = { navController.popBackStack() })
                            }

                            composable(
                                route = Screen.IndividualNews.route,
                                arguments = listOf(navArgument("newsId") { type = NavType.IntType })
                            ) { backStackEntry ->
                                val newsId = backStackEntry.arguments?.getInt("newsId") ?: 0
                                IndividualNewsScreen(
                                    newsId = newsId,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("profile") {
                                ProfileScreen(
                                    isDarkMode = isDarkMode,
                                    onThemeToggle = { isDarkMode = it },
                                    onBack = { navController.popBackStack() },
                                    onLogout = {
                                        navController.navigate("auth") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable("weather") {
                                WeatherScreen(onBack = { navController.popBackStack() })
                            }

                            // Inside your NavHost block in MainActivity.kt, add:

                            composable(Screen.DiseaseDiagnostic.route) {
                                DiseaseDiagnosticScreen(onBack = { navController.popBackStack() })
                            }

                            composable(Screen.AdvisoryDashboard.route) {
                                AdvisoryDashboardScreen(onBack = { navController.popBackStack() })
                            }
                        }

                        // --- GLOBAL HEADER OVERLAY ---
                        if (showChrome) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .padding(top = statusBarHeight + 4.dp, start = 20.dp, end = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val titleText = when (currentRoute) {
                                    Screen.MyFarm.route -> LanguageManager.get("My Farm")
                                    Screen.Marketplace.route -> LanguageManager.get("Marketplace")
                                    Screen.BackendTest.route -> LanguageManager.get("Test")
                                    "home" -> "${LanguageManager.get("Hello")}, $greetingName!"
                                    else -> ""
                                }

                                Text(
                                    text = titleText,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 24.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                )

                                IconButton(
                                    onClick = { navController.navigate("profile") },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "Profile",
                                        modifier = Modifier.size(36.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // --- GLOBAL CUSTOM BOTTOM BAR ---
                        if (showChrome) {
                            CustomBottomBar(
                                selectedIndex = when (currentRoute) {
                                    "home" -> 0
                                    Screen.MyFarm.route -> 1
                                    Screen.BackendTest.route -> 2
                                    Screen.Marketplace.route -> 3
                                    else -> 0
                                },
                                onItemSelected = { index ->
                                    val target = when(index) {
                                        0 -> "home"
                                        1 -> Screen.MyFarm.route
                                        2 -> Screen.BackendTest.route
                                        3 -> Screen.Marketplace.route
                                        else -> null
                                    }
                                    target?.let {
                                        if (currentRoute != it) {
                                            navController.navigate(it) {
                                                popUpTo("home")
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                },
                                focusManager = focusManager,
                                onChatBoxClick = { /* Handled by internal CustomBottomBar logic */ },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }
                }
            }
        }
    }
}