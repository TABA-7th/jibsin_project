package com.project.jibsin_project.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.accompanist.pager.*
import com.project.jibsin_project.R
import com.project.jibsin_project.utils.FirebaseStorageUtil
import com.project.jibsin_project.utils.FirestoreUtil
import com.project.jibsin_project.utils.ScannedDocument
import com.project.jibsin_project.utils.rememberCameraPermissionState
import kotlinx.coroutines.launch

class OnboardingScanActivity : ComponentActivity() {
    private val firebaseStorageUtil = FirebaseStorageUtil()
    private val firestoreUtil = FirestoreUtil()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OnboardingScanScreen(firebaseStorageUtil, firestoreUtil)
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun OnboardingScanScreen(firebaseStorageUtil: FirebaseStorageUtil, firestoreUtil: FirestoreUtil) {
    val context = LocalContext.current
    val activity = LocalContext.current as ComponentActivity
    var hasCameraPermission by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val pagerState = rememberPagerState()
    val pages = listOf(
        OnboardingPage("건축물대장", "건축물대장을 스캔하여 분석하세요.", R.drawable.ic_scan),
        OnboardingPage("등기부등본", "등기부등본을 스캔하여 분석하세요.", R.drawable.ic_scan),
        OnboardingPage("계약서", "계약서를 스캔하여 분석하세요.", R.drawable.ic_scan)
    )

    // 카메라 권한 요청 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCameraPermission = isGranted
    }

    // 카메라 실행 런처
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            isLoading = true
            coroutineScope.launch {
                try {
                    val documentType = when (pagerState.currentPage) {
                        0 -> "building_registry"
                        1 -> "registry_document"
                        2 -> "contract"
                        else -> return@launch
                    }
                    val imageUrl = firebaseStorageUtil.uploadImage(bitmap, documentType)
                    val document = ScannedDocument(
                        type = documentType,
                        imageUrl = imageUrl,
                        userId = "test_user"
                    )
                    val documentId = firestoreUtil.saveScannedDocument(document)
                    isLoading = false

                    val intent = Intent(context, AIAnalysisResultActivity::class.java).apply {
                        putExtra("documentId", documentId)
                        putExtra("documentType", documentType)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    isLoading = false
                    errorMessage = "업로드 실패: ${e.message}"
                }
            }
        }
    }

    // 갤러리 실행 런처
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isLoading = true
            coroutineScope.launch {
                try {
                    val documentType = when (pagerState.currentPage) {
                        0 -> "building_registry"
                        1 -> "registry_document"
                        2 -> "contract"
                        else -> return@launch
                    }
                    val imageUrl = firebaseStorageUtil.uploadImageFromUri(uri, context, documentType)
                    val document = ScannedDocument(
                        type = documentType,
                        imageUrl = imageUrl,
                        userId = "test_user"
                    )
                    val documentId = firestoreUtil.saveScannedDocument(document)
                    isLoading = false

                    val intent = Intent(context, AIAnalysisResultActivity::class.java).apply {
                        putExtra("documentId", documentId)
                        putExtra("documentType", documentType)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    isLoading = false
                    errorMessage = "업로드 실패: ${e.message}"
                }
            }
        }
    }

    // 카메라 버튼 클릭 처리
    val onCameraClick = {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // 권한이 있으면 카메라 실행
                takePictureLauncher.launch(null)
            }
            else -> {
                // 권한 요청
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            BottomAppBar(
                containerColor = Color.White,
                content = {
                    Spacer(modifier = Modifier.weight(1f))
                }
            )
        }
    ) { padding ->
        Box {
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
                    OnboardingPageContent(
                        page = pages[page],
                        onCameraClick = onCameraClick,
                        onGalleryClick = { pickImageLauncher.launch("image/*") }
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalPagerIndicator(
                        pagerState = pagerState,
                        activeColor = Color(0xFF253F5A),
                        inactiveColor = Color(0xFFBDBDBD),
                        indicatorWidth = 8.dp,
                        indicatorHeight = 8.dp,
                        spacing = 8.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

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

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF253F5A)
                )
            }

            errorMessage?.let {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { errorMessage = null }) {
                            Text("확인")
                        }
                    }
                ) {
                    Text(text = it)
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(
    page: OnboardingPage,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
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

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onCameraClick,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(2.dp, Color(0xFF253F5A))
            ) {
                Icon(Icons.Filled.Camera, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("카메라로 촬영", color = Color.Black, fontSize = 16.sp)
            }

            OutlinedButton(
                onClick = onGalleryClick,
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

data class OnboardingPage(
    val title: String,
    val description: String,
    val imageRes: Int
)

@Preview(showBackground = true)
@Composable
fun PreviewOnboardingScanScreen() {
    OnboardingScanScreen(FirebaseStorageUtil(), FirestoreUtil())
}