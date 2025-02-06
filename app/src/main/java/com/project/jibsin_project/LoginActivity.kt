package com.project.jibsin_project

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginScreen(
                onNavigateToSignUp = {
                    val intent = Intent(this, SignUpActivity::class.java)
                    startActivity(intent)
                },
                onLoginSuccess = {
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish() // 로그인 화면 종료
                }
            )
        }
    }
}
