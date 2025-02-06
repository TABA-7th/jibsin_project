package com.project.jibsin_project.Home

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.project.jibsin_project.scan.BuildingRegistryScanActivity

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomeScreen(
                onMonthlyRentClick = { startActivity(Intent(this, BuildingRegistryScanActivity::class.java)) },
                onLeaseClick = { startActivity(Intent(this, BuildingRegistryScanActivity::class.java)) }
            )
        }
    }
}