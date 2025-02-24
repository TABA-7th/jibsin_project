package com.project.jibsin_project.scan.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.project.jibsin_project.utils.FirestoreUtil
import com.project.jibsin_project.utils.BoundingBox
import com.project.jibsin_project.utils.calculateRelativePosition

@Composable
fun BuildingRegistryScreen(contractId: String) {
    val firestoreUtil = remember { FirestoreUtil() }
    var boundingBoxes by remember { mutableStateOf<List<BoundingBox>>(emptyList()) }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    val density = LocalDensity.current

    LaunchedEffect(contractId) {
        val contract = firestoreUtil.getContract("test_user", contractId)
        imageUrl = contract?.building_registry?.firstOrNull()?.imageUrl
        boundingBoxes = firestoreUtil.getBuildingRegistryAnalysis("test_user", contractId)

        // 바운딩 박스 좌표 로그 출력
        println("=== Bounding Boxes ===")
        boundingBoxes.forEach { bbox ->
            println("BoundingBox: x1=${bbox.x1}, y1=${bbox.y1}, x2=${bbox.x2}, y2=${bbox.y2}")
        }
        println("==================")
    }

    imageUrl?.let { url ->
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = url,
                contentDescription = "건축물대장",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            boundingBoxes.forEach { bbox ->
                val position = calculateRelativePosition(bbox, density)

                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) { position.x1.toDp() },
                            y = with(density) { position.y1.toDp() }
                        )
                        .size(
                            width = with(density) { (position.x2 - position.x1).toDp() },
                            height = with(density) { (position.y2 - position.y1).toDp() }
                        )
                        .border(2.dp, Color.Red)
                )
            }
        }
    }
}

