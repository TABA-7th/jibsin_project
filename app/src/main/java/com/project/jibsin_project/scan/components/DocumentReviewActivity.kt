package com.project.jibsin_project.scan.components

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.project.jibsin_project.utils.FirestoreUtil
import com.project.jibsin_project.utils.BoundingBox
import com.project.jibsin_project.utils.Contract

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
    var imageWidth by remember { mutableStateOf(0f) }
    var imageHeight by remember { mutableStateOf(0f) }
    var imageWidthPx by remember { mutableStateOf(0) }
    var imageHeightPx by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val docTypeTabItems = listOf("건축물대장", "등기부등본", "계약서")
    val docTypeKeys = listOf("building_registry", "registry_document", "contract")

    // 계약 데이터 로드
    LaunchedEffect(contractId) {
        isLoading = true
        contract = firestoreUtil.getContract("test_user", contractId)
        isLoading = false
    }

    // 현재 문서 및 페이지에 대한 바운딩 박스 로드
    LaunchedEffect(contractId, currentDocumentType, currentPageIndex) {
        isLoading = true

        // 현재 문서 타입에 따라 분석 데이터 가져오기
        val (boxes, dimensions) = when (currentDocumentType) {
            "building_registry" -> firestoreUtil.getBuildingRegistryAnalysis("test_user", contractId, currentPageIndex + 1)
            "registry_document" -> firestoreUtil.getRegistryDocumentAnalysis("test_user", contractId, currentPageIndex + 1)
            "contract" -> firestoreUtil.getContractAnalysis("test_user", contractId, currentPageIndex + 1)
            else -> Pair(emptyList(), Pair(1f, 1f))
        }

        boundingBoxes = boxes
        imageWidth = dimensions.first
        imageHeight = dimensions.second
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

                    // 2. 바운딩 박스 오버레이
                    if (boundingBoxes.isNotEmpty() && imageWidth > 0 && imageHeight > 0 && imageWidthPx > 0) {
                        BoundingBoxOverlay(
                            boundingBoxes = boundingBoxes,
                            originalWidth = imageWidth,
                            originalHeight = imageHeight,
                            displayWidth = imageWidthPx.toFloat(),
                            displayHeight = imageHeightPx.toFloat()
                        )
                    }
                }

                // 네비게이션 버튼
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
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

@Composable
fun BoundingBoxOverlay(
    boundingBoxes: List<BoundingBox>,
    originalWidth: Float,
    originalHeight: Float,
    displayWidth: Float,
    displayHeight: Float
) {
    val density = LocalDensity.current
    val widthRatio = displayWidth / originalWidth
    val heightRatio = displayHeight / originalHeight

    Box(
        modifier = Modifier
            .size(
                width = with(density) { displayWidth.toDp() },
                height = with(density) { displayHeight.toDp() }
            )
            .zIndex(2f)
    ) {
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
    }
}