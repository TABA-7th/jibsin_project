package com.project.jibsin_project.scan.components

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.project.jibsin_project.scan.AIAnalysisResultActivity
import com.project.jibsin_project.utils.Contract
import com.project.jibsin_project.utils.ErrorDialog
import com.project.jibsin_project.utils.FirestoreUtil
import kotlinx.coroutines.delay
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement

class DocumentReviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contractId = intent.getStringExtra("contractId") ?: return

        setContent {
            DocumentReviewScreen(contractId)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DocumentReviewScreen(contractId: String) {
    var contract by remember { mutableStateOf<Contract?>(null) }
    var analysisResult by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentDocumentIndex by remember { mutableStateOf(0) }
    val firestoreUtil = remember { FirestoreUtil() }
    val context = LocalContext.current

    val documentTypes = listOf("건축물대장", "등기부등본", "계약서")

    LaunchedEffect(contractId) {
        try {
            isLoading = true
            while (true) {
                val currentContract = firestoreUtil.getContract("test_user", contractId)
                if (currentContract?.analysisStatus == "completed") {
                    contract = currentContract
                    analysisResult = currentContract.analysisResult
                    break
                }
                delay(2000)
            }
            isLoading = false
        } catch (e: Exception) {
            errorMessage = "분석 결과를 불러오는 중 오류가 발생했습니다: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(documentTypes[currentDocumentIndex]) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFF9F9F9),
                        titleContentColor = Color(0xFF253F5A)
                    )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    documentTypes.forEachIndexed { index, title ->
                        Button(
                            onClick = { currentDocumentIndex = index },
                            enabled = currentDocumentIndex != index
                        ) {
                            Text(title)
                        }
                    }
                }
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = { if (currentDocumentIndex > 0) currentDocumentIndex-- },
                    enabled = currentDocumentIndex > 0
                ) {
                    Text("이전 문서")
                }

                TextButton(
                    onClick = {
                        if (currentDocumentIndex < documentTypes.size - 1) {
                            currentDocumentIndex++
                        } else {
                            val intent = Intent(context, AIAnalysisResultActivity::class.java).apply {
                                putExtra("contractId", contractId)
                            }
                            context.startActivity(intent)
                            (context as? Activity)?.finish()
                        }
                    }
                ) {
                    Text(if (currentDocumentIndex < documentTypes.size - 1) "다음 문서" else "완료")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF253F5A)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    contract?.let { currentContract ->
                        val currentDocType = when (currentDocumentIndex) {
                            0 -> currentContract.building_registry
                            1 -> currentContract.registry_document
                            2 -> currentContract.contract
                            else -> listOf()
                        }

                        if (currentDocType.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(currentDocType) { doc ->
                                    DocumentPreviewWithNotices(
                                        imageUrl = doc.imageUrl,
                                        notices = extractNoticesFromAnalysisResult(analysisResult ?: mapOf()).filter {
                                            it.documentType == documentTypes[currentDocumentIndex]
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(500.dp)
                                    )
                                }
                            }
                        } else {
                            Text("문서가 없습니다.", modifier = Modifier.align(Alignment.CenterHorizontally))
                        }
                    }
                }
            }

            errorMessage?.let { message ->
                ErrorDialog(
                    message = message,
                    onDismiss = { errorMessage = null }
                )
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