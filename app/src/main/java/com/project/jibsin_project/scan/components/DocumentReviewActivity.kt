package com.project.jibsin_project.scan.components

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.project.jibsin_project.history.WarningItem
import com.project.jibsin_project.history.WarningLevel
import com.project.jibsin_project.scan.AIAnalysisResultActivity
import com.project.jibsin_project.scan.components.DocumentReviewActivity.Companion.getDocumentTypeKorean
import com.project.jibsin_project.scan.components.DocumentReviewActivity.Companion.isHighRiskNotice
import com.project.jibsin_project.utils.FirestoreUtil
import com.project.jibsin_project.utils.BoundingBox
import com.project.jibsin_project.utils.Contract
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class DocumentReviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contractId = intent.getStringExtra("contractId") ?: return

        setContent {
            MultiPageDocumentReviewScreen(contractId)
        }
    }

    // 문서 타입 한글 명칭 반환 함수
    companion object {
        fun getDocumentTypeKorean(documentType: String): String {
            return when (documentType) {
                "building_registry" -> "건축물대장"
                "registry_document" -> "등기부등본"
                "contract" -> "계약서"
                else -> documentType
            }
        }

        // 높은 위험 알림인지 판단하는 함수
        fun isHighRiskNotice(notice: Notice): Boolean {
            // 위험 키워드가 포함된 알림은 높은 위험으로 분류
            val highRiskKeywords = listOf(
                "보증금", "일치하지 않", "소유자", "임대인", "근저당", "압류",
                "가압류", "가처분", "경매", "위험", "찾을 수 없", "불일치"
            )

            // 알림 내용에 위험 키워드가 포함되어 있는지 확인
            return highRiskKeywords.any { keyword ->
                notice.notice.contains(keyword)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiPageDocumentReviewScreen(contractId: String) {
    val firestoreUtil = remember { FirestoreUtil() }
    val context = LocalContext.current
    var contract by remember { mutableStateOf<Contract?>(null) }
    var currentDocumentType by remember { mutableStateOf("building_registry") }
    var currentPageIndex by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var boundingBoxes by remember { mutableStateOf(emptyList<BoundingBox>()) }
    var notices by remember { mutableStateOf(emptyList<Notice>()) }
    var imageWidth by remember { mutableStateOf(0f) }
    var imageHeight by remember { mutableStateOf(0f) }
    var imageWidthPx by remember { mutableStateOf(0) }
    var imageHeightPx by remember { mutableStateOf(0) }
    val docTypeTabItems = listOf("건축물대장", "등기부등본", "계약서")
    val docTypeKeys = listOf("building_registry", "registry_document", "contract")
    var showNotices by remember { mutableStateOf(true) } // 알림 표시 여부 상태

    // 계약 데이터 로드
    LaunchedEffect(contractId) {
        isLoading = true
        contract = firestoreUtil.getContract("test_user", contractId)
        isLoading = false
    }

    // 현재 문서 및 페이지에 대한 바운딩 박스와 알림 데이터 로드
    LaunchedEffect(contractId, currentDocumentType, currentPageIndex) {
        isLoading = true

        try {
            // 1. 바운딩 박스 데이터 가져오기
            val (boxes, dimensions) = when (currentDocumentType) {
                "building_registry" -> firestoreUtil.getBuildingRegistryAnalysis("test_user", contractId, currentPageIndex + 1)
                "registry_document" -> firestoreUtil.getRegistryDocumentAnalysis("test_user", contractId, currentPageIndex + 1)
                "contract" -> firestoreUtil.getContractAnalysis("test_user", contractId, currentPageIndex + 1)
                else -> Pair(emptyList(), Pair(1f, 1f))
            }

            boundingBoxes = boxes
            imageWidth = dimensions.first
            imageHeight = dimensions.second

            // 2. 알림 데이터 직접 가져오기
            val noticesList = firestoreUtil.getAIAnalysisNotices(
                userId = "test_user",
                contractId = contractId,
                documentType = currentDocumentType,
                pageNumber = currentPageIndex + 1
            ).toMutableList()

            // 3. 발급일자 체크 및 알림 추가 (등기부등본과 건축물대장에만 적용)
            if (currentDocumentType == "registry_document" || currentDocumentType == "building_registry") {
                checkAndCreateDateAlert(
                    notices = noticesList,
                    documentType = currentDocumentType,
                    firestoreUtil = firestoreUtil,
                    contractId = contractId
                )
            }

            // 4. 최종 알림 목록 설정
            notices = noticesList

            // 5. 알림을 경고 내역으로 자동 저장 - 최초 페이지 로드 시 한 번만 수행
            if (currentPageIndex == 0) {
                // 알림 목록 순회
                noticesList.forEach { notice ->
                    // 문제가 있는 알림만 저장 ("문제 없음"이 아닌 알림)
                    if (notice.notice != "문제 없음" && notice.notice.isNotEmpty()) {
                        val warningLevel = if (DocumentReviewActivity.isHighRiskNotice(notice)) WarningLevel.DANGER else WarningLevel.WARNING

                        // 경고 내역으로 변환
                        val warningItem = WarningItem(
                            id = UUID.randomUUID().toString(),
                            level = warningLevel,
                            message = notice.notice,
                            solution = notice.solution,
                            source = DocumentReviewActivity.getDocumentTypeKorean(notice.documentType),
                            date = Date(),
                            contractId = contractId
                        )

                        // LaunchedEffect 내부에서는 직접 suspend 함수 호출 가능
                        try {
                            firestoreUtil.saveWarning("test_user", warningItem)
                        } catch (e: Exception) {
                            println("경고 저장 중 오류: ${e.message}")
                        }
                    }
                }
            }

        } catch (e: Exception) {
            println("문서 데이터 로드 중 오류: ${e.message}")
            e.printStackTrace()
            boundingBoxes = emptyList()
            notices = emptyList()
        }

        isLoading = false
    }

    // 현재 문서 타입의 총 페이지 수
    val totalPages = when (currentDocumentType) {
        "building_registry" -> contract?.building_registry?.size ?: 0
        "registry_document" -> contract?.registry_document?.size ?: 0
        "contract" -> contract?.contract?.size ?: 0
        else -> 0
    }

    // 현재 문서의 현재 페이지 URL
    val currentImageUrl = when (currentDocumentType) {
        "building_registry" -> contract?.building_registry?.getOrNull(currentPageIndex)?.imageUrl
        "registry_document" -> contract?.registry_document?.getOrNull(currentPageIndex)?.imageUrl
        "contract" -> contract?.contract?.getOrNull(currentPageIndex)?.imageUrl
        else -> null
    }

    // 현재 마지막 페이지인지 확인
    val isLastDocType = currentDocumentType == docTypeKeys.last()
    val isLastPage = currentPageIndex == totalPages - 1
    val isVeryLastPage = isLastDocType && isLastPage

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            when (currentDocumentType) {
                                "building_registry" -> "건축물대장 검토"
                                "registry_document" -> "등기부등본 검토"
                                "contract" -> "계약서 검토"
                                else -> "문서 검토"
                            }
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFF9F9F9),
                        titleContentColor = Color(0xFF253F5A)
                    )
                )

                // 문서 타입 탭
                TabRow(
                    selectedTabIndex = docTypeKeys.indexOf(currentDocumentType),
                    containerColor = Color(0xFF253F5A),
                    contentColor = Color.White
                ) {
                    docTypeTabItems.forEachIndexed { index, title ->
                        Tab(
                            selected = docTypeKeys.indexOf(currentDocumentType) == index,
                            onClick = {
                                currentDocumentType = docTypeKeys[index]
                                currentPageIndex = 0  // 문서 타입 변경시 첫 페이지로 이동
                            },
                            text = { Text(title) }
                        )
                    }
                }

                // 페이지 정보 표시
                if (totalPages > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.LightGray.copy(alpha = 0.3f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "페이지 ${currentPageIndex + 1} / $totalPages",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF253F5A)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.LightGray)
        ) {
            // 로딩 인디케이터
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF253F5A)
                )
            } else if (currentImageUrl != null) {
                // 이미지와 바운딩 박스
                Box(modifier = Modifier.fillMaxSize()) {
                    // 1. 이미지
                    AsyncImage(
                        model = currentImageUrl,
                        contentDescription = "문서 이미지",
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(1f)
                            .onGloballyPositioned { coordinates ->
                                imageWidthPx = coordinates.size.width
                                imageHeightPx = coordinates.size.height
                            },
                        contentScale = ContentScale.FillWidth
                    )

                    // 2. 바운딩 박스와 알림 아이콘 오버레이 (알림 표시 여부에 따라 조건부 표시)
                    if (boundingBoxes.isNotEmpty() && imageWidth > 0 && imageHeight > 0 && imageWidthPx > 0) {
                        BoundingBoxOverlay(
                            boundingBoxes = boundingBoxes,
                            originalWidth = imageWidth,
                            originalHeight = imageHeight,
                            displayWidth = imageWidthPx.toFloat(),
                            displayHeight = imageHeightPx.toFloat(),
                            notices = if (showNotices) notices else emptyList()  // 알림 표시 여부에 따라 전달
                        )
                    }
                }

                // 네비게이션 버튼과 알림 정보를 포함하는 Column
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    // 알림 정보 표시 (알림이 있고 표시 설정이 켜져 있는 경우만)
                    if (notices.isNotEmpty() && showNotices) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = "경고 (${notices.size}개)",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                notices.take(2).forEachIndexed { index, notice ->
                                    Text(
                                        text = "[$index] '${notice.notice.take(30)}...'",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                                if (notices.size > 2) {
                                    Text(
                                        text = "그 외 ${notices.size - 2}개...",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // 알림 토글 버튼 행
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 알림 활성화/비활성화 토글 버튼
                        OutlinedButton(
                            onClick = { showNotices = !showNotices },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (showNotices) Color(0xFF4CAF50) else Color.Gray
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (showNotices) Color(0xFF4CAF50) else Color.Gray
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (showNotices)
                                        Icons.Default.Check
                                    else
                                        Icons.Default.Clear,
                                    contentDescription = if (showNotices) "알림 켜짐" else "알림 꺼짐",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (showNotices) "알림 켜짐" else "알림 꺼짐")
                            }
                        }
                    }

                    // 페이지 네비게이션
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                if (currentPageIndex > 0) {
                                    currentPageIndex--
                                } else {
                                    // 첫 페이지에서 이전 문서 타입의 마지막 페이지로 이동
                                    val currentTypeIndex = docTypeKeys.indexOf(currentDocumentType)
                                    if (currentTypeIndex > 0) {
                                        val prevType = docTypeKeys[currentTypeIndex - 1]
                                        currentDocumentType = prevType
                                        currentPageIndex = when (prevType) {
                                            "building_registry" -> contract?.building_registry?.size ?: 0
                                            "registry_document" -> contract?.registry_document?.size ?: 0
                                            "contract" -> contract?.contract?.size ?: 0
                                            else -> 0
                                        } - 1
                                    }
                                }
                            },
                            enabled = currentPageIndex > 0 || docTypeKeys.indexOf(currentDocumentType) > 0,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFF253F5A)
                            )
                        ) {
                            Text("이전 페이지")
                        }

                        // 마지막 페이지에서는 "요약" 버튼 표시, 그렇지 않으면 "다음 페이지" 버튼 표시
                        if (isVeryLastPage) {
                            Button(
                                onClick = {
                                    // AIAnalysisResultActivity로 이동
                                    val intent = Intent(context, AIAnalysisResultActivity::class.java).apply {
                                        putExtra("contractId", contractId)
                                    }
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF253F5A)
                                )
                            ) {
                                Text("요약", color = Color.White)
                            }
                        } else {
                            TextButton(
                                onClick = {
                                    if (currentPageIndex < totalPages - 1) {
                                        currentPageIndex++
                                    } else {
                                        // 마지막 페이지에서 다음 문서 타입의 첫 페이지로 이동
                                        val currentTypeIndex = docTypeKeys.indexOf(currentDocumentType)
                                        if (currentTypeIndex < docTypeKeys.size - 1) {
                                            currentDocumentType = docTypeKeys[currentTypeIndex + 1]
                                            currentPageIndex = 0
                                        }
                                    }
                                },
                                enabled = currentPageIndex < totalPages - 1 || docTypeKeys.indexOf(currentDocumentType) < docTypeKeys.size - 1,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Color(0xFF253F5A)
                                )
                            ) {
                                Text("다음 페이지")
                            }
                        }
                    }
                }
            } else {
                // 이미지가 없는 경우
                Text(
                    "문서가 없습니다",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

// 스크롤바 UI
@Composable
fun CustomScrollbar(
    scrollState: androidx.compose.foundation.ScrollState,
    height: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(4.dp)
            .height(height)
            .padding(vertical = 8.dp)
    ) {
        // 스크롤바 트랙
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
        )

        // 스크롤바 핸들 (최소 높이 설정)
        if (scrollState.maxValue > 0) {
            val scrollRatio = scrollState.value.toFloat() / scrollState.maxValue

            // 컨텐츠 길이에 따른 핸들 높이 계산 (최소값 40dp 보장)
            val contentRatio = minOf(1f, height.value / (height.value + scrollState.maxValue))
            val handleHeight = maxOf(40.dp, (height.value * contentRatio).dp)

            // 스크롤 위치에 따른 오프셋 계산
            val availableSpace = height - handleHeight - 16.dp // 패딩 고려
            val yOffset = (scrollRatio * availableSpace.value).dp + 8.dp // 상단 패딩 추가

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(handleHeight)
                    .offset(y = yOffset)
                    .background(Color.Gray.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
            )
        }
    }
}

// 특약사항 텍스트 처리 함수
fun formatSpecialTermText(text: String, key: String = ""): String {
    // "특약사항" 또는 "특약"이 포함된 키인지 확인
    if ((key.contains("특약사항") || key.contains("특약") || text.contains("특약")) && text.length > 100) {
        // 첫 줄이나 첫 50자까지만 표시
        val firstPart = text.take(50)
        return "$firstPart...(이하생략)"
    }
    return text
}

// 날짜 확인 및 발급일자 관련 알림 생성 함수
suspend fun checkAndCreateDateAlert(
    notices: MutableList<Notice>,
    documentType: String,
    firestoreUtil: FirestoreUtil,
    contractId: String,
    userId: String = "test_user"
) {
    try {
        // Firestore에서 발급일자 데이터 가져오기
        when (documentType) {
            "registry_document" -> {
                // 등기부등본의 경우 "열람일시" 또는 "발급일자" 키를 확인
                val dates = firestoreUtil.getRegistryDocumentDates(userId, contractId)
                if (dates.isNotEmpty()) {
                    // 발급일자 Notice 생성
                    for (dateInfo in dates) {
                        val text = dateInfo.text
                        val key = dateInfo.key
                        val boundingBox = dateInfo.boundingBox

                        // 오늘 날짜와 비교 - 전역 함수로 접근
                        if (!isDateEqualToToday(text)) {
                            val notice = Notice(
                                documentType = "registry_document",
                                boundingBox = boundingBox,
                                notice = "오늘 발급받은 등기부등본이 아닙니다.",
                                text = text,
                                solution = "최신 등기부등본을 확인해야 합니다.",
                                key = key
                            )
                            notices.add(notice)
                        }
                    }
                }
            }
            "building_registry" -> {
                // 건축물대장의 경우 "발급일자" 키를 확인
                val dates = firestoreUtil.getBuildingRegistryDates(userId, contractId)
                if (dates.isNotEmpty()) {
                    // 발급일자 Notice 생성
                    for (dateInfo in dates) {
                        val text = dateInfo.text
                        val boundingBox = dateInfo.boundingBox

                        // 오늘 날짜와 비교 - 전역 함수로 접근
                        if (!isDateEqualToToday(text)) {
                            val notice = Notice(
                                documentType = "building_registry",
                                boundingBox = boundingBox,
                                notice = "오늘 발급받은 건축물대장이 아닙니다.",
                                text = text,
                                solution = "최신 건축물발급대장을 확인해야 합니다.",
                                key = "발급일자"
                            )
                            notices.add(notice)
                        }
                    }
                }
            }
            else -> {
                // 다른 문서 타입은 처리하지 않음
            }
        }
    } catch (e: Exception) {
        println("발급일자 확인 중 오류: ${e.message}")
    }
}

// 날짜가 오늘과 동일한지 확인하는 전역 함수
fun isDateEqualToToday(dateText: String): Boolean {
    try {
        // 현재 날짜 계산
        val currentDate = Calendar.getInstance().time
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDateString = dateFormatter.format(currentDate)

        // 다양한 날짜 포맷 처리
        val formats = listOf(
            "yyyy-MM-dd",
            "yyyy년 MM월 dd일",
            "yyyy년 MM월 d일",
            "yyyy년 M월 d일",
            "yyyy년 M월 dd일",
            "yyyy.MM.dd"
        )

        for (format in formats) {
            try {
                val parser = SimpleDateFormat(format, Locale.getDefault())
                val date = parser.parse(dateText.trim())
                val dateStr = dateFormatter.format(date)
                return dateStr == currentDateString
            } catch (e: Exception) {
                // 파싱 실패시 다음 포맷 시도
                continue
            }
        }

        // 날짜에 "년", "월", "일"이 포함된 경우 직접 파싱 시도
        if (dateText.contains("년") && dateText.contains("월") && dateText.contains("일")) {
            val year = dateText.substringBefore("년").trim()
                .filter { it.isDigit() }.toIntOrNull() ?: return false

            val month = dateText.substringAfter("년").substringBefore("월").trim()
                .filter { it.isDigit() }.toIntOrNull() ?: return false

            val day = dateText.substringAfter("월").substringBefore("일").trim()
                .filter { it.isDigit() }.toIntOrNull() ?: return false

            val cal = Calendar.getInstance()
            cal.time = currentDate

            return year == cal.get(Calendar.YEAR) &&
                    month == cal.get(Calendar.MONTH) + 1 &&
                    day == cal.get(Calendar.DAY_OF_MONTH)
        }

        return false
    } catch (e: Exception) {
        return false
    }
}

@Composable
fun BoundingBoxOverlay(
    boundingBoxes: List<BoundingBox>,
    originalWidth: Float,
    originalHeight: Float,
    displayWidth: Float,
    displayHeight: Float,
    notices: List<Notice> = emptyList()
) {
    val density = LocalDensity.current
    val widthRatio = displayWidth / originalWidth
    val heightRatio = displayHeight / originalHeight
    var expandedNoticeId by remember { mutableStateOf<Int?>(null) }
    val currentDate = remember { Calendar.getInstance().time }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val currentDateString = remember { dateFormatter.format(currentDate) }

    // dp 값들 정의
    val cardWidth = 260.dp
    val horizontalPadding = 16.dp
    val verticalPadding = 16.dp
    val tooltipHeight = 280.dp
    val iconSize = 24.dp

    // dp to pixels 변환 준비
    val cardWidthPx = with(density) { cardWidth.toPx() }
    val horizontalPaddingPx = with(density) { horizontalPadding.toPx() }
    val verticalPaddingPx = with(density) { verticalPadding.toPx() }
    val tooltipHeightPx = with(density) { tooltipHeight.toPx() }
    val iconSizePx = with(density) { iconSize.toPx() }

    // 날짜 형식 확인 및 비교 함수
    fun isDateEqual(dateText: String): Boolean {
        try {
            // 현재 날짜 계산
            val currentDate = Calendar.getInstance().time
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDateString = dateFormatter.format(currentDate)

            // 다양한 날짜 포맷 처리
            val formats = listOf(
                "yyyy-MM-dd",
                "yyyy년 MM월 dd일",
                "yyyy년 MM월 d일",
                "yyyy년 M월 d일",
                "yyyy년 M월 dd일",
                "yyyy.MM.dd"
            )

            for (format in formats) {
                try {
                    val parser = SimpleDateFormat(format, Locale.getDefault())
                    val date = parser.parse(dateText.trim())
                    val dateStr = dateFormatter.format(date)
                    return dateStr == currentDateString
                } catch (e: Exception) {
                    // 파싱 실패시 다음 포맷 시도
                    continue
                }
            }

            // 날짜에 "년", "월", "일"이 포함된 경우 직접 파싱 시도
            if (dateText.contains("년") && dateText.contains("월") && dateText.contains("일")) {
                val year = dateText.substringBefore("년").trim()
                    .filter { it.isDigit() }.toIntOrNull() ?: return false

                val month = dateText.substringAfter("년").substringBefore("월").trim()
                    .filter { it.isDigit() }.toIntOrNull() ?: return false

                val day = dateText.substringAfter("월").substringBefore("일").trim()
                    .filter { it.isDigit() }.toIntOrNull() ?: return false

                val cal = Calendar.getInstance()
                cal.time = currentDate

                return year == cal.get(Calendar.YEAR) &&
                        month == cal.get(Calendar.MONTH) + 1 &&
                        day == cal.get(Calendar.DAY_OF_MONTH)
            }

            return false
        } catch (e: Exception) {
            return false
        }
    }

    // 위험 수준 결정 함수 수정
    fun determineRiskLevel(notice: Notice): RiskLevel {
        // 바운딩 박스가 (0,0,0,0)인 경우 표시 안함
        if (notice.boundingBox.x1 == 0 && notice.boundingBox.y1 == 0 &&
            notice.boundingBox.x2 == 0 && notice.boundingBox.y2 == 0) {
            return RiskLevel.NONE
        }

        // NA 텍스트인 경우 표시 안함
        if (notice.text == "NA") {
            return RiskLevel.NONE
        }

        // 발급일자 관련 키워드가 포함된 경우 - 날짜가 다르면 항상 위험 표시
        if ((notice.key == "발급일자" || notice.key == "열람일시" ||
                    notice.key.contains("발급") || notice.key.contains("열람")) &&
            (notice.documentType == "registry_document" || notice.documentType == "building_registry")) {

            // 노티스 내용이 지정된 경우 (위에서 추가한 발급일자 알림)
            if (notice.notice.contains("오늘 발급받은") ||
                notice.notice.contains("최신") ||
                notice.solution.contains("최신")) {
                return RiskLevel.DANGER
            }

            // 날짜 텍스트만 있는 경우 직접 비교
            val isCurrentDate = isDateEqualToToday(notice.text)
            if (!isCurrentDate) {
                return RiskLevel.DANGER
            }
        }

        // 문제 없음인 경우 표시 안함 (발급일자 제외)
        if (notice.notice == "문제 없음") {
            return RiskLevel.NONE
        }

        // 기본은 경고(주황색)로 시작
        var riskLevel = RiskLevel.WARNING

        // 계약서의 보증금_1 키에 대한 위험 처리
        if (notice.documentType == "contract" && notice.key == "보증금_1" && notice.notice != "문제 없음") {
            return RiskLevel.DANGER
        }

        // 임대인, 소유자, 성명 관련 위험 처리
        if ((notice.documentType == "contract" && notice.key == "임대인") ||
            (notice.documentType == "registry_document" && notice.key.contains("소유자")) ||
            (notice.documentType == "building_registry" && notice.key.contains("성명"))) {
            if (notice.notice != "문제 없음") {
                return RiskLevel.DANGER
            }
        }

        // 등기부등본 특정 키워드 확인 (항상 위험)
        if (notice.documentType == "registry_document") {
            val dangerKeywords = listOf("주택임차권", "신탁", "압류", "가처분", "가압류", "경매개시결정", "가등기")
            for (keyword in dangerKeywords) {
                if (notice.key.contains(keyword)) {
                    return RiskLevel.DANGER
                }
            }
        }

        // 주소 정보 관련 위험 처리
        if ((notice.documentType == "contract" && (notice.key == "소재지" || notice.key == "임차할부분")) ||
            (notice.documentType == "building_registry" && (notice.key == "대지위치" || notice.key == "도로명주소")) ||
            (notice.documentType == "registry_document" && notice.key == "건물주소")) {
            if (notice.notice != "문제 없음") {
                return RiskLevel.DANGER
            }
        }

        return riskLevel
    }

    Box(
        modifier = Modifier
            .size(
                width = with(density) { displayWidth.toDp() },
                height = with(density) { displayHeight.toDp() }
            )
            .zIndex(2f)
    ) {
        // 바운딩 박스 그리기
        boundingBoxes.forEach { bbox ->
            // 화면에 맞게 바운딩 박스 좌표 조정
            val boxX = bbox.x1 * widthRatio
            val boxY = bbox.y1 * heightRatio
            val boxWidth = (bbox.x2 - bbox.x1) * widthRatio
            val boxHeight = (bbox.y2 - bbox.y1) * heightRatio

            // 바운딩 박스 그리기
            if (boxWidth > 0 && boxHeight > 0) {
                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) { boxX.toDp() },
                            y = with(density) { boxY.toDp() }
                        )
                        .size(
                            width = with(density) { boxWidth.toDp() },
                            height = with(density) { boxHeight.toDp() }
                        )
                        .border(2.dp, Color.Blue.copy(alpha = 0.7f))
                )
            }
        }

        // 알림이 있는 바운딩 박스에 경고 아이콘 표시
        notices.forEachIndexed { index, notice ->
            // 위험 수준에 따라 표시 여부와 색상 결정
            val riskLevel = determineRiskLevel(notice)

            // NONE이 아닐 때만 알림 표시
            if (riskLevel != RiskLevel.NONE) {
                val boxX = notice.boundingBox.x1 * widthRatio
                val boxY = notice.boundingBox.y1 * heightRatio

                // 위험 수준에 따른 색상 결정
                val backgroundColor = when (riskLevel) {
                    RiskLevel.DANGER -> Color(0xFFE53935) // 빨간색
                    RiskLevel.WARNING -> Color(0xFFFF9800) // 주황색
                    else -> Color(0xFF4CAF50) // 초록색 (사용되지는 않음)
                }

                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) { (boxX - iconSizePx/2).toDp() },
                            y = with(density) { (boxY - iconSizePx/2).toDp() }
                        )
                        .zIndex(3f) // 경고 아이콘에 더 높은 z-index
                ) {
                    // 경고 아이콘
                    IconButton(
                        onClick = { expandedNoticeId = if (expandedNoticeId == index) null else index },
                        modifier = Modifier
                            .size(iconSize)
                            .background(backgroundColor, CircleShape),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = backgroundColor
                        )
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "경고",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp) // 아이콘 내부 크기 조정
                        )
                    }
                }
            }
        }

        // 배경 오버레이 (툴팁이 표시될 때만) - 더 어두운 배경으로 변경
        if (expandedNoticeId != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(9f) // 툴팁 아래, 나머지 요소 위에
                    .background(Color.Black.copy(alpha = 0.6f)) // 더 어두운 배경으로 변경
                    .clickable { expandedNoticeId = null } // 바깥 클릭시 툴팁 닫기
            )
        }

// 알림 툴팁을 별도의 레이어로 표시 (가장 높은 z-index)
        if (expandedNoticeId != null && expandedNoticeId!! < notices.size) {
            val notice = notices[expandedNoticeId!!]

            // 위험 수준에 따라 표시 여부와 색상 결정
            val riskLevel = determineRiskLevel(notice)

            // NONE이 아닐 때만 툴팁 표시
            if (riskLevel != RiskLevel.NONE) {
                val boxX = notice.boundingBox.x1 * widthRatio
                val boxY = notice.boundingBox.y1 * heightRatio

                // 위험 수준에 따른 색상 결정 (테두리 없음)
                val titleColor = when (riskLevel) {
                    RiskLevel.DANGER -> Color(0xFFE53935) // 빨간색
                    RiskLevel.WARNING -> Color(0xFFFF9800) // 주황색
                    else -> Color(0xFF4CAF50) // 초록색 (사용되지는 않음)
                }

                // 툴팁 위치 계산 개선 - 항상 화면 안에 표시되도록
                val halfScreenWidth = displayWidth / 2
                val halfScreenHeight = displayHeight / 2

                // 화면을 4분면으로 나눠서 처리
                val isRightHalf = boxX > halfScreenWidth
                val isTopHalf = boxY < halfScreenHeight

                // X 좌표 계산 - 화면 안에 들어오도록
                // 기본 좌표 계산
                val rawXOffset = if (isRightHalf) {
                    (boxX - cardWidthPx) // 오른쪽에 있는 경우 왼쪽으로
                } else {
                    boxX // 왼쪽에 있는 경우 오른쪽으로
                }

                // 화면 경계 체크 및 보정
                val safeXOffset = when {
                    rawXOffset < horizontalPaddingPx -> horizontalPaddingPx // 왼쪽 경계 넘어감
                    rawXOffset + cardWidthPx > displayWidth - horizontalPaddingPx ->
                        displayWidth - cardWidthPx - horizontalPaddingPx // 오른쪽 경계 넘어감
                    else -> rawXOffset
                }

                // Y 좌표 계산 - 화면 안에 들어오도록
                // 기본 좌표 계산 (위아래 여유공간 필요)
                val rawYOffset = if (isTopHalf) {
                    boxY // 상단에 있는 경우
                } else {
                    boxY - tooltipHeightPx // 하단에 있는 경우 위로
                }

                // 화면 경계 체크 및 보정
                val safeYOffset = when {
                    rawYOffset < verticalPaddingPx -> verticalPaddingPx // 상단 경계 넘어감
                    rawYOffset + tooltipHeightPx > displayHeight - verticalPaddingPx ->
                        displayHeight - tooltipHeightPx - verticalPaddingPx // 하단 경계 넘어감
                    else -> rawYOffset
                }

                // dp로 변환
                val xOffset = with(density) { safeXOffset.toDp() }
                val yOffset = with(density) { safeYOffset.toDp() }

                // 특약사항 키인지 확인하고 텍스트 처리
                val displayText = formatSpecialTermText(notice.text, notice.key)

                // 카드 위치를 모달로 띄우기 (오버레이)
                Box(modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10f) // 다른 모든 요소 위에 표시
                ) {
                    Card(
                        modifier = Modifier
                            .width(cardWidth)
                            .height(tooltipHeight) // 고정 높이 설정
                            .padding(all = 0.dp)
                            .offset(
                                x = xOffset,
                                y = yOffset
                            )
                            // 툴팁 클릭은 이벤트 전파 중단
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { /* 툴팁 내부 클릭은 무시, 이벤트 전파 중단 */ }
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 8.dp // 더 명확한 구분을 위해 그림자 강화
                        ),
                        shape = RoundedCornerShape(8.dp) // 모서리 둥글게
                    ) {
                        // 내부 레이아웃
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            // 스크롤 상태
                            val scrollState = rememberScrollState()

                            // 스크롤 가능한 컬럼
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(scrollState)
                                    .padding(PaddingValues(top = 16.dp, bottom = 16.dp, end = 12.dp)) // 명시적 PaddingValues 사용
                            ) {
                                // 원본 텍스트 (스크롤 내부에 포함)
                                if (displayText.isNotEmpty() && displayText != "NA") {
                                    Text(
                                        text = "\"${displayText}\"",
                                        color = Color.Black,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }

                                // 알림 유형
                                Text(
                                    text = if (riskLevel == RiskLevel.DANGER) "위험" else "주의",
                                    color = titleColor,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                // 알림 내용
                                Text(
                                    text = notice.notice,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    fontSize = 14.sp
                                )

                                // 해결방법이 있는 경우만 표시
                                if (notice.solution.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "해결 방법",
                                        color = Color(0xFF4CAF50),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = notice.solution,
                                        fontSize = 14.sp
                                    )
                                }

                                // 스크롤 여백 추가
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // 커스텀 스크롤바
                            CustomScrollbar(
                                scrollState = scrollState,
                                height = tooltipHeight - 32.dp,
                                modifier = Modifier.align(Alignment.CenterEnd)
                            )
                        }
                    }
                }
            }
        }
    }
}

// 위험 수준
enum class RiskLevel {
    NONE,      // 표시 안함
    WARNING,   // 주황색 (경고)
    DANGER     // 빨간색 (위험)
}