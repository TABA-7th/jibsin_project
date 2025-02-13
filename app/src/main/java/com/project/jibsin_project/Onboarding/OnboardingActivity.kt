package com.project.jibsin_project.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.accompanist.pager.*
import com.project.jibsin_project.R
import com.project.jibsin_project.login.LoginActivity
import kotlinx.coroutines.launch

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 📌 온보딩 완료 여부 확인 후 로그인 화면으로 이동 (주석 해제 필요함)
        //val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        //if (sharedPreferences.getBoolean("isOnboardingCompleted", false)) {
        //    startActivity(Intent(this, LoginActivity::class.java))
        //    finish()
        //    return
        //}

        setContent {
            OnboardingScreen()
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun OnboardingScreen() {
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    val pages = listOf(
        OnboardingPage("서류 업로드", "계약서를 스캔 또는 업로드하세요.", R.raw.scan_animation),
        OnboardingPage("AI 분석", "업로드한 서류를 AI가 분석하여 주의사항을 알려드립니다.", R.raw.analysis_animation),
        OnboardingPage("계약 검토", "중요 계약 정보를 검토하고 요약해드립니다.", R.raw.contract_animation)
    )

    Scaffold(containerColor = androidx.compose.ui.graphics.Color.White) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 🚀 "건너뛰기" 버튼을 우측 상단에 배치
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        sharedPreferences.edit().putBoolean("isOnboardingCompleted", true).apply()
                        val intent = Intent(context, LoginActivity::class.java)
                        context.startActivity(intent)
                        (context as ComponentActivity).finish()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF253F5A)),
                    modifier = Modifier.align(Alignment.TopEnd) // 우측 상단 정렬
                ) {
                    Text("건너뛰기")
                }
            }

            Spacer(modifier = Modifier.height(16.dp)) // 버튼과 콘텐츠 간격 추가

            HorizontalPager(
                state = pagerState,
                count = pages.size,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(page = pages[page])
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalPagerIndicator(
                    pagerState = pagerState,
                    activeColor = androidx.compose.ui.graphics.Color(0xFF253F5A),
                    inactiveColor = androidx.compose.ui.graphics.Color(0xFFBDBDBD),
                    indicatorWidth = 8.dp,
                    indicatorHeight = 8.dp,
                    spacing = 8.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (pagerState.currentPage < pages.size - 1) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF253F5A)), // 남색 적용
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("다음", color = Color(0xFF253F5A))
                    }
                } else {
                    Button(
                        onClick = {
                            // 온보딩 완료 상태 저장(주석 해제 필요함)
                            //sharedPreferences.edit().putBoolean("isOnboardingCompleted", true).apply()

                            // 로그인 화면으로 이동
                            val intent = Intent(context, LoginActivity::class.java)
                            context.startActivity(intent)
                            (context as ComponentActivity).finish()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF253F5A)), // 버튼 배경 남색
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("시작하기")
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(page.animationRes))
    val progress by animateLottieCompositionAsState(composition)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center // 중앙 정렬 유지
    ) {
        // contract_animation이면 추가 간격
        if (page.animationRes == R.raw.contract_animation) {
            Spacer(modifier = Modifier.height(40.dp))
        }

        // 애니메이션 크기 조정
        val animationSize = if (page.animationRes == R.raw.contract_animation) 300.dp else 400.dp

        // 애니메이션
        LottieAnimation(
            composition = composition,
            progress = progress,
            modifier = Modifier.size(animationSize)
        )

        Spacer(
            modifier = Modifier.height(
                if (page.animationRes == R.raw.contract_animation) 52.dp else 2.dp
            )
        )

        // 텍스트
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = page.title,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = page.description,
                fontSize = 14.sp
            )
        }
    }
}

data class OnboardingPage(
    val title: String,
    val description: String,
    val animationRes: Int
)

@Preview(showBackground = true)
@Composable
fun PreviewOnboardingScreen() {
    OnboardingScreen()
}
