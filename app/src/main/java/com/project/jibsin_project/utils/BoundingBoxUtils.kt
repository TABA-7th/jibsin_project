package com.project.jibsin_project.utils

import androidx.compose.ui.unit.Density

data class BoundingBox(
    val x1: Int,
    val y1: Int,
    val x2: Int,
    val y2: Int
)

fun calculateRelativePosition(
    bbox: BoundingBox,
    density: Density
): BoundingBox {
    val imageWidth = 1000f  // Firestore에서 저장된 원본 이미지의 기준 너비 (가정)
    val imageHeight = 1000f // Firestore에서 저장된 원본 이미지의 기준 높이 (가정)

    val scaleX = imageWidth / 1000f
    val scaleY = imageHeight / 1000f

    return BoundingBox(
        x1 = (bbox.x1 * scaleX).toInt(),
        y1 = (bbox.y1 * scaleY).toInt(),
        x2 = (bbox.x2 * scaleX).toInt(),
        y2 = (bbox.y2 * scaleY).toInt()
    )
}
