package com.project.jibsin_project.scan.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.project.jibsin_project.utils.BoundingBox
import com.project.jibsin_project.utils.FirestoreUtil

@Composable
fun BuildingRegistryScreen(contractId: String) {
    val firestoreUtil = remember { FirestoreUtil() }
    var boundingBoxes by remember { mutableStateOf(emptyList<BoundingBox>()) }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var originalWidth by remember { mutableStateOf(1f) }
    var originalHeight by remember { mutableStateOf(1f) }

    LaunchedEffect(contractId) {
        val contract = firestoreUtil.getContract("test_user", contractId)
        imageUrl = contract?.building_registry?.firstOrNull()?.imageUrl

        val (boundingBoxList, imageSize) = firestoreUtil.getBuildingRegistryAnalysis("test_user", contractId)
        boundingBoxes = boundingBoxList
        originalWidth = imageSize.first
        originalHeight = imageSize.second

        println("🔥 데이터 로딩 완료: 바운딩 박스 ${boundingBoxes.size}개")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray)
    ) {
        // 이미지 레이어
        imageUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = "건축물대장",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
        }

        // 바운딩 박스 레이어
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 테스트용 빨간 박스
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.Red.copy(alpha = 0.5f))
                    .align(Alignment.Center)
            )

            // 실제 바운딩 박스들 (테스트 후 주석 제거)
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
}