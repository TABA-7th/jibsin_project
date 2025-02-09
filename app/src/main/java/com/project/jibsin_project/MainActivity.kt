package com.project.jibsin_project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.project.jibsin_project.Home.HomeScreen
import com.project.jibsin_project.login.LoginScreen
import com.project.jibsin_project.login.SignUpScreen

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
    var isLoggedIn by remember { mutableStateOf(false) }

    if (isLoggedIn) {
        // 로그인 성공 후 홈 화면
        HomeScreen(
            onMonthlyRentClick = { /* 월세 화면 이동 처리 */ },
            onLeaseClick = { /* 전세 화면 이동 처리 */ },
            onChatBotClick= { /* 챗봇 화면 이동 처리 */ }
        )
    } else if (isSignUpScreen) {
        // 회원가입 화면
        SignUpScreen(onSignUpComplete = { isSignUpScreen = false })
    } else {
        // 로그인 화면
        LoginScreen(
            onNavigateToSignUp = { isSignUpScreen = true },
            onLoginSuccess = { isLoggedIn = true } // 로그인 성공 처리
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAppNavigation() {
    AppNavigation()
}
