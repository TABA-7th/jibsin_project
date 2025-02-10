package com.project.jibsin_project.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.project.jibsin_project.utils.FirebaseStorageUtil
import kotlinx.coroutines.launch

class BuildingRegistryScanActivity : ComponentActivity() {
    private val firebaseStorageUtil = FirebaseStorageUtil()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BuildingRegistryScanScreen(firebaseStorageUtil)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingRegistryScanScreen(firebaseStorageUtil: FirebaseStorageUtil) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    // 카메라 권한 상태 체크
    val hasPermission = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 카메라 실행 결과 처리
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            isLoading = true
            coroutineScope.launch {
                try {
                    val imageUrl = firebaseStorageUtil.uploadImage(bitmap, "building_registry")
                    // TODO: Firestore에 문서 정보 저장
                    isLoading = false
                } catch (e: Exception) {
                    // TODO: 에러 처리
                    isLoading = false
                }
            }
        }
    }

    // 갤러리 실행 결과 처리
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isLoading = true
            coroutineScope.launch {
                try {
                    val imageUrl = firebaseStorageUtil.uploadImageFromUri(uri, context, "building_registry")
                    // TODO: Firestore에 문서 정보 저장
                    isLoading = false
                } catch (e: Exception) {
                    // TODO: 에러 처리
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("건축물대장 스캔", fontSize = 20.sp) },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFFF9F9F9))
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { takePictureLauncher.launch(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF253F5A))
                ) {
                    Text("카메라로 스캔", color = Color.White, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { pickImageLauncher.launch("image/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF253F5A))
                ) {
                    Text("갤러리에서 업로드", color = Color.White, fontSize = 16.sp)
                }
            }

            // 로딩 인디케이터
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF253F5A)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewBuildingRegistryScanScreen() {
    BuildingRegistryScanScreen(FirebaseStorageUtil())
}