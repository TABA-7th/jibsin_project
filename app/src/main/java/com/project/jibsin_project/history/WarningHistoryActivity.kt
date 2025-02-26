package com.project.jibsin_project.history

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
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
import com.project.jibsin_project.utils.FirestoreUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WarningHistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WarningHistoryScreen(
                onBackPressed = { finish() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarningHistoryScreen(
    onBackPressed: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val firestoreUtil = remember { FirestoreUtil() }
    var warnings by remember { mutableStateOf<List<WarningItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    // 경고 내역 데이터 로드
    LaunchedEffect(key1 = Unit) {
        isLoading = true
        coroutineScope.launch {
            try {
                // 실제 구현에서는 Firestore에서 경고 내역을 가져오는 로직 구현
                // 여기서는 샘플 데이터로 대체
                warnings = getSampleWarnings()
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
                title = { Text("경고 내역", fontWeight = FontWeight.Bold) },
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
            } else if (warnings.isEmpty()) {
                // 경고 내역이 없을 때
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "저장된 경고 내역이 없습니다.",
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "계약서 분석 중 발견된 경고들이 여기에 표시됩니다.",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            } else {
                // 경고 내역 리스트
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(warnings.sortedByDescending { it.date }) { warning ->
                        WarningHistoryItem(
                            warning = warning,
                            onDelete = {
                                // 경고 삭제 로직
                                coroutineScope.launch {
                                    // 실제 구현에서는 Firestore에서 경고 삭제 로직 구현
                                    warnings = warnings.filter { it.id != warning.id }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarningHistoryItem(
    warning: WarningItem,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(warning.date)
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 위험 수준에 따른 색상 및 아이콘
    val iconContent: @Composable () -> Unit = when (warning.level) {
        WarningLevel.DANGER -> {
            { Icon(Icons.Filled.Error, contentDescription = null, tint = Color.White) }
        }
        WarningLevel.WARNING -> {
            { Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White) }
        }
    }

    val backgroundColor = when (warning.level) {
        WarningLevel.DANGER -> Color(0xFFE53935)
        WarningLevel.WARNING -> Color(0xFFFF9800)
    }

    val textColor = backgroundColor

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 상단 행: 날짜 및 삭제 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 날짜
                Text(
                    text = formattedDate,
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                // 삭제 버튼
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "삭제",
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 경고 내용 행
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 경고 아이콘
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(backgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    iconContent()
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 경고 제목
                Text(
                    text = if (warning.level == WarningLevel.DANGER) "위험" else "주의",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 경고 상세 내용
            Text(
                text = warning.message,
                fontSize = 14.sp,
                color = Color.DarkGray,
                modifier = Modifier.padding(start = 44.dp) // 아이콘 너비 + 간격만큼 들여쓰기
            )

            // 해결 방안이 있는 경우 표시
            if (warning.solution.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "해결 방법:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(start = 44.dp)
                )
                Text(
                    text = warning.solution,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(start = 44.dp)
                )
            }

            // 문서 출처 정보
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "출처: ${warning.source}",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 44.dp)
            )
        }
    }

    // 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("경고 삭제") },
            text = { Text("이 경고를 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("삭제", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

// 위험 수준 열거형
enum class WarningLevel {
    WARNING,    // 주의 (주황색)
    DANGER      // 위험 (빨간색)
}

// 경고 항목 데이터 클래스
data class WarningItem(
    val id: String,               // 고유 ID
    val level: WarningLevel,      // 위험 수준
    val message: String,          // 경고 메시지
    val solution: String = "",    // 해결 방안
    val source: String,           // 출처 (어떤 문서에서 발견됐는지)
    val date: Date = Date(),      // 생성 날짜
    val contractId: String        // 관련 계약 ID
)

// 샘플 경고 데이터 생성 함수
fun getSampleWarnings(): List<WarningItem> {
    return listOf(
        WarningItem(
            id = "warning1",
            level = WarningLevel.DANGER,
            message = "보증금이 계약서와 등기부등본 사이에 일치하지 않습니다. 계약서에는 1억원, 등기부등본에는 5천만원으로 기재되어 있습니다.",
            solution = "계약 전 임대인에게 확인하고 정확한 보증금 금액을 명시해야 합니다.",
            source = "계약서 / 등기부등본",
            date = Date(),
            contractId = "contract-123"
        ),
        WarningItem(
            id = "warning2",
            level = WarningLevel.WARNING,
            message = "임대인 이름이 계약서와 등기부등본에서 다르게 표기되어 있습니다.",
            solution = "계약 전 실제 소유자 확인이 필요합니다.",
            source = "계약서 / 등기부등본",
            date = Date(System.currentTimeMillis() - 86400000), // 하루 전
            contractId = "contract-123"
        ),
        WarningItem(
            id = "warning3",
            level = WarningLevel.DANGER,
            message = "등기부등본에 근저당권이 설정되어 있습니다. 총액 2억 5천만원의 근저당권이 설정되어 있어 보증금 회수에 위험이 있을 수 있습니다.",
            solution = "전세권 설정 또는 보증보험 가입을 고려하세요.",
            source = "등기부등본",
            date = Date(System.currentTimeMillis() - 172800000), // 이틀 전
            contractId = "contract-456"
        ),
        WarningItem(
            id = "warning4",
            level = WarningLevel.WARNING,
            message = "건축물대장의 발급일자가 오늘이 아닙니다. 최신 정보가 아닐 수 있습니다.",
            solution = "최신 건축물대장을 확인하세요.",
            source = "건축물대장",
            date = Date(System.currentTimeMillis() - 259200000), // 3일 전
            contractId = "contract-789"
        ),
        WarningItem(
            id = "warning5",
            level = WarningLevel.DANGER,
            message = "계약서에 명시된 주소와 등기부등본의 주소가 일치하지 않습니다.",
            solution = "정확한 주소를 확인하고 계약서를 수정하세요.",
            source = "계약서 / 등기부등본",
            date = Date(System.currentTimeMillis() - 345600000), // 4일 전
            contractId = "contract-789"
        )
    )
}