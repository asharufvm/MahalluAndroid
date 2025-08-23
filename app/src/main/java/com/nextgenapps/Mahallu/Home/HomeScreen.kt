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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
    // Main NavControllers per tab
    val profileNavController = rememberNavController()
    val accountNavController = rememberNavController()
    val donateNavController = rememberNavController()
    val settingsNavController = rememberNavController()

    val tabs = listOf(
        BottomTabItem("Profile", Icons.Default.Person, "profile_root"),
        BottomTabItem("Account", Icons.Default.AccountCircle, "account_root"),
        BottomTabItem("Donate", Icons.Filled.CurrencyRupee, "donate_root"),
        BottomTabItem("Settings", Icons.Default.Settings, "settings_root")
    )

    // Remember state per tab to avoid reloading
    val saveableStateHolder = rememberSaveableStateHolder()
    var selectedTab by rememberSaveable { mutableStateOf("profile_root") }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        selected = selectedTab == tab.route,
                        onClick = { selectedTab = tab.route }
                    )
                }
            }
        }
    ) { innerPadding ->
        // Remove innerPadding or handle only bottom
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            tabs.forEach { tab ->
                val isSelected = tab.route == selectedTab
                saveableStateHolder.SaveableStateProvider(tab.route) {
                    if (isSelected) {
                        when (tab.route) {
                            "profile_root" -> ProfileTab(profileNavController)
                            "account_root" -> AccountTab(accountNavController)
                            "donate_root" -> DonateNowScreen()
                            "settings_root" -> SettingsTab(settingsNavController)
                        }
                    }
                }
            }
        }
    }

}

// ----------------- Profile Tab -----------------
@Composable
fun ProfileTab(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "profile_main",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("profile_main") {
            MyProfileScreen(navController = navController)
        }
        composable("edit_profile") {
            EditProfileScreen(
                navController = navController,
                onProfileUpdated = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("profile_updated", true)
                }
            )
        }
    }
}

// ----------------- Account Tab -----------------
@Composable
fun AccountTab(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "account_main",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("account_main") {
            MyAccountScreen(navController = navController)
        }
        composable(
            "transaction_detail/{transactionId}",
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
            TransactionDetailsScreen(
                transactionId = transactionId,
                navController = navController
            )
        }
    }
}

// ----------------- Settings Tab -----------------
@Composable
fun SettingsTab(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "settings_main",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("settings_main") {
            SettingsScreen(navController = navController)
        }
        composable("my_receipts") {
            MyReceiptsScreen(navController = navController)
        }
    }
}









data class BottomTabItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

