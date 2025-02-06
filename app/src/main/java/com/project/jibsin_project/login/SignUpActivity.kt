package com.project.jibsin_project.login

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class SignUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SignUpScreen(onSignUpComplete = {
                finish() // 회원가입 완료 후 LoginActivity로 돌아감
            })
        }
    }
}
