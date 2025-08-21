package com.nextgenapps.Mahallu.Splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight


@Composable
fun SplashScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()

    // Correct use of LaunchedEffect with delay
    LaunchedEffect(Unit) {
        delay(2000) // Delay for 2 seconds

        if (auth.currentUser != null) {
            navController.navigate("home") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    // Splash UI
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "🕌 Mahallu App",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color(0xFF2E7D32), // A nice green shade
                    fontSize = 32.sp,           // Custom font size
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            CircularProgressIndicator(
                color = Color(0xFF2E7D32) // Green shade
            )
        }
    }
}






//@Preview(showBackground = true)
//@Composable
//fun SplashScreenPreview() {
//    MaterialTheme {
//        SplashScreen()
//    }
//}