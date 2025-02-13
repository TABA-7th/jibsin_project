package com.project.jibsin_project.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.accompanist.pager.*
import com.project.jibsin_project.utils.*
import kotlinx.coroutines.launch

class OnboardingScanActivity : ComponentActivity() {
    private val firebaseStorageUtil = FirebaseStorageUtil()
    private val firestoreUtil = FirestoreUtil()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OnboardingScanScreen(firebaseStorageUtil, firestoreUtil)
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun OnboardingScanScreen(
    firebaseStorageUtil: FirebaseStorageUtil,
    firestoreUtil: FirestoreUtil,
    documentUploadManager: DocumentUploadManager = DocumentUploadManager.getInstance()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showProgress by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val pagerState = rememberPagerState()

    val pages = listOf(
        Triple("building_registry", "건축물대장", "건축물대장을 스캔하여 분석하세요."),
        Triple("registry_document", "등기부등본", "등기부등본을 스캔하여 분석하세요."),
        Triple("contract", "계약서", "계약서를 스캔하여 분석하세요.")
    )

    // 문서 업로드 상태 관찰
    val documentStatus by documentUploadManager.documentStatus.collectAsState()

    // 완료 버튼 클릭 처리
    fun onCompleteClick() {
        if (documentUploadManager.isReadyForAnalysis()) {
            scope.launch {
                try {
                    showProgress = true
                    val groupId = documentUploadManager.getCurrentGroupId()
                    val analysisId = DocumentAnalyzer().startAnalysis(documentStatus)

                    // 분석 결과 화면으로 이동
                    val intent = Intent(context, AIAnalysisResultActivity::class.java).apply {
                        putExtra("analysisId", analysisId)
                        putExtra("groupId", groupId)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    errorMessage = "분석 요청 실패: ${e.message}"
                } finally {
                    showProgress = false
                }
            }
        } else {
            errorMessage = "모든 문서를 하나 이상 업로드해주세요."
        }
    }

    Scaffold(
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 진행 상태 표시
            LinearProgressIndicator(
                progress = (pagerState.currentPage + 1) / 3f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = Color(0xFF253F5A)
            )

            HorizontalPager(
                state = pagerState,
                count = pages.size,
                modifier = Modifier.weight(1f)
            ) { page ->
                val (type, title, description) = pages[page]
                DocumentUploadScreen(
                    documentType = type,
                    title = title,
                    description = description,
                    documentUploadManager = documentUploadManager,
                    firebaseStorageUtil = firebaseStorageUtil,
                    firestoreUtil = firestoreUtil,
                    onUploadComplete = {
                        scope.launch {
                            if (page < pages.size - 1) {
                                pagerState.animateScrollToPage(page + 1)
                            }
                        }
                    }
                )
            }

            // 하단 네비게이션
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 이전 버튼
                if (pagerState.currentPage > 0) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    ) {
                        Text("이전", color = Color(0xFF253F5A))
                    }
                } else {
                    Spacer(Modifier.width(64.dp))
                }

                // 페이지 인디케이터
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { index ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(8.dp)
                                .background(
                                    color = if (pagerState.currentPage == index)
                                        Color(0xFF253F5A)
                                    else
                                        Color(0xFFE0E0E0),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // 다음/완료 버튼
                if (pagerState.currentPage < pages.size - 1) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    ) {
                        Text("다음", color = Color(0xFF253F5A))
                    }
                } else {
                    Button(
                        onClick = { onCompleteClick() },
                        enabled = documentUploadManager.isReadyForAnalysis(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF253F5A),
                            disabledContainerColor = Color(0xFFE0E0E0)
                        )
                    ) {
                        Text(
                            "분석 시작",
                            color = if (documentUploadManager.isReadyForAnalysis())
                                Color.White
                            else
                                Color(0xFF9E9E9E)
                        )
                    }
                }
            }
        }

        // 진행 상태 다이얼로그
        if (showProgress) {
            ProgressDialog()
        }

        // 에러 다이얼로그
        errorMessage?.let { message ->
            ErrorDialog(
                message = message,
                onDismiss = { errorMessage = null }
            )
        }
    }
}

@Composable
fun DocumentUploadScreen(
    documentType: String,
    title: String,
    description: String,
    documentUploadManager: DocumentUploadManager,
    firebaseStorageUtil: FirebaseStorageUtil,
    firestoreUtil: FirestoreUtil,
    onUploadComplete: () -> Unit
) {
    var uploadedDocuments by remember { mutableStateOf<List<DocumentPreview>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 이미지 선택 런처
    val pickMultipleImages = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                isLoading = true
                try {
                    val groupId = documentUploadManager.getCurrentGroupId()
                    val newDocuments = uris.mapIndexed { index, uri ->
                        val bitmap = context.contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it)
                        } ?: throw IllegalStateException("Failed to read image")

                        val imageUrl = firebaseStorageUtil.uploadScannedImage(
                            bitmap,
                            documentType,
                            groupId,
                            uploadedDocuments.size + index + 1
                        )

                        val document = ScannedDocument(
                            type = documentType,
                            imageUrl = imageUrl,
                            userId = "test_user", // TODO: 실제 사용자 ID로 교체
                            groupId = groupId,
                            pageNumber = uploadedDocuments.size + index + 1
                        )

                        val documentId = firestoreUtil.saveScannedDocument(document)
                        DocumentPreview(documentId, imageUrl, uploadedDocuments.size + index + 1)
                    }

                    uploadedDocuments = uploadedDocuments + newDocuments
                    documentUploadManager.updateDocuments(
                        documentType,
                        uploadedDocuments.map { it.id }
                    )
                } catch (e: Exception) {
                    errorMessage = "업로드 실패: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 문서 타입 제목
        Text(
            text = title,
            fontSize = 24.sp,
            color = Color(0xFF253F5A),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 설명
        Text(
            text = description,
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 업로드된 문서 목록
        if (uploadedDocuments.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uploadedDocuments) { document ->
                    DocumentPreviewItem(
                        document = document,
                        onDelete = {
                            uploadedDocuments = uploadedDocuments - document
                            documentUploadManager.updateDocuments(
                                documentType,
                                uploadedDocuments.map { it.id }
                            )
                        }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("업로드된 문서가 없습니다", color = Color.Gray)
            }
        }

        // 업로드 버튼
        Button(
            onClick = { pickMultipleImages.launch("image/*") },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF253F5A)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("문서 추가하기", color = Color.White)
        }

        // 완료 버튼
        if (uploadedDocuments.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onUploadComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("완료", color = Color.White)
            }
        }
    }

    // 로딩 인디케이터
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = Color(0xFF253F5A)
            )
        }
    }

    // 에러 메시지
    errorMessage?.let { message ->
        ErrorDialog(
            message = message,
            onDismiss = { errorMessage = null }
        )
    }
}

@Composable
fun DocumentPreviewItem(
    document: DocumentPreview,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(0.7f)
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
    ) {
        // 문서 미리보기 이미지
        AsyncImage(
            model = document.imageUrl,
            contentDescription = "Document preview",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        // 페이지 번호
        Text(
            text = "P.${document.pageNumber}",
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(
                    color = Color(0x88000000),
                    shape = RoundedCornerShape(bottomEnd = 8.dp)
                )
                .padding(4.dp),
            color = Color.White,
            fontSize = 12.sp
        )

        // 삭제 버튼
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .background(
                    color = Color(0x88000000),
                    shape = CircleShape
                )
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Delete",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

data class DocumentPreview(
    val id: String,
    val imageUrl: String,
    val pageNumber: Int
)

@Preview(showBackground = true)
@Composable
fun PreviewOnboardingScanScreen() {
    OnboardingScanScreen(
        firebaseStorageUtil = FirebaseStorageUtil(),
        firestoreUtil = FirestoreUtil()
    )
}