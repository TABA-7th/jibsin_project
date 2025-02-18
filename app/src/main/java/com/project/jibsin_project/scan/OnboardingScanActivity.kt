package com.project.jibsin_project.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.accompanist.pager.*
import com.project.jibsin_project.utils.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

class OnboardingScanActivity : ComponentActivity() {
    private val contractManager = ContractManager()
    private val firebaseStorageUtil = FirebaseStorageUtil()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OnboardingScanScreen(contractManager, firebaseStorageUtil)
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun OnboardingScanScreen(
    contractManager: ContractManager,
    firebaseStorageUtil: FirebaseStorageUtil,
    documentUploadManager: DocumentUploadManager = DocumentUploadManager.getInstance()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showProgress by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val pagerState = rememberPagerState()

    var isContractInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            documentUploadManager.createNewContract("test_user")
            isContractInitialized = true
        } catch (e: Exception) {
            errorMessage = "계약 초기화 실패: ${e.message}"
        }
    }

    val pages = listOf(
        Triple("building_registry", "건축물대장", "건축물대장을 업로드하세요."),
        Triple("registry_document", "등기부등본", "등기부등본을 업로드하세요."),
        Triple("contract", "계약서", "계약서를 업로드하세요.")
    )

    // 문서 업로드 상태 관찰
    val documentStatus by documentUploadManager.documentStatus.collectAsState()

    // 각 페이지별 문서 업로드 여부 확인
    val hasUploads = remember(documentStatus) {
        pages.associate { (type, _, _) ->
            type to (documentStatus.documentSets[type]?.isNotEmpty() == true)
        }
    }

    if (!isContractInitialized) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF253F5A))
        }
        return
    }

    Scaffold(
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
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
                    contractManager = contractManager,
                    firebaseStorageUtil = firebaseStorageUtil,
                    documentUploadManager = documentUploadManager
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

                if (pagerState.currentPage < pages.size - 1) {
                    TextButton(
                        onClick = {
                            if (hasUploads[pages[pagerState.currentPage].first] == true) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        enabled = hasUploads[pages[pagerState.currentPage].first] == true
                    ) {
                        Text(
                            "다음",
                            color = if (hasUploads[pages[pagerState.currentPage].first] == true)
                                Color(0xFF253F5A)
                            else
                                Color(0xFFBDBDBD)
                        )
                    }
                } else {
                    // 분석 버튼
                    Button(
                        onClick = {
                            if (documentUploadManager.isReadyForAnalysis()) {
                                scope.launch {
                                    try {
                                        showProgress = true
                                        val contractId = documentUploadManager.getCurrentContractId()
                                        val userId = documentUploadManager.getCurrentUserId()
                                        val analysisId = DocumentAnalyzer().startAnalysis(userId, documentStatus)
                                        val intent = Intent(context, AIAnalysisResultActivity::class.java).apply {
                                            putExtra("analysisId", analysisId)
                                            putExtra("contractId", contractId)
                                            putExtra("userId", userId)
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
    }

    if (showProgress) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("오류") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("확인")
                }
            }
        )
    }
}

@Composable
fun DocumentUploadScreen(
    documentType: String,
    title: String,
    description: String,
    contractManager: ContractManager,
    firebaseStorageUtil: FirebaseStorageUtil,
    documentUploadManager: DocumentUploadManager
) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    // 현재 계약 정보 가져오기
    val contractId = try {
        documentUploadManager.getCurrentContractId()
    } catch (e: Exception) {
        return  // 계약이 초기화되지 않았으면 화면을 표시하지 않음
    }

    val userId = documentUploadManager.getCurrentUserId()

    // 업로드된 문서 목록
    val uploadedDocuments = remember(contractId) {
        runBlocking {
            val docs = contractManager.getDocuments(userId, contractId, documentType)
            docs.mapIndexed { index, doc ->
                DocumentPreview(
                    id = "${doc.pageNumber}",  // pageNumber를 id로 사용
                    imageUrl = doc.imageUrl,
                    pageNumber = doc.pageNumber
                )
            }.sortedBy { it.pageNumber }  // 페이지 번호로 정렬
        }
    }

    var draggingItem by remember { mutableStateOf<DocumentPreview?>(null) }
    var dropTarget by remember { mutableStateOf<DocumentPreview?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    // 카메라 실행 런처
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            coroutineScope.launch {
                isLoading = true
                try {
                    val nextPageNumber = uploadedDocuments.size + 1
                    val imageUrl = firebaseStorageUtil.uploadScannedImage(
                        bitmap = bitmap,
                        documentType = documentType,
                        contractId = contractId,
                        pageNumber = nextPageNumber
                    )

                    documentUploadManager.addDocument(
                        type = documentType,
                        imageUrl = imageUrl,
                        pageNumber = nextPageNumber
                    )
                } catch (e: Exception) {
                    errorMessage = "업로드 실패: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
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
                    uris.forEachIndexed { index, uri ->
                        val nextPageNumber = uploadedDocuments.size + index + 1
                        val imageUrl = firebaseStorageUtil.uploadScannedImageFromUri(
                            uri = uri,
                            context = context,
                            documentType = documentType,
                            contractId = contractId,
                            pageNumber = nextPageNumber
                        )

                        documentUploadManager.addDocument(
                            type = documentType,
                            imageUrl = imageUrl,
                            pageNumber = nextPageNumber
                        )
                    }
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
                state = gridState
            ) {
                items(uploadedDocuments) { document ->
                    DocumentPreviewItem(
                        document = document,
                        onDelete = {
                            coroutineScope.launch {
                                try {
                                    documentUploadManager.removeDocument(
                                        type = documentType,
                                        pageNumber = document.pageNumber
                                    )
                                } catch (e: Exception) {
                                    errorMessage = "문서 삭제 실패: ${e.message}"
                                }
                            }
                        },
                        onDragStart = { draggingItem = document },
                        onDragEnd = {
                            dropTarget?.let { target ->
                                val fromIndex = document.pageNumber - 1
                                val toIndex = target.pageNumber - 1
                                if (fromIndex != toIndex) {
                                    coroutineScope.launch {
                                        try {
                                            documentUploadManager.reorderDocuments(
                                                type = documentType,
                                                fromIndex = fromIndex,
                                                toIndex = toIndex,
                                                firebaseStorageUtil = firebaseStorageUtil
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
                        modifier = Modifier.zIndex(if (draggingItem?.id == document.id) 1f else 0f),
                        isDragging = draggingItem?.id == document.id
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
            CircularProgressIndicator(color = Color(0xFF253F5A))
        }
    }

    // 에러 메시지
    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("오류") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("확인")
                }
            }
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
    var showFullImage by remember { mutableStateOf(false) }

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
            .clickable { showFullImage = true } // 클릭하면 전체보기 활성화
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

    // 이미지 전체 보기 다이얼로그
    if (showFullImage) {
        FullScreenImageDialog(
            imageUrl = document.imageUrl,
            onDismiss = { showFullImage = false }
        )
    }
}

@Composable
fun FullScreenImageDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(0.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Full Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, rotationChange ->
                            // 확대/축소 범위 제한 (0.5배 ~ 5배)
                            scale = (scale * zoom).coerceIn(0.5f, 5f)

                            // 이미지가 확대된 상태에서만 이동 가능
                            if (scale > 1f) {
                                val newOffset = offset + pan
                                // 이동 범위 제한 (화면 밖으로 너무 많이 벗어나지 않도록)
                                val maxOffset = size.width * (scale - 1f) / 2f
                                offset = Offset(
                                    newOffset.x.coerceIn(-maxOffset, maxOffset),
                                    newOffset.y.coerceIn(-maxOffset, maxOffset)
                                )
                            }
                        }
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                        // 이미지가 원래 크기일 때는 위치 초기화
                        if (scale <= 1f) {
                            translationX = 0f
                            translationY = 0f
                        }
                    },
                contentScale = ContentScale.Fit
            )

            // 닫기 버튼
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // 확대/축소 안내 텍스트
            if (scale <= 1f) {
                Text(
                    text = "손가락으로 확대/축소할 수 있습니다",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                )
            }
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
        contractManager = ContractManager(),
        firebaseStorageUtil = FirebaseStorageUtil()
    )
}