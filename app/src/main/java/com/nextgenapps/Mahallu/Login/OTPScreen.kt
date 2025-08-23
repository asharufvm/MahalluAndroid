package com.nextgenapps.Mahallu.Login

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController

// Add to top if not already
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

class OTPViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Expose loading state to the UI
    var isVerifying by mutableStateOf(false)
        private set

    fun verifyOTP(
        verificationId: String,
        otp: String,
        mobileNumber: String,
        context: Context,
        navController: NavHostController
    ) {
        isVerifying = true
        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val fullPhoneNumber = "+91$mobileNumber"
                    firestore.collection("users")
                        .whereEqualTo("phoneNumber", fullPhoneNumber)
                        .get()
                        .addOnSuccessListener { result ->
                            isVerifying = false
                            if (!result.isEmpty) {
                                /*val userDoc = result.documents.first()
                                val organizationId = userDoc.getString("organizationId") ?: ""
                                val role = userDoc.getString("role") ?: ""
                                val phoneNumber = userDoc.getString("phoneNumber") ?: ""

                                val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                                prefs.edit()
                                    .putString("organizationId", organizationId)
                                    .putString("role", role)
                                    .putString("phoneNumber", phoneNumber)
                                    .apply()*/

                                navController.navigate("home") {
                                    popUpTo(0) { inclusive = true }
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Mobile number not registered with Mahallu, please contact Mahallu Admin",
                                    Toast.LENGTH_LONG
                                ).show()
                                auth.signOut()
                                navController.navigate("login") {
                                    popUpTo("OTPScreen") { inclusive = true }
                                }
                            }
                        }
                        .addOnFailureListener {
                            isVerifying = false
                            Toast.makeText(
                                context,
                                "Error checking user: ${it.localizedMessage}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    isVerifying = false
                    Toast.makeText(context, "Invalid OTP", Toast.LENGTH_SHORT).show()
                }
            }
    }
}


/// --- OTPScreen.kt ---
@Composable
fun OTPScreen(
    navController: NavHostController,
    verificationId: String,
    mobileNumber: String,
    viewModel: OTPViewModel = remember { OTPViewModel() }
) {
    val context = LocalContext.current
    var otp by remember { mutableStateOf("") }
    var isResending by remember { mutableStateOf(false) }
    var resendAvailable by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(30) }

    // Countdown timer for resend
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        resendAvailable = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background), // dark mode safe background
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Enter OTP sent to +91$mobileNumber",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // ✅ OTP input field
            OutlinedTextField(
                value = otp,
                onValueChange = { input ->
                    if (input.length <= 6 && input.all { it.isDigit() }) otp = input
                },
                label = { Text("OTP") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (viewModel.isVerifying) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                Button(
                    onClick = {
                        viewModel.verifyOTP(
                            verificationId,
                            otp,
                            mobileNumber,
                            context,
                            navController
                        )
                    },
                    enabled = otp.length == 6,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "Verify OTP",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!resendAvailable) {
                Text(
                    text = "Resend available in ${String.format("%02d:%02d", countdown / 60, countdown % 60)}",
                    color = MaterialTheme.colorScheme.onBackground
                )
            } else {
                if (isResending) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    TextButton(onClick = { /* Handle resend logic */ }) {
                        Text(
                            "Resend OTP",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}


