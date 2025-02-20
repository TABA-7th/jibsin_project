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
import com.project.jibsin_project.utils.ContractManager
import com.project.jibsin_project.utils.FirebaseStorageUtil
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.UUID

class ContractScanActivity : ComponentActivity() {
    private val contractManager = ContractManager()
    private val firebaseStorageUtil = FirebaseStorageUtil()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ContractScanScreen(contractManager, firebaseStorageUtil)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractScanScreen(contractManager: ContractManager, firebaseStorageUtil: FirebaseStorageUtil) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val userId = "test_user"

    // 계약 ID 생성
    val contractId = remember {
        runBlocking {
            contractManager.createContract(userId)
        }
    }
    var currentPage by remember { mutableStateOf(1) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            isLoading = true
            coroutineScope.launch {
                try {
                    val imageUrl = firebaseStorageUtil.uploadScannedImage(
                        bitmap = bitmap,
                        documentType = "contract",
                        contractId = contractId,
                        pageNumber = currentPage
                    )

                    contractManager.addDocument(
                        userId = userId,
                        contractId = contractId,
                        documentType = "contract",
                        imageUrl = imageUrl,
                        pageNumber = currentPage
                    )
                    currentPage++
                    isLoading = false

                    val intent = Intent(context, AIAnalysisResultActivity::class.java).apply {
                        putExtra("contractId", contractId)
                        putExtra("userId", userId)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    isLoading = false
                    errorMessage = "업로드 실패: ${e.message}"
                }
            }
        }
    }

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
                        documentType = "contract",
                        contractId = contractId,
                        pageNumber = currentPage
                    )

                    contractManager.addDocument(
                        userId = userId,
                        contractId = contractId,
                        documentType = "contract",
                        imageUrl = imageUrl,
                        pageNumber = currentPage
                    )
                    currentPage++
                    isLoading = false

                    val intent = Intent(context, AIAnalysisResultActivity::class.java).apply {
                        putExtra("contractId", contractId)
                        putExtra("userId", userId)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    isLoading = false
                    errorMessage = "업로드 실패: ${e.message}"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("계약서 스캔", fontSize = 20.sp) },
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
fun PreviewContractScanScreen() {
    ContractScanScreen(
        firebaseStorageUtil = FirebaseStorageUtil(),
        contractManager = ContractManager()
    )
}