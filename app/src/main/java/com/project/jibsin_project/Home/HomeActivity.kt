package com.project.jibsin_project.Home

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.project.jibsin_project.scan.OnboardingScanActivity
import com.project.jibsin_project.chatbot.ChatBotActivity

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomeScreen(
                onMonthlyRentClick = { startActivity(Intent(this, OnboardingScanActivity::class.java)) },
                onLeaseClick = { startActivity(Intent(this, OnboardingScanActivity::class.java)) },
                onChatBotClick = { startActivity(Intent(this, ChatBotActivity::class.java)) } // AI 챗봇 클릭 처리
            )
        }
    }
}
