package com.nextgenapps.Mahallu.Settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
//import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.filled.Delete

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Logout
import com.nextgenapps.Mahallu.MainActivity
import com.nextgenapps.Mahallu.Profile.SessionManager

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        // My Receipt Option
        ListItem(
            headlineContent = { Text("My Receipt") },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "My Receipt"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    navController.navigate("my_receipts")
                }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Sign Out Option
        ListItem(
            headlineContent = { Text("Sign Out") },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Sign Out"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    logoutAndRestart(context)
                }
        )
    }
}

fun logoutAndRestart(context: Context) {
    FirebaseAuth.getInstance().signOut()
    clearUserSession(context)

    val intent = Intent(context, MainActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    context.startActivity(intent)
}

fun clearUserSession(context: Context) {
    SessionManager.organizationId = ""
    SessionManager.role = ""
}


