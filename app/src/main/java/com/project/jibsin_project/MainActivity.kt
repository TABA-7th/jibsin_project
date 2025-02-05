package com.project.jibsin_project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation() {
    var isSignUpScreen by remember { mutableStateOf(false) }

    if (isSignUpScreen) {
        SignUpScreen(onSignUpComplete = { isSignUpScreen = false })
    } else {
        LoginScreen(onNavigateToSignUp = { isSignUpScreen = true })
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAppNavigation() {
    AppNavigation()
}
