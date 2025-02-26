package com.project.jibsin_project.scan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.jibsin_project.scan.components.DocumentAnalysisScreen
import com.project.jibsin_project.utils.DocumentAnalyzer
import com.project.jibsin_project.utils.ErrorDialog
import com.project.jibsin_project.utils.FirestoreUtil
import kotlinx.coroutines.delay
import com.project.jibsin_project.utils.Contract

class AIAnalysisResultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contractId = intent.getStringExtra("contractId") ?: return

        setContent {
            AIAnalysisResultScreen(contractId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAnalysisResultScreen(contractId: String) {
    var analysisResult by remember { mutableStateOf<Map<String, Any>?>(null) }
    var contract by remember { mutableStateOf<Contract?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val documentAnalyzer = remember { DocumentAnalyzer() }
    val firestoreUtil = remember { FirestoreUtil() }
    val context = LocalContext.current

    // 분석 결과 로딩
    LaunchedEffect(contractId) {
        try {
            isLoading = true

            // 분석 결과가 나올 때까지 주기적으로 확인
            while (true) {
                val currentContract = firestoreUtil.getContract("test_user", contractId)
                if (currentContract?.analysisStatus == "completed") {  // analysisStatus로 체크
                    contract = currentContract           // Contract 저장
                    analysisResult = currentContract.analysisResult
                    break
                }
                delay(2000) // 2초마다 확인
            }

            isLoading = false
        } catch (e: Exception) {
            errorMessage = "분석 결과를 불러오는 중 오류가 발생했습니다: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("문서 분석 결과", fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF9F9F9),
                    titleContentColor = Color(0xFF253F5A)
                ),
                navigationIcon = {
                    // 뒤로가기 버튼 추가
                    IconButton(onClick = {
                        // Activity 종료
                        (context as? ComponentActivity)?.finish()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = Color(0xFF253F5A)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF253F5A))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // 문서 미리보기와 알림 표시
                    item {
                        analysisResult?.let { result ->
                            contract?.let { currentContract ->  // null 체크
                                DocumentAnalysisScreen(
                                    analysisResult = result,
                                    contract = currentContract
                                )
                            }
                        }
                    }

                    // 문서 정보 섹션
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "문서 정보",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF253F5A),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        DocumentInfoSection(analysisResult)
                    }

                    // 유효성 검사 결과 섹션
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "유효성 검사 결과",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF253F5A),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        ValidationResultSection(analysisResult)
                    }

                    // 추가 정보 섹션
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "추가 정보",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF253F5A),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        AdditionalInfoSection(analysisResult)
                    }
                }
            }

            // 에러 메시지 표시
            errorMessage?.let { message ->
                ErrorDialog(
                    message = message,
                    onDismiss = { errorMessage = null }
                )
            }
        }
    }
}

@Composable
fun DocumentInfoSection(result: Map<String, Any>?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoItem("건물명", result?.get("buildingName") as? String)
            InfoItem("주소", result?.get("address") as? String)
            InfoItem("면적", result?.get("area") as? String)
            InfoItem("등기 종류", result?.get("registryType") as? String)
        }
    }
}

@Composable
fun ValidationResultSection(result: Map<String, Any>?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ValidationItem("임대인 정보 일치", result?.get("lessorMatch") as? Boolean ?: false)
            ValidationItem("임차인 정보 일치", result?.get("lesseeMatch") as? Boolean ?: false)
            ValidationItem("주소 정보 일치", result?.get("addressMatch") as? Boolean ?: false)
        }
    }
}

@Composable
fun AdditionalInfoSection(result: Map<String, Any>?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoItem("임대료", result?.get("rentAmount") as? String)
            InfoItem("계약 기간", result?.get("contractPeriod") as? String)
            InfoItem("보증금", result?.get("deposit") as? String)
        }
    }
}

@Composable
fun InfoItem(label: String, value: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
        Text(
            text = value ?: "-",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black
        )
    }
}

@Composable
fun ValidationItem(label: String, isValid: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
        Icon(
            imageVector = if (isValid) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            contentDescription = if (isValid) "일치" else "불일치",
            tint = if (isValid) Color(0xFF4CAF50) else Color(0xFFE57373)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAIAnalysisResultScreen() {
    AIAnalysisResultScreen(contractId = "preview_contract_id")
}