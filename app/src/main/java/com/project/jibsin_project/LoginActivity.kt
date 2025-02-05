package com.project.jibsin_project

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginScreen(onNavigateToSignUp = {
                val intent = Intent(this, SignUpActivity::class.java)
                startActivity(intent)
            })
        }
    }
}
