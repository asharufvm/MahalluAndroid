package com.nextgenapps.Mahallu.Home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.firestore.auth.User
import com.nextgenapps.Mahallu.DonateNow.DonateNowScreen
import com.nextgenapps.Mahallu.MyAccount.MyAccountScreen
import com.nextgenapps.Mahallu.MyAccount.TransactionDetailsScreen
import com.nextgenapps.Mahallu.Profile.MyProfileScreen
import com.nextgenapps.Mahallu.Settings.SettingsScreen
import com.nextgenapps.Mahallu.MyReceipts.MyReceiptsScreen

import com.nextgenapps.Mahallu.Profile.EditProfileScreen
import com.nextgenapps.Mahallu.utils.ConfigViewModel


import com.nextgenapps.Mahallu.AddReceiptAndVoucher.AddReceiptScreen
import com.nextgenapps.Mahallu.DayBookScreen.DayBookScreen
import com.nextgenapps.Mahallu.AccountStatementScreen.AccountStatementScreen
import com.nextgenapps.Mahallu.DuesScreen.AddMemberDuesScreen
import com.nextgenapps.Mahallu.MembersListScreen.MembersListScreen
import com.nextgenapps.Mahallu.AddMemberScreen.AddMemberScreen
import com.nextgenapps.Mahallu.AddDueScreen.AddDueScreen
import com.nextgenapps.Mahallu.AccountsScreen.AccountsScreen
import com.nextgenapps.Mahallu.CategoriesScreen.CategoriesScreen
import com.nextgenapps.Mahallu.ClassScreen.ClassesScreen
import com.nextgenapps.Mahallu.DashboardScreen.DashboardScreen
import com.nextgenapps.Mahallu.DailyStatusScreen.DailyStatusScreen
import com.nextgenapps.Mahallu.Profile.UserProfile
import com.nextgenapps.Mahallu.Splash.UserSharedViewModel
import com.nextgenapps.Mahallu.UserDetailScreen.UserDetailScreen
import com.nextgenapps.Mahallu.UserDetailScreen.UserDetailScreenViewModel

@Composable
fun HomeScreen(configViewModel: ConfigViewModel) {
    val isAppBlocked by configViewModel.isAppBlocked.collectAsState()

    if (isAppBlocked) {
        // Show blocked screen
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("App is blocked!")
        }
    } else {
        // Normal app content (your existing Scaffold)
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
            Box(
                modifier = Modifier
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
    val userSharedViewModel: UserSharedViewModel = viewModel() // Shared ViewModel

    NavHost(
        navController = navController,
        startDestination = "settings_main",
        modifier = Modifier.fillMaxSize()
    ) {
        // -------- Main settings --------
        composable("settings_main") {
            SettingsScreen(navController = navController)
        }

        // -------- Member Options --------
        composable("my_receipts") {
            MyReceiptsScreen(navController)
        }


        composable(
            route = "AddMemberDuesScreen/{phoneNumber}",
            arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            AddMemberDuesScreen(navController, phoneNumber)
        }


        composable(
            route = "member_account/{phoneNumber}",
            arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            MyAccountScreen(navController, phoneNumber)
        }


        composable(
            route = "member_receipts/{phoneNumber}",
            arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            MyReceiptsScreen(navController, phoneNumber)
        }


        // -------- Admin Data Entry --------
        composable("add_receipt") {
            AddReceiptScreen(navController, false)
        }
        composable("add_voucher") {
            AddReceiptScreen(navController, true)
        }

        // -------- Admin Reports --------
        composable("day_book") {
            DayBookScreen(navController)
        }
        composable("account_statement") {
            AccountStatementScreen(navController)
        }

        // -------- Admin Member Management --------
        composable("members_list") {
            MembersListScreen(navController, userSharedViewModel) // pass ViewModel
        }
        composable("add_member") {
            AddMemberScreen(navController)
        }
        composable("add_due") {
            AddDueScreen(navController)
        }

        composable("user_detail") {
            UserDetailScreen(navController, userSharedViewModel.selectedUser?.id ?: "")
        }

        // -------- Admin Account Management --------
        composable("accounts") {
            AccountsScreen(navController)
        }
        composable("categories") {
            CategoriesScreen(navController)
        }
        composable("class") {
            ClassesScreen(navController)
        }

        // -------- Admin Data Analytics --------
        composable("dashboard") {
            DashboardScreen(navController)
        }
        composable("daily_status") {
            DailyStatusScreen(navController)
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













data class BottomTabItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

