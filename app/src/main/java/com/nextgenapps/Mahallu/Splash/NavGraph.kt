package com.nextgenapps.Mahallu.Splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import com.nextgenapps.Mahallu.Login.EnterMobileScreen
import com.nextgenapps.Mahallu.Home.HomeScreen

import androidx.navigation.*
import com.google.gson.Gson
import com.nextgenapps.Mahallu.Login.OTPScreen
import com.nextgenapps.Mahallu.MyReceipts.MyReceiptsScreen

import com.nextgenapps.Mahallu.utils.ConfigViewModel


import com.nextgenapps.Mahallu.AddReceiptAndVoucher.AddReceiptScreen
import com.nextgenapps.Mahallu.DayBookScreen.DayBookScreen
import com.nextgenapps.Mahallu.AccountStatementScreen.AccountStatementScreen
import com.nextgenapps.Mahallu.MembersListScreen.MembersListScreen
import com.nextgenapps.Mahallu.AddMemberScreen.AddMemberScreen
import com.nextgenapps.Mahallu.AddDueScreen.AddDueScreen
import com.nextgenapps.Mahallu.AccountsScreen.AccountsScreen
import com.nextgenapps.Mahallu.CategoriesScreen.CategoriesScreen
import com.nextgenapps.Mahallu.ClassScreen.ClassesScreen
import com.nextgenapps.Mahallu.DashboardScreen.DashboardScreen
import com.nextgenapps.Mahallu.DailyStatusScreen.DailyStatusScreen
import com.nextgenapps.Mahallu.DuesScreen.AddMemberDuesScreen
import com.nextgenapps.Mahallu.MembersListScreen.UserProfile
import com.nextgenapps.Mahallu.MyAccount.MyAccountScreen
import com.nextgenapps.Mahallu.UserDetailScreen.UserDetailScreen
import com.nextgenapps.Mahallu.UserDetailScreen.UserDetailScreenViewModel



class UserSharedViewModel(
) : ViewModel() {
    var selectedUser by mutableStateOf<UserProfile?>(null)
}

@Composable
fun AppNavHost(configViewModel: ConfigViewModel) {
    val navController = rememberNavController()
    val userSharedViewModel: UserSharedViewModel = viewModel() // Shared ViewModel

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        // ================= Auth Flow =================
        composable("splash") {
            SplashScreen(navController)
        }

        composable("login") {
            EnterMobileScreen(navController)
        }

        composable(
            "otp_screen/{verificationId}/{mobileNumber}",
            arguments = listOf(
                navArgument("verificationId") { type = NavType.StringType },
                navArgument("mobileNumber") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val verificationId = backStackEntry.arguments?.getString("verificationId") ?: ""
            val mobileNumber = backStackEntry.arguments?.getString("mobileNumber") ?: ""
            OTPScreen(
                verificationId = verificationId,
                mobileNumber = mobileNumber,
                navController = navController
            )
        }

        composable("home") {
            HomeScreen(configViewModel)
        }

        // ================= Member Screens =================
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

        // ================= Admin Data Entry =================
        composable("add_receipt") {
            AddReceiptScreen(navController, false)
        }
        composable("add_voucher") {
            AddReceiptScreen(navController, true)
        }

        // ================= Admin Reports =================
        composable("day_book") {
            DayBookScreen(navController)
        }
        composable("account_statement") {
            AccountStatementScreen(navController)
        }

        // ================= Admin Member Management =================
        composable("members_list") {
            MembersListScreen(navController, userSharedViewModel)
        }

        composable("user_detail") {
            UserDetailScreen(navController, userSharedViewModel.selectedUser?.id ?: "")
        }

        composable("add_member") {
            AddMemberScreen(navController)
        }
        composable("add_due") {
            AddDueScreen(navController)
        }

        // ================= Admin Account Management =================
        composable("accounts") {
            AccountsScreen(navController)
        }
        composable("categories") {
            CategoriesScreen(navController)
        }
        composable("class") {
            ClassesScreen(navController)
        }

        // ================= Admin Data Analytics =================
        composable("dashboard") {
            DashboardScreen(navController)
        }
        composable("daily_status") {
            DailyStatusScreen(navController)
        }
    }
}




