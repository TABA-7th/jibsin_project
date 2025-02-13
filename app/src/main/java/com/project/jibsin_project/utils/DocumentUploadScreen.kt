package com.project.jibsin_project.utils

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    val documentUploadManager = remember { DocumentUploadManager.getInstance() }
    val firebaseStorageUtil = remember { FirebaseStorageUtil() }
    val firestoreUtil = remember { FirestoreUtil() }

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
                        DocumentPreview(documentId, imageUrl, index + 1)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF9F9F9)
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                        items(uploadedDocuments.size) { index ->
                            val document = uploadedDocuments[index]
                            DocumentPreviewItem(
                                document = document,
                                onDelete = {
                                    val updatedDocuments = uploadedDocuments.toMutableList()
                                    updatedDocuments.removeAt(index)
                                    uploadedDocuments = updatedDocuments
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
                    )
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
                        )
                    ) {
                        Text("완료", color = Color.White)
                    }
                }
            }

            // 로딩 인디케이터
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF253F5A)
                )
            }

            // 에러 메시지
            errorMessage?.let { message ->
                ErrorDialog(
                    message = message,
                    onDismiss = { errorMessage = null }
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
        AsyncImage(
            model = document.imageUrl,
            contentDescription = "Document preview",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

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