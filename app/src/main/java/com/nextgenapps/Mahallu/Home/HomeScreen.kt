package com.nextgenapps.Mahallu.Home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.nextgenapps.Mahallu.DonateNow.DonateNowScreen
import com.nextgenapps.Mahallu.MyAccount.MyAccountScreen
import com.nextgenapps.Mahallu.MyAccount.TransactionDetailsScreen
import com.nextgenapps.Mahallu.Profile.MyProfileScreen
import com.nextgenapps.Mahallu.Settings.SettingsScreen
import com.nextgenapps.Mahallu.MyReceipts.MyReceiptsScreen

import com.nextgenapps.Mahallu.Profile.EditProfileScreen


@Composable
fun HomeScreen() {
    val tabNavController = rememberNavController()

    val tabs = listOf(
        BottomTabItem("Profile", Icons.Default.Person, "profile_root"),
        BottomTabItem("Account", Icons.Default.AccountCircle, "account_root"),
        BottomTabItem("Donate", Icons.Filled.CurrencyRupee, "donate_root"),
        BottomTabItem("Settings", Icons.Default.Settings, "settings_root")
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentDestination = tabNavController.currentBackStackEntryAsState().value?.destination?.route
                tabs.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        selected = currentDestination == tab.route,
                        onClick = {
                            tabNavController.navigate(tab.route) {
                                popUpTo(tabNavController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = "profile_root",
            modifier = Modifier.padding(innerPadding)
        ) {
            // Profile
            composable("profile_root") {
                MyProfileScreen(navController = tabNavController)
            }
            composable("edit_profile") {
                EditProfileScreen(
                    navController = tabNavController,
                    onProfileUpdated = {
                        tabNavController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("profile_updated", true)
                    }
                )
            }

            // Account section
            composable("account_root") {
                MyAccountScreen(navController = tabNavController)
            }
            composable(
                "transaction_detail/{transactionId}",
                arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
                TransactionDetailsScreen(
                    transactionId = transactionId,
                    navController = tabNavController // Pass the navController here
                )
            }


            // Donate
            composable("donate_root") {
                DonateNowScreen()
            }

            // Settings section
            navigation(startDestination = "settings", route = "settings_root") {
                composable("settings") {
                    SettingsScreen(navController = tabNavController)
                }
                composable("my_receipts") {
                    MyReceiptsScreen(navController = tabNavController)
                }
            }
        }
    }
}



data class BottomTabItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

