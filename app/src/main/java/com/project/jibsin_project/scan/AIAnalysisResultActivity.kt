package com.project.jibsin_project.scan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class AIAnalysisResultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val documentId = intent.getStringExtra("documentId") ?: return
        val documentType = intent.getStringExtra("documentType") ?: return

        setContent {
            AIAnalysisResultScreen(documentId, documentType)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAnalysisResultScreen(documentId: String, documentType: String) {
    var analysisResult by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(documentId) {
        // TODO: Firestore에서 분석 결과 가져오기
        // 임시 데이터
        analysisResult = mapOf(
            "건물명" to "예시아파트",
            "주소" to "서울시 강남구 테헤란로 123",
            "면적" to "85.12㎡",
            "분석일시" to "2024-02-10 15:30"
        )
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("분석 결과", fontSize = 20.sp) },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFFF9F9F9))
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
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFF253F5A)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    analysisResult?.forEach { (key, value) ->
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = key,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF253F5A)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = value.toString(),
                                        fontSize = 14.sp,
                                        color = Color.Black
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

@Preview(showBackground = true)
@Composable
fun PreviewAIAnalysisResultScreen() {
    AIAnalysisResultScreen(
        documentId = "preview_doc_id",
        documentType = "building_registry"
    )
}
