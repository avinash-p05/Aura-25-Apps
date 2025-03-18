package com.techelites.attendacemarkingv1.ui.auth

import com.techelites.attendacemarkingv1.ui.main.MainActivity
import com.techelites.attendacemarkingv1.ui.theme.AuraSecurityTheme


import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthActivity : ComponentActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AuraSecurityTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authState by viewModel.authState.collectAsState()

                    LaunchedEffect(authState.isLoggedIn) {
                        if (authState.isLoggedIn) {
                            navigateToMain()
                        }
                    }

                    LoginScreen(
                        username = authState.username,
                        password = authState.password,
                        isLoading = authState.isLoading,
                        errorMessage = authState.errorMessage,
                        onUsernameChange = viewModel::onUsernameChange,
                        onPasswordChange = viewModel::onPasswordChange,
                        onLoginClick = viewModel::login,
                        onForgotPasswordClick = {
                            // TODO: Implement forgot password
                        }
                    )
                }
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}