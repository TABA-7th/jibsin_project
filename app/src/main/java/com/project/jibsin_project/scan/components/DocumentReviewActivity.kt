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

class DocumentReviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contractId = intent.getStringExtra("contractId") ?: return

        setContent {
            DocumentReviewScreen(contractId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentReviewScreen(contractId: String) {
    var currentDocumentIndex by remember { mutableStateOf(0) }
    val firestoreUtil = remember { FirestoreUtil() }
    val documents = listOf("building_registry", "registry_document", "contract")
    var isLoading by remember { mutableStateOf(true) }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var boundingBoxes by remember { mutableStateOf(emptyList<BoundingBox>()) }
    var imageWidth by remember { mutableStateOf(0f) }
    var imageHeight by remember { mutableStateOf(0f) }
    var imageWidthPx by remember { mutableStateOf(0) }
    var imageHeightPx by remember { mutableStateOf(0) }
    val context = LocalContext.current

    // 데이터 로드
    LaunchedEffect(contractId, currentDocumentIndex) {
        isLoading = true
        val documentType = documents[currentDocumentIndex]
        val contract = firestoreUtil.getContract("test_user", contractId)

        when (documentType) {
            "building_registry" -> {
                imageUrl = contract?.building_registry?.firstOrNull()?.imageUrl
                val (boundingBoxList, dimensions) = firestoreUtil.getBuildingRegistryAnalysis("test_user", contractId)
                boundingBoxes = boundingBoxList
                imageWidth = dimensions.first
                imageHeight = dimensions.second
            }
            "registry_document" -> {
                imageUrl = contract?.registry_document?.firstOrNull()?.imageUrl
                val (boundingBoxList, dimensions) = firestoreUtil.getRegistryDocumentAnalysis("test_user", contractId)
                boundingBoxes = boundingBoxList
                imageWidth = dimensions.first
                imageHeight = dimensions.second
            }
            "contract" -> {
                imageUrl = contract?.contract?.firstOrNull()?.imageUrl
                val (boundingBoxList, dimensions) = firestoreUtil.getContractAnalysis("test_user", contractId)
                boundingBoxes = boundingBoxList
                imageWidth = dimensions.first
                imageHeight = dimensions.second
            }
        }

        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (currentDocumentIndex) {
                            0 -> "건축물대장 검토"
                            1 -> "등기부등본 검토"
                            2 -> "계약서 검토"
                            else -> "문서 검토"
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF9F9F9),
                    titleContentColor = Color(0xFF253F5A)
                )
            )
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
            } else if (imageUrl != null) {
                // 이미지와 바운딩 박스
                Box(modifier = Modifier.fillMaxSize()) {
                    // 1. 이미지
                    AsyncImage(
                        model = imageUrl,
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
                            if (currentDocumentIndex > 0) {
                                currentDocumentIndex--
                            }
                        },
                        enabled = currentDocumentIndex > 0
                    ) {
                        Text("이전 페이지")
                    }

                    TextButton(
                        onClick = {
                            if (currentDocumentIndex < documents.size - 1) {
                                currentDocumentIndex++
                            }
                        },
                        enabled = currentDocumentIndex < documents.size - 1
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

private fun extractNoticesFromPage(
    pageData: Map<*, *>,
    documentType: String,
    notices: MutableList<Notice>
) {
    pageData.forEach { (_, sectionData) ->
        if (sectionData is Map<*, *>) {
            val boundingBox = sectionData["bounding_box"] as? Map<*, *>
            val notice = sectionData["notice"] as? String
            val text = sectionData["text"] as? String

            if (boundingBox != null && (!notice.isNullOrEmpty() || !text.isNullOrEmpty())) {
                notices.add(
                    Notice(
                        documentType = documentType,
                        boundingBox = BoundingBox(
                            x1 = (boundingBox["x1"] as? Number)?.toInt() ?: 0,
                            x2 = (boundingBox["x2"] as? Number)?.toInt() ?: 0,
                            y1 = (boundingBox["y1"] as? Number)?.toInt() ?: 0,
                            y2 = (boundingBox["y2"] as? Number)?.toInt() ?: 0
                        ),
                        notice = notice ?: "",
                        text = text ?: "",
                        solution = (sectionData["solution"] as? String) ?: ""
                    )
                )
            }
        }
    }
}