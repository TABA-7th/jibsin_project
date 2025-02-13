package com.project.jibsin_project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.firebase.FirebaseApp
import com.project.jibsin_project.Home.HomeScreen
import com.project.jibsin_project.login.LoginScreen
import com.project.jibsin_project.login.SignUpScreen
import com.project.jibsin_project.utils.DocumentUploadManager
import com.project.jibsin_project.utils.FirebaseStorageUtil
import com.project.jibsin_project.utils.FirestoreUtil

class MainActivity : ComponentActivity() {
    private val documentUploadManager = DocumentUploadManager.getInstance()
    private val firebaseStorageUtil = FirebaseStorageUtil()
    private val firestoreUtil = FirestoreUtil()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Firebase 초기화
        FirebaseApp.initializeApp(this)

        setContent {
            MyApp() // MyApp에서 상태바 색상과 UI를 설정
        }
    }
}

@Composable
fun MyApp() {
    val documentUploadManager = remember { DocumentUploadManager.getInstance() }

    // 상태바 설정
    val systemUiController = rememberSystemUiController()
    systemUiController.setSystemBarsColor(
        color = Color.White, // 상태바 색상 흰색
        darkIcons = true // 상태바 아이콘을 어둡게
    )

    // 네비게이션 및 UI 구성
    AppNavigation(documentUploadManager)
}

@Composable
fun AppNavigation(documentUploadManager: DocumentUploadManager) {
    var isSignUpScreen by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(false) }

    if (isLoggedIn) {
        HomeScreen(
            onMonthlyRentClick = { /* 월세 화면 이동 처리 */ },
            onLeaseClick = { /* 전세 화면 이동 처리 */ },
            onChatBotClick= { /* 챗봇 화면 이동 처리 */ }
        )
    } else if (isSignUpScreen) {
        SignUpScreen(onSignUpComplete = { isSignUpScreen = false })
    } else {
        LoginScreen(
            onNavigateToSignUp = { isSignUpScreen = true },
            onLoginSuccess = { isLoggedIn = true }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAppNavigation() {
    AppNavigation(DocumentUploadManager.getInstance())
}
