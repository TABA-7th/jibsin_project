package com.project.jibsin_project.scan.components

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.project.jibsin_project.utils.FirestoreUtil
import com.project.jibsin_project.utils.BoundingBox
import com.project.jibsin_project.utils.Contract
import com.project.jibsin_project.utils.DocumentAnalyzer

class DocumentReviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contractId = intent.getStringExtra("contractId") ?: return

        setContent {
            MultiPageDocumentReviewScreen(contractId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiPageDocumentReviewScreen(contractId: String) {
    val firestoreUtil = remember { FirestoreUtil() }
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
    val context = LocalContext.current
    val docTypeTabItems = listOf("건축물대장", "등기부등본", "계약서")
    val docTypeKeys = listOf("building_registry", "registry_document", "contract")
    var showNotices by remember { mutableStateOf(true) } // 알림 표시 여부 상태

    // 테스트용 데이터 생성 함수 - private 키워드 제거
    fun createTestNotices(documentType: String, count: Int): List<Notice> {
        val result = mutableListOf<Notice>()
        for (i in 0 until count) {
            // 바운딩 박스를 문서 내 다양한 위치에 분산
            val x1 = (i * 100) % 1000
            val y1 = 100 + (i * 150) % 1000
            result.add(
                Notice(
                    documentType = documentType,
                    boundingBox = BoundingBox(
                        x1 = x1,
                        y1 = y1,
                        x2 = x1 + 200,
                        y2 = y1 + 50
                    ),
                    notice = "테스트 알림 내용 #$i - 주의해야 할 내용입니다.",
                    text = "원본 텍스트 #$i",
                    solution = "해결 방법 #$i"
                )
            )
        }
        return result
    }

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

            println("로드된 바운딩 박스 수: ${boundingBoxes.size}")

            // 2. 알림 데이터 직접 가져오기
            notices = firestoreUtil.getAIAnalysisNotices(
                userId = "test_user",
                contractId = contractId,
                documentType = currentDocumentType,
                pageNumber = currentPageIndex + 1
            )

            println("가져온 알림 수: ${notices.size}")
            if (notices.isNotEmpty()) {
                notices.forEach { notice ->
                    println("  - notice: ${notice.notice}")
                    println("  - boundingBox: (${notice.boundingBox.x1}, ${notice.boundingBox.y1}, ${notice.boundingBox.x2}, ${notice.boundingBox.y2})")
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

private fun extractNotices(analysisResult: Map<String, Any>): List<Notice> {
    val notices = mutableListOf<Notice>()

    try {
        println("분석 결과 키: ${analysisResult.keys.joinToString()}")

        // 1. analysisResult는 다양한 형태로 제공될 수 있음
        // 방법 1: 직접 result 필드 확인
        val resultFromField = analysisResult["result"] as? Map<*, *>

        // 방법 2: analysisResult 자체가 result일 수도 있음
        // 방법 3: analysisResult에 다른 분석 결과 필드가 있을 수 있음

        // 모든 가능한 데이터 소스 확인
        val dataSources = listOfNotNull(
            resultFromField,                    // result 필드
            analysisResult,                     // 전체 결과
            analysisResult["analysisResult"] as? Map<*, *>,  // analysisResult 필드
            analysisResult["data"] as? Map<*, *>,            // data 필드
            analysisResult["analysis"] as? Map<*, *>         // analysis 필드
        )

        // 각 데이터 소스에서 문서 타입별 데이터 확인
        for (source in dataSources) {
            println("데이터 소스 키: ${source.keys.joinToString()}")

            // 1. 건축물대장
            extractDocumentData(source, "building_registry", notices)

            // 2. 등기부등본
            extractDocumentData(source, "registry_document", notices)

            // 3. 계약서
            extractDocumentData(source, "contract", notices)
        }

        println("추출된 전체 알림 수: ${notices.size}")
    } catch (e: Exception) {
        println("알림 추출 중 오류 발생: ${e.message}")
        e.printStackTrace()
    }

    return notices
}

// 문서 타입별 데이터 추출
private fun extractDocumentData(
    source: Map<*, *>,
    documentType: String,
    notices: MutableList<Notice>
) {
    try {
        // 1. 직접 문서 타입 키 확인
        val documentData = source[documentType] as? Map<*, *>
        if (documentData != null) {
            println("$documentType 데이터 있음 (케이스 1)")
            // 페이지 데이터 추출
            documentData.forEach { (page, pageData) ->
                if (pageData is Map<*, *>) {
                    extractNoticesFromPage(pageData, documentType, notices)
                }
            }
            return
        }

        // 2. notices 또는 warnings 키로 직접 알림 데이터 확인
        val directNotices = source["notices"] as? Map<*, *>
            ?: source["warnings"] as? Map<*, *>
            ?: source["alerts"] as? Map<*, *>

        if (directNotices != null) {
            println("직접 알림 데이터 있음 (케이스 2)")
            val typeNotices = directNotices[documentType] as? List<*>
            if (typeNotices != null) {
                typeNotices.forEach { item ->
                    if (item is Map<*, *>) {
                        val bbox = item["bounding_box"] as? Map<*, *>
                        val notice = item["message"] as? String
                            ?: item["notice"] as? String
                            ?: item["text"] as? String
                            ?: ""
                        val solution = item["solution"] as? String ?: ""

                        if (bbox != null) {
                            notices.add(
                                Notice(
                                    documentType = documentType,
                                    boundingBox = BoundingBox(
                                        x1 = (bbox["x1"] as? Number)?.toInt() ?: 0,
                                        x2 = (bbox["x2"] as? Number)?.toInt() ?: 0,
                                        y1 = (bbox["y1"] as? Number)?.toInt() ?: 0,
                                        y2 = (bbox["y2"] as? Number)?.toInt() ?: 0
                                    ),
                                    notice = notice,
                                    text = item["original_text"] as? String ?: "",
                                    solution = solution
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. 각 문서 타입의 페이지 내에 alerts, notices, warnings 키 확인
        source.forEach { (key, value) ->
            if (value is Map<*, *> && key.toString().contains(documentType, ignoreCase = true)) {
                println("$key 키에서 $documentType 관련 데이터 확인 (케이스 3)")

                // 페이지 데이터 순회
                value.forEach { (_, pageData) ->
                    if (pageData is Map<*, *>) {
                        val pageNotices = pageData["notices"] as? List<*>
                            ?: pageData["warnings"] as? List<*>
                            ?: pageData["alerts"] as? List<*>

                        if (pageNotices != null) {
                            pageNotices.forEach { item ->
                                if (item is Map<*, *>) {
                                    val bbox = item["bounding_box"] as? Map<*, *>
                                    val notice = item["message"] as? String
                                        ?: item["notice"] as? String
                                        ?: item["text"] as? String
                                        ?: ""
                                    val solution = item["solution"] as? String ?: ""

                                    if (bbox != null) {
                                        notices.add(
                                            Notice(
                                                documentType = documentType,
                                                boundingBox = BoundingBox(
                                                    x1 = (bbox["x1"] as? Number)?.toInt() ?: 0,
                                                    x2 = (bbox["x2"] as? Number)?.toInt() ?: 0,
                                                    y1 = (bbox["y1"] as? Number)?.toInt() ?: 0,
                                                    y2 = (bbox["y2"] as? Number)?.toInt() ?: 0
                                                ),
                                                notice = notice,
                                                text = item["original_text"] as? String ?: "",
                                                solution = solution
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        println("$documentType 데이터 추출 중 오류: ${e.message}")
    }
}

private fun extractNoticesFromPage(
    pageData: Map<*, *>,
    documentType: String,
    notices: MutableList<Notice>
) {
    try {
        // 1. 일반적인 섹션 데이터 처리
        pageData.forEach { (key, sectionData) ->
            if (sectionData is Map<*, *> && key.toString() != "image_dimensions") {
                // 1.1 바운딩 박스와 알림 직접 확인
                val boundingBox = sectionData["bounding_box"] as? Map<*, *>
                val noticeText = sectionData["notice"] as? String
                    ?: sectionData["warning"] as? String
                    ?: sectionData["alert"] as? String
                    ?: sectionData["message"] as? String
                val text = sectionData["text"] as? String
                    ?: sectionData["original_text"] as? String
                    ?: ""

                // 바운딩 박스가 있고, 알림이나 텍스트가 있는 경우 알림 추가
                if (boundingBox != null && (!noticeText.isNullOrEmpty() || !text.isNullOrEmpty())) {
                    notices.add(
                        Notice(
                            documentType = documentType,
                            boundingBox = BoundingBox(
                                x1 = (boundingBox["x1"] as? Number)?.toInt() ?: 0,
                                x2 = (boundingBox["x2"] as? Number)?.toInt() ?: 0,
                                y1 = (boundingBox["y1"] as? Number)?.toInt() ?: 0,
                                y2 = (boundingBox["y2"] as? Number)?.toInt() ?: 0
                            ),
                            notice = noticeText ?: "",
                            text = text,
                            solution = (sectionData["solution"] as? String) ?: ""
                        )
                    )
                }

                // 1.2 해당 섹션에 notices 또는 warnings 배열이 있는지 확인
                val sectionNotices = sectionData["notices"] as? List<*>
                    ?: sectionData["warnings"] as? List<*>
                    ?: sectionData["alerts"] as? List<*>

                if (sectionNotices != null) {
                    sectionNotices.forEach { item ->
                        if (item is Map<*, *>) {
                            val bbox = item["bounding_box"] as? Map<*, *> ?: boundingBox
                            val notice = item["message"] as? String
                                ?: item["notice"] as? String
                                ?: item["text"] as? String
                                ?: ""

                            if (bbox != null) {
                                notices.add(
                                    Notice(
                                        documentType = documentType,
                                        boundingBox = BoundingBox(
                                            x1 = (bbox["x1"] as? Number)?.toInt() ?: 0,
                                            x2 = (bbox["x2"] as? Number)?.toInt() ?: 0,
                                            y1 = (bbox["y1"] as? Number)?.toInt() ?: 0,
                                            y2 = (bbox["y2"] as? Number)?.toInt() ?: 0
                                        ),
                                        notice = notice,
                                        text = item["original_text"] as? String ?: "",
                                        solution = (item["solution"] as? String) ?: ""
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. 페이지 전체에 대한 notices 또는 warnings 확인
        val pageNotices = pageData["notices"] as? List<*>
            ?: pageData["warnings"] as? List<*>
            ?: pageData["alerts"] as? List<*>

        if (pageNotices != null) {
            pageNotices.forEach { item ->
                if (item is Map<*, *>) {
                    val bbox = item["bounding_box"] as? Map<*, *>
                    val notice = item["message"] as? String
                        ?: item["notice"] as? String
                        ?: item["text"] as? String
                        ?: ""

                    if (bbox != null) {
                        notices.add(
                            Notice(
                                documentType = documentType,
                                boundingBox = BoundingBox(
                                    x1 = (bbox["x1"] as? Number)?.toInt() ?: 0,
                                    x2 = (bbox["x2"] as? Number)?.toInt() ?: 0,
                                    y1 = (bbox["y1"] as? Number)?.toInt() ?: 0,
                                    y2 = (bbox["y2"] as? Number)?.toInt() ?: 0
                                ),
                                notice = notice,
                                text = item["original_text"] as? String ?: "",
                                solution = (item["solution"] as? String) ?: ""
                            )
                        )
                    }
                }
            }
        }
    } catch (e: Exception) {
        println("페이지 알림 추출 중 오류 발생: ${e.message}")
        e.printStackTrace()
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

    // 디버깅을 위한 정보 출력
    LaunchedEffect(notices, boundingBoxes) {
        println("BoundingBoxOverlay - 전달된 notices 수: ${notices.size}")
        println("BoundingBoxOverlay - 바운딩 박스 수: ${boundingBoxes.size}")
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
            // 알림 내용이 있는 경우에만 아이콘 표시
            if (!notice.notice.isNullOrEmpty()) {
                val boxX = notice.boundingBox.x1 * widthRatio
                val boxY = notice.boundingBox.y1 * heightRatio

                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) { boxX.toDp() - 12.dp },
                            y = with(density) { boxY.toDp() - 12.dp }
                        )
                ) {
                    // 경고 아이콘
                    IconButton(
                        onClick = { expandedNoticeId = if (expandedNoticeId == index) null else index },
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFFFF9800), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "경고",
                            tint = Color.White
                        )
                    }

                    // 알림 내용 툴팁 (클릭 시 표시)
                    if (expandedNoticeId == index) {
                        val cardWidth = 240.dp
                        // 카드가 화면 바깥으로 나가지 않도록 위치 조정
                        val isRightSide = boxX > displayWidth / 2

                        Card(
                            modifier = Modifier
                                .width(cardWidth)
                                .padding(top = 32.dp)
                                .align(if (isRightSide) Alignment.TopEnd else Alignment.TopStart)
                                .offset(
                                    x = if (isRightSide) (-cardWidth + 24.dp) else 0.dp
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 4.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                // 원본 텍스트
                                if (notice.text.isNotEmpty()) {
                                    Text(
                                        text = "\"${notice.text}\"",
                                        color = Color.Black,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }

                                // 알림 내용
                                Text(
                                    text = "주의",
                                    color = Color(0xFFFF9800),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = notice.notice,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    fontSize = 14.sp
                                )

                                // 해결방법이 있는 경우만 표시
                                if (notice.solution.isNotEmpty()) {
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
                            }
                        }
                    }
                }
            }
        }
    }
}