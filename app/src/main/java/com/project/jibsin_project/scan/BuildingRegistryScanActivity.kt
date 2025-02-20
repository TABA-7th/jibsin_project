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
import com.project.jibsin_project.utils.ContractManager
import com.project.jibsin_project.utils.DocumentUploadManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class BuildingRegistryScanActivity : ComponentActivity() {
    private val firebaseStorageUtil = FirebaseStorageUtil()
    private val contractManager = ContractManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BuildingRegistryScanScreen(firebaseStorageUtil, contractManager)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingRegistryScanScreen(
    firebaseStorageUtil: FirebaseStorageUtil,
    contractManager: ContractManager,
    documentUploadManager: DocumentUploadManager = DocumentUploadManager.getInstance()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 계약 초기화 상태 관리
    var isContractInitialized by remember { mutableStateOf(false) }

    // 계약 초기화
    LaunchedEffect(Unit) {
        try {
            isLoading = true
            if (documentUploadManager.getCurrentContractId().isEmpty()) {
                documentUploadManager.createNewContract("test_user")
            }
            isContractInitialized = true
            isLoading = false
        } catch (e: Exception) {
            errorMessage = "계약 초기화 실패: ${e.message}"
            isLoading = false
        }
    }

    if (!isContractInitialized) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF253F5A))
        }
        return
    }

    val contractId = documentUploadManager.getCurrentContractId()
    val userId = documentUploadManager.getCurrentUserId()

    // 현재 문서 수 확인
    val documents = remember(contractId) {
        runBlocking {
            contractManager.getDocuments(userId, contractId, "building_registry")
        }
    }
    val currentPageNumber = remember(documents) {
        documents.size + 1
    }

    // 카메라 실행 결과 처리
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            isLoading = true
            coroutineScope.launch {
                try {
                    val imageUrl = firebaseStorageUtil.uploadScannedImage(
                        bitmap = bitmap,
                        documentType = "building_registry",
                        contractId = contractId,
                        pageNumber = currentPageNumber
                    )

                    documentUploadManager.addDocument(
                        type = "building_registry",
                        imageUrl = imageUrl,
                        pageNumber = currentPageNumber
                    )

                    // AIAnalysisResultActivity로 이동
                    val intent = Intent(context, AIAnalysisResultActivity::class.java).apply {
                        putExtra("contractId", contractId)
                        putExtra("userId", userId)
                    }
                    context.startActivity(intent)
                    isLoading = false
                } catch (e: Exception) {
                    errorMessage = "업로드 실패: ${e.message}"
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
                    val imageUrl = firebaseStorageUtil.uploadScannedImageFromUri(
                        uri = uri,
                        context = context,
                        documentType = "building_registry",
                        contractId = contractId,
                        pageNumber = currentPageNumber
                    )

                    documentUploadManager.addDocument(
                        type = "building_registry",
                        imageUrl = imageUrl,
                        pageNumber = currentPageNumber
                    )

                    // AIAnalysisResultActivity로 이동
                    val intent = Intent(context, AIAnalysisResultActivity::class.java).apply {
                        putExtra("contractId", contractId)
                        putExtra("userId", userId)
                    }
                    context.startActivity(intent)
                    isLoading = false
                } catch (e: Exception) {
                    errorMessage = "업로드 실패: ${e.message}"
                    isLoading = false
                }
            }
        }
    }

    // 카메라 권한 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            takePictureLauncher.launch(null)
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
                    onClick = {
                        when {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED -> {
                                takePictureLauncher.launch(null)
                            }
                            else -> {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    },
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

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = it,
                        color = Color.Red,
                        fontSize = 14.sp
                    )
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
    BuildingRegistryScanScreen(
        firebaseStorageUtil = FirebaseStorageUtil(),
        contractManager = ContractManager()
    )
}