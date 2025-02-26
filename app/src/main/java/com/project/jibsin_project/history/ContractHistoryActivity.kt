package com.project.jibsin_project.history

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.jibsin_project.scan.components.DocumentReviewActivity
import com.project.jibsin_project.utils.Contract
import com.project.jibsin_project.utils.FirestoreUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ContractHistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ContractHistoryScreen(
                onBackPressed = { finish() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractHistoryScreen(
    onBackPressed: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val firestoreUtil = remember { FirestoreUtil() }
    var contracts by remember { mutableStateOf<List<Pair<String, Contract>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    // 계약 내역 로드
    LaunchedEffect(key1 = Unit) {
        isLoading = true
        coroutineScope.launch {
            try {
                contracts = firestoreUtil.getAllContracts("test_user")
                isLoading = false
            } catch (e: Exception) {
                // 오류 처리
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("나의 계약 내역", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF9F9F9)
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF9F9F9))
        ) {
            if (isLoading) {
                // 로딩 중 표시
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF253F5A)
                )
            } else if (contracts.isEmpty()) {
                // 계약 내역이 없을 때
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "계약 내역이 없습니다.",
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "새 계약서를 업로드하여 분석해보세요.",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            } else {
                // 계약 내역 리스트
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(contracts.sortedByDescending { it.second.createDate }) { (contractId, contract) ->
                        ContractHistoryItem(
                            contractId = contractId,
                            contract = contract,
                            onClick = {
                                // 계약서 분석 화면으로 이동
                                val intent = Intent(context, DocumentReviewActivity::class.java).apply {
                                    putExtra("contractId", contractId)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContractHistoryItem(
    contractId: String,
    contract: Contract,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
    val createDate = if (contract.createDate != null) {
        dateFormat.format(contract.createDate.toDate())
    } else {
        "날짜 없음"
    }

    // 계약서 유형 (전세/월세) 결정
    val contractTypeText = when {
        contractId.contains("lease", ignoreCase = true) -> "전세"
        contractId.contains("monthly", ignoreCase = true) -> "월세"
        else -> "계약서"
    }

    // 분석 상태에 따른 상태 텍스트 및 색상
    val (statusText, statusColor) = when (contract.analysisStatus) {
        "completed" -> Pair("분석 완료", Color(0xFF4CAF50))
        "processing" -> Pair("분석 중", Color(0xFFFF9800))
        else -> Pair("대기 중", Color.Gray)
    }

    // 문서 수량 계산
    val documentCount = contract.building_registry.size + contract.registry_document.size + contract.contract.size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 상단 정보 (ID, 날짜)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 계약서 ID (앞부분만 표시)
                Text(
                    text = contractId.substringAfter("-").take(10) + "...",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                // 상태 표시 (분석 완료/진행 중/대기 중)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 계약 타입 및 정보
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Description,
                    contentDescription = null,
                    tint = Color(0xFF253F5A),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$contractTypeText 계약서",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF253F5A)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 생성 날짜
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.DateRange,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = createDate,
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.width(16.dp))

                // 문서 수량 표시
                Text(
                    text = "등록 문서 ${documentCount}개",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // 분석 결과 요약 (있는 경우)
            if (contract.analysisResult != null && contract.analysisStatus == "completed") {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                // 간단한 결과 요약 (예: 위험 요소 수)
                val warningCount = getWarningCount(contract.analysisResult)

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (warningCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFE53935), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "위험 요소 ${warningCount}개 발견",
                            fontSize = 14.sp,
                            color = Color(0xFFE53935),
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF4CAF50), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "이상 없음",
                            fontSize = 14.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// 위험 요소 개수 파악 함수 (예시)
fun getWarningCount(analysisResult: Map<String, Any>?): Int {
    if (analysisResult == null) return 0

    var count = 0

    // 분석 결과에서 경고 수 찾기 (구조에 맞게 수정 필요)
    try {
        // 예: warning, notice, alert 등의 키에서 검색
        (analysisResult["warnings"] as? List<*>)?.size?.let { count += it }
        (analysisResult["notices"] as? List<*>)?.size?.let { count += it }

        // 중첩된 구조도 확인
        (analysisResult["result"] as? Map<*, *>)?.let { result ->
            (result["warnings"] as? List<*>)?.size?.let { count += it }
            (result["notices"] as? List<*>)?.size?.let { count += it }
        }
    } catch (e: Exception) {
        // 오류 발생 시 0 반환
        return 0
    }

    return count
}