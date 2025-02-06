package com.project.jibsin_project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.systemuicontroller.rememberSystemUiController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp() // MyApp에서 상태바 색상과 UI를 설정
        }
    }
}

@Composable
fun MyApp() {
    // 상태바 설정
    val systemUiController = rememberSystemUiController()
    systemUiController.setSystemBarsColor(
        color = Color.White, // 상태바 색상 흰색
        darkIcons = true // 상태바 아이콘을 어둡게
    )

    // 네비게이션 및 UI 구성
    AppNavigation()
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
