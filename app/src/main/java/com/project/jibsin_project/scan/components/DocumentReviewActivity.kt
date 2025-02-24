package com.project.jibsin_project.scan.components

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
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
    var contract by remember { mutableStateOf<Contract?>(null) }
    var analysisResult by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentDocumentIndex by remember { mutableStateOf(0) }
    val firestoreUtil = remember { FirestoreUtil() }
    val context = LocalContext.current

    // 문서 타입 리스트
    val documentTypes = listOf("building_registry", "registry_document", "contract")

    when (documentTypes[currentDocumentIndex]) {
        "building_registry" -> BuildingRegistryScreen(contractId)
        //"registry_document" -> RegistryDocumentScreen(contractId)  // 등기부등본 검토 화면 (추후 구현)
        //"contract" -> ContractReviewScreen(contractId)  // 계약서 검토 화면 (추후 구현)
    }

    LaunchedEffect(contractId) {
        try {
            isLoading = true

            // 분석 결과가 나올 때까지 주기적으로 확인
            while (true) {
                val currentContract = firestoreUtil.getContract("test_user", contractId)
                if (currentContract?.analysisStatus == "completed") {
                    // 로그 추가
                    println("=== Document URLs ===")
                    println("Building Registry: ${currentContract.building_registry.map { it.imageUrl }}")
                    println("Registry Document: ${currentContract.registry_document.map { it.imageUrl }}")
                    println("Contract: ${currentContract.contract.map { it.imageUrl }}")
                    println("==================")

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
                    // 현재 문서 미리보기
                    contract?.let { currentContract ->
                        val currentDocType = documentTypes[currentDocumentIndex]
                        val documents = when (currentDocType) {
                            "building_registry" -> currentContract.building_registry
                            "registry_document" -> currentContract.registry_document
                            "contract" -> currentContract.contract
                            else -> listOf()
                        }

                        // 문서 정보 로그
                        println("=== Current Document ===")
                        println("Type: $currentDocType")
                        println("Documents: $documents")
                        println("==================")

                        documents.firstOrNull()?.let { doc ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                DocumentPreviewWithNotices(
                                    imageUrl = doc.imageUrl,
                                    notices = extractNoticesFromAnalysisResult(analysisResult ?: mapOf()).filter {
                                        it.documentType == currentDocType
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // 네비게이션 버튼
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = {
                                    if (currentDocumentIndex > 0) currentDocumentIndex--
                                },
                                enabled = currentDocumentIndex > 0
                            ) {
                                Text("이전 페이지")
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
                                Text(if (currentDocumentIndex < documentTypes.size - 1) "다음 페이지" else "완료")
                            }
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