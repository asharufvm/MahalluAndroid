package com.nextgenapps.Mahallu.Login

import android.app.Activity
import android.widget.Toast
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.Firebase

// Add to top if not already
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.functions.functions
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

class EnterMobileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val functions = Firebase.functions("asia-south1")

    var mobileNumber by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun onMobileNumberChange(input: String) {
        if (input.length <= 10 && input.all { it.isDigit() }) {
            mobileNumber = input
        }
    }

    fun checkAndSendOTP(
        activity: Activity,
        onOTPSent: (verificationId: String, phoneNumber: String) -> Unit
    ) {
        val fullPhoneNumber = "+91$mobileNumber"
        isLoading = true
        errorMessage = null

        // Step 1: checkPhoneNumber
        functions.getHttpsCallable("checkPhoneNumber")
            .call(hashMapOf("phoneNumber" to fullPhoneNumber))
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    isLoading = false
                    errorMessage = task.exception?.localizedMessage ?: "Error checking number."
                    return@addOnCompleteListener
                }

                val data = task.result?.data as? Map<*, *>
                val allowed = data?.get("allowed") as? Boolean ?: false
                val serverMessage = data?.get("message") as? String

                if (!allowed) {
                    // ❌ Not allowed → show server message
                    isLoading = false
                    errorMessage = serverMessage
                        ?: "Mobile Number not registered. Please contact Mahallu Admin."
                } else {
                    // Step 2: sendOTPWithRateLimit
                    functions.getHttpsCallable("sendOTPWithRateLimit")
                        .call(hashMapOf("phoneNumber" to fullPhoneNumber))
                        .addOnCompleteListener { otpTask ->
                            if (!otpTask.isSuccessful) {
                                isLoading = false
                                errorMessage =
                                    otpTask.exception?.localizedMessage ?: "Error sending OTP"
                                return@addOnCompleteListener
                            }

                            val otpData = otpTask.result?.data as? Map<*, *>
                            val otpAllowed = otpData?.get("allowed") as? Boolean ?: false
                            val otpMessage = otpData?.get("message") as? String

                            if (!otpAllowed) {
                                // ❌ Not allowed → show server message
                                isLoading = false
                                errorMessage = otpMessage ?: "OTP request not allowed."
                            } else {
                                // ✅ Send OTP via Firebase PhoneAuth
                                val options = PhoneAuthOptions.newBuilder(auth)
                                    .setPhoneNumber(fullPhoneNumber)
                                    .setTimeout(60L, TimeUnit.SECONDS)
                                    .setActivity(activity)
                                    .setCallbacks(object :
                                        PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                            isLoading = false
                                        }

                                        override fun onVerificationFailed(e: FirebaseException) {
                                            isLoading = false
                                            errorMessage = "Verification failed: ${e.message}"
                                        }

                                        override fun onCodeSent(
                                            verificationId: String,
                                            token: PhoneAuthProvider.ForceResendingToken
                                        ) {
                                            isLoading = false
                                            onOTPSent(verificationId, mobileNumber)
                                        }
                                    })
                                    .build()

                                PhoneAuthProvider.verifyPhoneNumber(options)
                            }
                        }
                }
            }
    }
}





@Composable
fun EnterMobileScreen(
    navController: NavHostController,
    viewModel: EnterMobileViewModel = viewModel()
) {
    val context = LocalContext.current
    val isValid = viewModel.mobileNumber.length == 10

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Enter Mobile Number",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = viewModel.mobileNumber,
                onValueChange = { viewModel.onMobileNumberChange(it) },
                label = { Text("Mobile Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ✅ Center-aligned error message
            viewModel.errorMessage?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (viewModel.isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        viewModel.checkAndSendOTP(
                            activity = context as Activity,
                            onOTPSent = { verificationId, phoneNumber ->
                                navController.navigate("otp_screen/$verificationId/$phoneNumber")
                            }
                        )
                    },
                    enabled = isValid
                ) {
                    Text(
                        text = "Continue",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }
    }
}


