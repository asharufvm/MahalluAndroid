package com.nextgenapps.Mahallu

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.BuildConfig
import com.google.firebase.Firebase
import com.nextgenapps.Mahallu.ui.theme.MahalluTheme
import com.nextgenapps.Mahallu.Splash.AppNavHost
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.nextgenapps.Mahallu.utils.ConfigViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        enableEdgeToEdge()
        setContent {
            val configViewModel: ConfigViewModel = viewModel()

            // Collect states from ViewModel
            val isAppBlocked by configViewModel.isAppBlocked.collectAsState()
            val isVersionBlocked by configViewModel.isVersionBlocked.collectAsState()

            MahalluTheme {
                when {
                    isAppBlocked -> {
                        // App is fully blocked
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "App is blocked!",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    isVersionBlocked -> {
                        // User must update app
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "A new version is required to continue.",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = {
                                    // Open Play Store link
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        //Uri.parse("market://details?id=${BuildConfig.APPLICATION_ID}")
                                    )
                                    startActivity(intent)
                                }) {
                                    Text("Update Now")
                                }
                            }
                        }
                    }

                    else -> {
                        // Show normal app flow
                        AppNavHost(configViewModel)
                    }
                }
            }
        }

    }

    // Called when app comes to foreground
    override fun onStart() {
        super.onStart()
        val configViewModel: ConfigViewModel by viewModels()
        configViewModel.loadConfig() // refresh Remote Config
    }

}

