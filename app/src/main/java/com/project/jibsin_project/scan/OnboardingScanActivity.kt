package com.project.jibsin_project.scan

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.pager.*
import com.project.jibsin_project.R
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class OnboardingScanActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OnboardingScanScreen()
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun OnboardingScanScreen() {
    val pagerState = rememberPagerState()
    val pages = listOf(
        OnboardingPage("건축물대장", "건축물대장을 스캔하여 분석하세요.", R.drawable.ic_scan),
        OnboardingPage("등기부등본", "등기부등본을 스캔하여 분석하세요.", R.drawable.ic_scan),
        OnboardingPage("계약서", "계약서를 스캔하여 분석하세요.", R.drawable.ic_scan)
    )
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.White, // 배경 색상 설정
        bottomBar = {
            BottomAppBar(
                containerColor = Color.White,
                content = {
                    Spacer(modifier = Modifier.weight(1f))
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            HorizontalPager(
                state = pagerState,
                count = pages.size,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(page = pages[page])
            }

            // 인디케이터 및 버튼
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 페이지 인디케이터
                HorizontalPagerIndicator(
                    pagerState = pagerState,
                    activeColor = Color(0xFF253F5A), // 활성화된 색상
                    inactiveColor = Color(0xFFBDBDBD), // 비활성화된 색상
                    indicatorWidth = 8.dp,
                    indicatorHeight = 8.dp,
                    spacing = 8.dp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 버튼 스타일 결정
                if (pagerState.currentPage < pages.size - 1) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(2.dp, Color(0xFF253F5A)),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("다음", color = Color.Black, fontSize = 16.sp)
                    }
                } else {
                    Button(
                        onClick = {
                            // 완료 동작 처리
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF253F5A)),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("완료", color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    val context = LocalContext.current

    // 카메라 권한 확인
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
            context as ComponentActivity,
            arrayOf(Manifest.permission.CAMERA),
            100
        )
    }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            // 카메라로 촬영한 이미지 처리
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            // 갤러리에서 선택한 이미지 처리
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Image(
            painter = painterResource(id = page.imageRes),
            contentDescription = null,
            modifier = Modifier
                .size(400.dp)
                .padding(16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(page.title, fontSize = 22.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            page.description,
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        // 카메라 및 갤러리 버튼
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { takePictureLauncher.launch(null) },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(2.dp, Color(0xFF253F5A))
            ) {
                Icon(Icons.Filled.Camera, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("카메라로 촬영", color = Color.Black, fontSize = 16.sp)
            }

            OutlinedButton(
                onClick = { pickImageLauncher.launch("image/*") },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(2.dp, Color(0xFF253F5A))
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("갤러리에서 선택", color = Color.Black, fontSize = 16.sp)
            }
        }
    }
}

data class OnboardingPage(val title: String, val description: String, val imageRes: Int)

@Preview(showBackground = true)
@Composable
fun PreviewOnboardingScanScreen() {
    OnboardingScanScreen()
}
