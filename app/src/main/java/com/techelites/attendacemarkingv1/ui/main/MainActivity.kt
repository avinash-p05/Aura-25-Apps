package com.techelites.attendacemarkingv1.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.techelites.attendacemarkingv1.ui.auth.AuthActivity
import com.techelites.attendacemarkingv1.ui.main.scanner.ScannerActivity
import com.techelites.attendacemarkingv1.ui.theme.AuraSecurityTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up navigation event collection
        lifecycleScope.launch {
            viewModel.navigationEvents.collectLatest { event ->
                Log.d(TAG, "Navigation event received: $event")
                when (event) {
                    is MainNavigationEvent.NavigateToScanner -> {
                        val intent = Intent(this@MainActivity, ScannerActivity::class.java).apply {
                            putExtra("checkpoint", event.checkpoint)
                            putExtra("action", event.action)
                        }
                        Log.d(TAG, "Starting ScannerActivity with checkpoint=${event.checkpoint}, action=${event.action}")
                        startActivity(intent)
                    }
                    is MainNavigationEvent.NavigateToLogin -> {
                        startActivity(Intent(this@MainActivity, AuthActivity::class.java))
                        finish()
                    }
                }
            }
        }

        setContent {
            AuraSecurityTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    MainNavHost(
                        navController = navController,
                        viewModel = viewModel,
                        onNavigateToScanner = { checkpoint, action ->
                            // For backward compatibility - prefer to use ViewModel events instead
                            Log.d(TAG, "onNavigateToScanner called directly with checkpoint=$checkpoint, action=$action")
                            val intent = Intent(this, ScannerActivity::class.java).apply {
                                putExtra("checkpoint", checkpoint)
                                putExtra("action", action)
                            }
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}