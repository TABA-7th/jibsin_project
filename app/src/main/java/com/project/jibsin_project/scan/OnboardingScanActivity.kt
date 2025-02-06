package com.project.jibsin_project.scan

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
    val pages = listOf("건축물대장", "등기부등본", "계약서")
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            BottomAppBar(
                containerColor = Color.White,
                content = {
                    Spacer(modifier = Modifier.weight(1f))
                    if (pagerState.currentPage < pages.size - 1) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF253F5A))
                        ) {
                            Text("다음", color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            )
        }
    ) { padding ->
        HorizontalPager(state = pagerState, count = pages.size, modifier = Modifier.padding(padding)) { page ->
            ScanStep(title = "${pages[page]}을(를) 스캔하세요")
        }
    }
}

@Composable
fun ScanStep(title: String) {
    val context = LocalContext.current

    // 런타임 권한 요청을 위한 상태 변수
    var showPermissionDialog by remember { mutableStateOf(false) }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            // TODO: 촬영된 사진 처리
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            // TODO: 선택된 이미지 처리
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 20.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                painter = painterResource(id = R.drawable.ic_scan),
                contentDescription = null,
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            // 카메라 권한이 이미 허용된 경우
                            takePictureLauncher.launch(null)
                        } else {
                            // 권한 요청
                            showPermissionDialog = true
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF253F5A))
                ) {
                    Icon(Icons.Filled.Camera, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("카메라로 촬영", color = Color.White, fontSize = 16.sp)
                }

                Button(
                    onClick = { pickImageLauncher.launch("image/*") },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF253F5A))
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("갤러리에서 선택", color = Color.White, fontSize = 16.sp)
                }
            }
        }

        // 권한 요청 다이얼로그 표시
        if (showPermissionDialog) {
            PermissionDialog(onDismiss = { showPermissionDialog = false }) {
                ActivityCompat.requestPermissions(
                    context as ComponentActivity,
                    arrayOf(Manifest.permission.CAMERA),
                    100
                )
                showPermissionDialog = false
            }
        }
    }
}

@Composable
fun PermissionDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("권한 요청") },
        text = { Text("카메라를 사용하려면 권한이 필요합니다.") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("허용")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewOnboardingScanScreen() {
    OnboardingScanScreen()
}
