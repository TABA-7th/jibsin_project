package com.project.jibsin_project.scan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.project.jibsin_project.utils.ErrorDialog
import kotlinx.coroutines.tasks.await

class AIAnalysisResultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contractId = intent.getStringExtra("contractId") ?: "test_user-CT-2502275712"

        setContent {
            AIAnalysisResultScreen(contractId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAnalysisResultScreen(contractId: String) {
    var summaryData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Firestore에서 데이터 로딩
    LaunchedEffect(contractId) {
        try {
            isLoading = true
            val db = Firebase.firestore
            val documentSnapshot = db.collection("users")
                .document("test_user")
                .collection("contracts")
                .document(contractId)
                .collection("summary")
                .document("summary")
                .get()
                .await()

            if (documentSnapshot.exists()) {
                summaryData = documentSnapshot.data
            } else {
                errorMessage = "요약 데이터를 찾을 수 없습니다."
            }
            isLoading = false
        } catch (e: Exception) {
            errorMessage = "데이터를 불러오는 중 오류가 발생했습니다: ${e.message}"
            isLoading = false
        }
    }

    val primaryColor = Color(0xFF253F5A)
    val backgroundColor = Color(0xFFF9F9F9)
    val warningColor = Color(0xFFE57373)
    val successColor = Color(0xFF4CAF50)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("문서 분석 요약", fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = primaryColor
                ),
                navigationIcon = {
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = primaryColor
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
                .background(backgroundColor)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = primaryColor)
                }
            } else {
                summaryData?.let { data ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // 계약서 요약 정보 (상단 알림 카드)
                        item {
                            SummaryCard(data)
                        }

                        // 계약 기본 정보
                        item {
                            SectionTitle("계약 기본 정보")
                            ContractBasicInfoCard(data)
                        }

                        // 계약 세부 정보
                        item {
                            SectionTitle("계약 세부 정보")
                            ContractDetailsCard(data)
                        }

                        // 특약 사항
                        item {
                            SectionTitle("특약 사항")
                            SpecialTermsCard(data)
                        }
                    }
                } ?: run {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("데이터를 불러올 수 없습니다.", color = Color.Gray)
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
fun SummaryCard(data: Map<String, Any>) {
    val summaryMap = data["summary"] as? Map<*, *>
    val summaryText = summaryMap?.get("text") as? String ?: "요약 정보가 없습니다."

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF8E1) // 옅은 노란색 배경
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "경고",
                tint = Color(0xFFFF9800),
                modifier = Modifier.padding(end = 16.dp, top = 2.dp)
            )

            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF5D4037)
            )
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = Color(0xFF253F5A),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 16.dp)
    )
}

@Composable
fun ContractBasicInfoCard(data: Map<String, Any>) {
    val contractId = data["contractId"] as? String ?: ""
    val 소재지Map = (data["contract_details"] as? Map<*, *>)?.get("소재지") as? Map<*, *>
    val 소재지 = 소재지Map?.get("text") as? String ?: "-"
    val 임대인Map = (data["contract_details"] as? Map<*, *>)?.get("임대인") as? Map<*, *>
    val 임대인 = 임대인Map?.get("text") as? String ?: "-"
    val 임차할부분Map = (data["contract_details"] as? Map<*, *>)?.get("임차할부분") as? Map<*, *>
    val 임차할부분 = 임차할부분Map?.get("text") as? String ?: "-"

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
            InfoRow("계약서 ID", contractId)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            InfoRow("소재지", 소재지)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            InfoRow("임대인", 임대인)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            InfoRow("임차할 부분", 임차할부분)
        }
    }
}

@Composable
fun ContractDetailsCard(data: Map<String, Any>) {
    val contractDetails = data["contract_details"] as? Map<*, *> ?: mapOf<String, Any>()

    val 계약기간Map = contractDetails["계약기간"] as? Map<*, *>
    val 계약기간 = 계약기간Map?.get("text") as? String ?: "-"
    val 계약기간Check = 계약기간Map?.get("check") as? Boolean ?: false

    val 등기부등본Map = contractDetails["등기부등본"] as? Map<*, *>
    val 등기부등본 = 등기부등본Map?.get("text") as? String ?: "-"
    val 등기부등본Check = 등기부등본Map?.get("check") as? Boolean ?: false

    val 면적Map = contractDetails["면적"] as? Map<*, *>
    val 면적 = 면적Map?.get("text") as? String ?: "-"
    val 면적Check = 면적Map?.get("check") as? Boolean ?: false

    val 보증금Map = contractDetails["보증금"] as? Map<*, *>
    val 보증금 = 보증금Map?.get("text") as? String ?: "-"
    val 보증금Check = 보증금Map?.get("check") as? Boolean ?: false

    val 차임Map = contractDetails["차임"] as? Map<*, *>
    val 차임 = 차임Map?.get("text") as? String ?: "-"
    val 차임Check = 차임Map?.get("check") as? Boolean ?: false

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
            InfoRowWithCheck("계약기간", 계약기간, 계약기간Check)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            InfoRowWithCheck("등기부등본", 등기부등본, 등기부등본Check)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            InfoRowWithCheck("면적", 면적, 면적Check)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            InfoRowWithCheck("보증금", 보증금, 보증금Check)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            InfoRowWithCheck("차임(월세)", 차임, 차임Check)
        }
    }
}

@Composable
fun SpecialTermsCard(data: Map<String, Any>) {
    val contractDetails = data["contract_details"] as? Map<*, *> ?: mapOf<String, Any>()
    val 특약사항Map = contractDetails["특약사항"] as? Map<*, *>
    val 특약사항 = 특약사항Map?.get("text") as? String ?: "특약사항이 없습니다."
    val 특약사항Check = 특약사항Map?.get("check") as? Boolean ?: false

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (특약사항Check) Color.White else Color(0xFFFFEBEE)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!특약사항Check) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "정보",
                        tint = Color(0xFFE57373),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "특약사항을 주의 깊게 확인하세요",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE57373)
                    )
                }
                Divider(modifier = Modifier.padding(vertical = 4.dp))
            }

            Text(
                text = 특약사항,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
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
            color = Color.Gray,
            modifier = Modifier.weight(0.3f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black,
            modifier = Modifier.weight(0.7f)
        )
    }
}

@Composable
fun InfoRowWithCheck(label: String, value: String, isValid: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            modifier = Modifier.weight(0.3f)
        )

        Row(
            modifier = Modifier.weight(0.7f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isValid) Color.Black else Color(0xFFE57373),
                modifier = Modifier.weight(1f)
            )

            if (!isValid) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "문제 있음",
                    tint = Color(0xFFE57373),
                    modifier = Modifier.padding(start = 8.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "정상",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}