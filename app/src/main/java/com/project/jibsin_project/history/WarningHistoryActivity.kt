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
    var loadError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val userId = "test_user" // 실제 환경에서는 로그인된 사용자 ID를 사용

    // 경고 내역 데이터 로드
    LaunchedEffect(key1 = Unit) {
        isLoading = true
        loadError = null

        coroutineScope.launch {
            try {
                // Firebase에서 실제 경고 내역 불러오기
                warnings = firestoreUtil.getWarnings(userId)
                isLoading = false
            } catch (e: Exception) {
                // 오류 발생 시 처리
                loadError = "경고 내역을 불러오는 중 오류가 발생했습니다: ${e.message}"
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
            } else if (loadError != null) {
                // 오류 발생 시 메시지 표시
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "오류 발생",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = loadError!!,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                loadError = null
                                try {
                                    warnings = firestoreUtil.getWarnings(userId)
                                    isLoading = false
                                } catch (e: Exception) {
                                    loadError = "경고 내역을 불러오는 중 오류가 발생했습니다: ${e.message}"
                                    isLoading = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF253F5A)
                        )
                    ) {
                        Text("다시 시도")
                    }
                }
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
                                    try {
                                        // Firebase에서 실제로 경고 삭제
                                        firestoreUtil.deleteWarning(userId, warning.id)

                                        // 삭제 후 로컬 리스트 업데이트
                                        warnings = warnings.filter { it.id != warning.id }
                                    } catch (e: Exception) {
                                        // 삭제 중 오류 발생 시 처리
                                        loadError = "경고를 삭제하는 중 오류가 발생했습니다: ${e.message}"
                                    }
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