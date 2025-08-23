package com.nextgenapps.Mahallu.Splash

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import com.nextgenapps.Mahallu.Login.EnterMobileScreen
import com.nextgenapps.Mahallu.Home.HomeScreen

import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nextgenapps.Mahallu.Login.OTPScreen
import com.nextgenapps.Mahallu.MyReceipts.MyReceiptsScreen

import com.nextgenapps.Mahallu.DonateNow.DonateNowScreen
import com.nextgenapps.Mahallu.MyAccount.MyAccountScreen
//import com.nextgenapps.Mahallu.Profile.EditProfileScreen
import com.nextgenapps.Mahallu.Profile.EditProfileViewModel
import com.nextgenapps.Mahallu.Profile.MyProfileScreen
import com.nextgenapps.Mahallu.utils.ConfigViewModel

@Composable
fun AppNavHost(configViewModel: ConfigViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
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

    }
}

