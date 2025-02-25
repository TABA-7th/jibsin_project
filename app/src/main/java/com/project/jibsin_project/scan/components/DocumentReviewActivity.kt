package com.project.jibsin_project.scan.components

import android.app.Activity
import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.project.jibsin_project.scan.AIAnalysisResultActivity
import com.project.jibsin_project.utils.Contract
import com.project.jibsin_project.utils.ErrorDialog
import com.project.jibsin_project.utils.FirestoreUtil
import kotlinx.coroutines.delay
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

    // Firestore 데이터 불러오기 위한 변수들
    val firestoreUtil = remember { FirestoreUtil() }
    var boundingBoxes by remember { mutableStateOf(emptyList<BoundingBox>()) }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var originalWidth by remember { mutableStateOf(1f) }
    var originalHeight by remember { mutableStateOf(1f) }

    // 데이터 불러오기
    LaunchedEffect(contractId) {
        val contract = firestoreUtil.getContract("test_user", contractId)
        imageUrl = contract?.building_registry?.firstOrNull()?.imageUrl

        val (boundingBoxList, imageSize) = firestoreUtil.getBuildingRegistryAnalysis("test_user", contractId)
        boundingBoxes = boundingBoxList
        originalWidth = imageSize.first
        originalHeight = imageSize.second

        println("🔥 데이터 로딩 완료: 바운딩 박스 ${boundingBoxes.size}개")
        println("🔥 이미지 URL: $imageUrl")
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
        ) {
            when (currentDocumentIndex) {
                0 -> {
                    // 건축물대장 화면
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.LightGray)
                    ) {
                        // 레이어 1: 이미지
                        imageUrl?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = "건축물대장",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.FillWidth
                            )
                        }

                        // 레이어 2: 테스트용 빨간 박스
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(Color.Red.copy(alpha = 0.5f))
                                .align(Alignment.Center)
                        )

                        // 레이어 3: 바운딩 박스들
                        boundingBoxes.forEach { bbox ->
                            val scaleX = 0.5f  // 이미지 스케일에 맞게 수정 필요
                            val scaleY = 0.5f  // 이미지 스케일에 맞게 수정 필요

                            val width = (bbox.x2 - bbox.x1) * scaleX
                            val height = (bbox.y2 - bbox.y1) * scaleY

                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = (bbox.x1 * scaleX).dp,
                                        y = (bbox.y1 * scaleY).dp
                                    )
                                    .size(
                                        width = width.dp,
                                        height = height.dp
                                    )
                                    .border(2.dp, Color.Blue.copy(alpha = 0.7f))
                            )
                        }
                    }
                }
                else -> {
                    // 다른 문서 화면 (나중에 구현)
                }
            }
        }
    }
}

// Notice 추출 함수를 public으로 변경
fun extractNoticesFromAnalysisResult(analysisResult: Map<String, Any>): List<Notice> {
    val notices = mutableListOf<Notice>()

    (analysisResult["result"] as? Map<*, *>)?.let { result ->
        // 건축물대장 분석
        (result["building_registry"] as? Map<*, *>)?.forEach { (page, pageData) ->
            if (pageData is Map<*, *>) {
                extractNoticesFromPage(pageData, "building_registry", notices)
            }
        }

        // 등기부등본 분석
        (result["registry_document"] as? Map<*, *>)?.forEach { (page, pageData) ->
            if (pageData is Map<*, *>) {
                extractNoticesFromPage(pageData, "registry_document", notices)
            }
        }

        // 계약서 분석
        (result["contract"] as? Map<*, *>)?.forEach { (page, pageData) ->
            if (pageData is Map<*, *>) {
                extractNoticesFromPage(pageData, "contract", notices)
            }
        }
    }

    return notices
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