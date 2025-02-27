package com.project.jibsin_project.scan.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.project.jibsin_project.utils.BoundingBox

data class Notice(
    val documentType: String,
    val boundingBox: BoundingBox,
    val notice: String,
    val text: String,
    val solution: String
)

data class BoundingBox(
    val x1: Int,
    val x2: Int,
    val y1: Int,
    val y2: Int
)

@Composable
fun DocumentWithNotices(
    documentType: String,
    imageUrl: String,
    notices: List<Notice>,
    originalWidth: Float,
    originalHeight: Float,
    modifier: Modifier = Modifier
) {
    var imageSize by remember { mutableStateOf(Size(0f, 0f)) }
    var expandedNoticeId by remember { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // 문서 이미지
        AsyncImage(
            model = imageUrl,
            contentDescription = "Document preview",
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    imageSize = Size(
                        coordinates.size.width.toFloat(),
                        coordinates.size.height.toFloat()
                    )
                },
            contentScale = ContentScale.FillWidth
        )

        // 알림 아이콘과 툴팁
        if (imageSize.width > 0 && imageSize.height > 0) {
            notices.forEachIndexed { index, notice ->
                // 화면 크기에 맞게 위치 계산
                val widthRatio = imageSize.width / originalWidth
                val heightRatio = imageSize.height / originalHeight

                val position = Offset(
                    x = notice.boundingBox.x1 * widthRatio,
                    y = notice.boundingBox.y1 * heightRatio
                )

                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) { position.x.toDp() },
                            y = with(density) { position.y.toDp() }
                        )
                ) {
                    // 알림 아이콘
                    IconButton(
                        onClick = { expandedNoticeId = if (expandedNoticeId == index) null else index },
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFFFF9800), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color.White
                        )
                    }

                    // 알림 내용 (클릭 시 표시)
                    if (expandedNoticeId == index) {
                        val cardWidth = 200.dp
                        // 카드가 화면 바깥으로 나가지 않도록 위치 조정
                        val isRightSide = position.x > imageSize.width / 2

                        Card(
                            modifier = Modifier
                                .width(cardWidth)
                                .padding(top = 32.dp)
                                .align(if (isRightSide) Alignment.TopEnd else Alignment.TopStart)
                                .offset(
                                    x = if (isRightSide) (-cardWidth) else 0.dp
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 4.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                // 문서 타입
                                Text(
                                    text = when(documentType) {
                                        "building_registry" -> "건축물대장"
                                        "registry_document" -> "등기부등본"
                                        "contract" -> "계약서"
                                        else -> "문서"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                // 원본 텍스트
                                if (notice.text.isNotEmpty()) {
                                    Text(
                                        text = notice.text,
                                        color = Color.Black,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }

                                // 알림 내용이 있는 경우만 표시
                                if (notice.notice.isNotEmpty()) {
                                    Text(
                                        text = "주의",
                                        color = Color(0xFFFF9800),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = notice.notice,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        fontSize = 14.sp
                                    )
                                }

                                // 해결방법이 있는 경우만 표시
                                if (notice.solution.isNotEmpty()) {
                                    Text(
                                        text = "해결 방법",
                                        color = Color(0xFF4CAF50),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = notice.solution,
                                        fontSize = 14.sp
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

private fun calculatePosition(box: BoundingBox, imageSize: Size): Offset {
    val x = (box.x1 / 1000f) * imageSize.width
    val y = (box.y1 / 1000f) * imageSize.height
    return Offset(x, y)
}

data class Size(val width: Float, val height: Float)
