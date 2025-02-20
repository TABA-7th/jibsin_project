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
import com.project.jibsin_project.utils.FirestoreUtil
import com.project.jibsin_project.utils.ContractStatus
import kotlinx.coroutines.launch

class BuildingRegistryScanActivity : ComponentActivity() {
    private val firebaseStorageUtil = FirebaseStorageUtil()
    private val firestoreUtil = FirestoreUtil()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BuildingRegistryScanScreen(firebaseStorageUtil, firestoreUtil)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingRegistryScanScreen(
    firebaseStorageUtil: FirebaseStorageUtil,
    firestoreUtil: FirestoreUtil
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentPage by remember { mutableStateOf(1) }

    // contractId 상태 관리
    var contractId by remember { mutableStateOf<String?>(null) }

    // 컴포넌트가 처음 생성될 때 새로운 계약서 문서 생성
    LaunchedEffect(Unit) {
        try {
            contractId = firestoreUtil.createNewContract("test_user")
        } catch (e: Exception) {
            errorMessage = "계약서 생성 실패: ${e.message}"
        }
    }

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
        if (bitmap != null && contractId != null) {
            isLoading = true
            coroutineScope.launch {
                try {
                    val imageUrl = firebaseStorageUtil.uploadScannedImage(
                        bitmap = bitmap,
                        documentType = "building_registry",
                        userId = "test_user",
                        contractId = contractId!!,
                        pageNumber = currentPage
                    )

                    firestoreUtil.addDocumentToContract(
                        userId = "test_user",
                        contractId = contractId!!,
                        documentType = "building_registry",
                        imageUrl = imageUrl,
                        pageNumber = currentPage
                    )

                    currentPage++
                    isLoading = false

                    val intent = Intent(context, AIAnalysisResultActivity::class.java).apply {
                        putExtra("contractId", contractId)
                    }
                    context.startActivity(intent)
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
        if (uri != null && contractId != null) {
            isLoading = true
            coroutineScope.launch {
                try {
                    val imageUrl = firebaseStorageUtil.uploadScannedImageFromUri(
                        uri = uri,
                        context = context,
                        documentType = "building_registry",
                        userId = "test_user",
                        contractId = contractId!!,
                        pageNumber = currentPage
                    )

                    firestoreUtil.addDocumentToContract(
                        userId = "test_user",
                        contractId = contractId!!,
                        documentType = "building_registry",
                        imageUrl = imageUrl,
                        pageNumber = currentPage
                    )

                    currentPage++
                    isLoading = false

                    val intent = Intent(context, AIAnalysisResultActivity::class.java).apply {
                        putExtra("contractId", contractId)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    errorMessage = "업로드 실패: ${e.message}"
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
        firestoreUtil = FirestoreUtil()
    )
}