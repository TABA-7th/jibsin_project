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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.accompanist.pager.*
import com.project.jibsin_project.utils.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.geometry.Offset
import kotlin.math.roundToInt
import androidx.compose.ui.draw.shadow

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
        Triple("building_registry", "건축물대장", "건축물대장을 업로드하세요."),
        Triple("registry_document", "등기부등본", "등기부등본을 업로드하세요."),
        Triple("contract", "계약서", "계약서를 업로드하세요.")
    )

    // 문서 업로드 상태 관찰
    val documentStatus by documentUploadManager.documentStatus.collectAsState()

    // 각 페이지별 문서 업로드 여부 확인
    val hasDocuments = remember(documentStatus) {
        documentStatus.documentSets.mapValues { it.value.isNotEmpty() }
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
                    onUploadComplete = {}  // 완료 버튼 제거로 인해 빈 람다로 설정
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
                // 이전 버튼 - 첫 페이지가 아닐 때만 표시
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
                    // 첫 페이지일 때는 빈 공간
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

                // 다음/분석 버튼
                if (pagerState.currentPage < pages.size - 1) {
                    TextButton(
                        onClick = {
                            if (hasDocuments[pages[pagerState.currentPage].first] == true) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        enabled = hasDocuments[pages[pagerState.currentPage].first] == true
                    ) {
                        Text(
                            "다음",
                            color = if (hasDocuments[pages[pagerState.currentPage].first] == true)
                                Color(0xFF253F5A)
                            else
                                Color(0xFFBDBDBD)
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            if (documentUploadManager.isReadyForAnalysis()) {
                                scope.launch {
                                    try {
                                        showProgress = true
                                        val groupId = documentUploadManager.getCurrentGroupId()
                                        val analysisId = DocumentAnalyzer().startAnalysis(documentStatus)
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
                            }
                        },
                        enabled = documentUploadManager.isReadyForAnalysis(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF253F5A),
                            disabledContainerColor = Color(0xFFE0E0E0)
                        )
                    ) {
                        Text(
                            "분석",
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
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var draggedItem by remember { mutableStateOf<DocumentPreview?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    var draggingItem by remember { mutableStateOf<DocumentPreview?>(null) }
    var dropTarget by remember { mutableStateOf<DocumentPreview?>(null) }
    val density = LocalDensity.current
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    // 문서 상태 관찰
    val documentStatus by documentUploadManager.documentStatus.collectAsState()
    val uploadedDocuments = remember(documentStatus) {
        val documents = documentStatus.documentSets[documentType] ?: emptyList()
        documents.mapIndexed { index, id ->
            try {
                val doc = runBlocking {
                    firestoreUtil.getDocument(id)
                }
                DocumentPreview(
                    id = id,
                    imageUrl = doc?.getString("imageUrl") ?: "",
                    pageNumber = index + 1
                )
            } catch (e: Exception) {
                null
            }
        }.filterNotNull()
    }

    // 카메라 실행 런처
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            coroutineScope.launch {
                isLoading = true
                try {
                    val groupId = documentUploadManager.getCurrentGroupId()
                    val imageUrl = firebaseStorageUtil.uploadScannedImage(
                        bitmap = bitmap,
                        documentType = documentType,
                        groupId = groupId,
                        pageNumber = uploadedDocuments.size + 1
                    )

                    val document = ScannedDocument(
                        type = documentType,
                        imageUrl = imageUrl,
                        userId = "test_user",
                        groupId = groupId,
                        pageNumber = uploadedDocuments.size + 1
                    )

                    val documentId = firestoreUtil.saveScannedDocument(document)
                    documentUploadManager.addDocument(documentType, documentId)
                } catch (e: Exception) {
                    errorMessage = "업로드 실패: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    // 카메라 권한 체크
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            takePictureLauncher.launch(null)
        }
    }

    // 갤러리 실행 런처
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                isLoading = true
                try {
                    val groupId = documentUploadManager.getCurrentGroupId()
                    uris.forEachIndexed { index, uri ->
                        val imageUrl = firebaseStorageUtil.uploadScannedImageFromUri(
                            uri = uri,
                            context = context,
                            documentType = documentType,
                            groupId = groupId,
                            pageNumber = uploadedDocuments.size + index + 1
                        )

                        val document = ScannedDocument(
                            type = documentType,
                            imageUrl = imageUrl,
                            userId = "test_user",
                            groupId = groupId,
                            pageNumber = uploadedDocuments.size + index + 1
                        )

                        val documentId = firestoreUtil.saveScannedDocument(document)
                        documentUploadManager.addDocument(documentType, documentId)
                    }
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
                state = gridState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uploadedDocuments,
                    key = { it.id }
                ) { document ->
                    val isDragging = draggingItem?.id == document.id
                    val isDropTarget = dropTarget?.id == document.id

                    Box(
                        modifier = Modifier
                            .offset {
                                if (isDragging) {
                                    IntOffset(
                                        dragOffset.x.roundToInt(),
                                        dragOffset.y.roundToInt()
                                    )
                                } else {
                                    IntOffset.Zero
                                }
                            }
                    ) {
                        DocumentPreviewItem(
                            document = document,
                            onDelete = {
                                coroutineScope.launch {
                                    try {
                                        documentUploadManager.removeDocumentWithStorage(
                                            documentType,
                                            document.id,
                                            firebaseStorageUtil,
                                            firestoreUtil
                                        )
                                    } catch (e: Exception) {
                                        errorMessage = "문서 삭제 실패: ${e.message}"
                                    }
                                }
                            },
                            onDragStart = { draggingItem = document },
                            onDragEnd = {
                                dropTarget?.let { target ->
                                    val fromIndex = uploadedDocuments.indexOfFirst { it.id == draggingItem?.id }
                                    val toIndex = uploadedDocuments.indexOfFirst { it.id == target.id }
                                    if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
                                        coroutineScope.launch {
                                            try {
                                                documentUploadManager.reorderDocuments(
                                                    documentType,
                                                    fromIndex,
                                                    toIndex,
                                                    firebaseStorageUtil,
                                                    firestoreUtil
                                                )
                                            } catch (e: Exception) {
                                                errorMessage = "문서 순서 변경 실패: ${e.message}"
                                            }
                                        }
                                    }
                                }
                                draggingItem = null
                                dropTarget = null
                                dragOffset = Offset.Zero
                            },
                            onDragCancel = {
                                draggingItem = null
                                dropTarget = null
                                dragOffset = Offset.Zero
                            },
                            onPositionChanged = { x, y ->
                                dragOffset = Offset(x, y)
                                val hitDocument = uploadedDocuments.firstOrNull { item ->
                                    val position = gridState.layoutInfo.visibleItemsInfo.firstOrNull {
                                        it.key == item.id
                                    }
                                    position != null && x >= position.offset.x &&
                                            x <= position.offset.x + position.size.width &&
                                            y >= position.offset.y &&
                                            y <= position.offset.y + position.size.height
                                }
                                dropTarget = hitDocument
                            },
                            modifier = Modifier.zIndex(if (isDragging) 1f else 0f),
                            isDragging = isDragging
                        )
                    }
                }
            }
        }else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("업로드된 문서가 없습니다", color = Color.Gray)
            }
        }

        // 문서 추가 버튼들
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 카메라 버튼
            Button(
                onClick = {
                    when {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            takePictureLauncher.launch(null)
                        }
                        else -> {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF253F5A)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    Icons.Default.Camera,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Text("카메라", color = Color.White)
            }

            // 갤러리 버튼
            Button(
                onClick = { pickImageLauncher.launch("image/*") },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF253F5A)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Text("갤러리", color = Color.White)
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
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onPositionChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false
) {
    Box(
        modifier = modifier
            .aspectRatio(0.7f)
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragCancel() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onPositionChanged(change.position.x, change.position.y)
                    }
                )
            }
            .then(
                if (isDragging) {
                    Modifier
                        .alpha(0.7f)
                        .shadow(8.dp, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            )
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
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            color = Color.White,
            fontSize = 12.sp
        )

        // 삭제 버튼
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Delete",
                tint = Color.Black,
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