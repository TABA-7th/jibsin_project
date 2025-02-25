package com.project.jibsin_project.scan.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.project.jibsin_project.utils.Contract
import com.project.jibsin_project.utils.BoundingBox

// 사용 예시
@Composable
fun DocumentAnalysisScreen(
    analysisResult: Map<String, Any>,
    contract: Contract
) {
    val notices = remember(analysisResult) {
        extractNotices(analysisResult)
    }

    // 원본 이미지 크기 (기본값으로 설정)
    val defaultImageSize = Pair(1000f, 1400f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 각 문서별 이미지와 알림 표시
        Text(
            text = "건축물대장",
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF253F5A),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        contract.building_registry.firstOrNull()?.let { doc ->
            // 건축물대장에 대한 원본 이미지 크기 (분석 결과에서 가져오기)
            val imageSize = getImageDimensions(analysisResult, "building_registry") ?: defaultImageSize

            DocumentWithNotices(
                documentType = "building_registry",
                imageUrl = doc.imageUrl,
                notices = notices.filter { it.documentType == "building_registry" },
                originalWidth = imageSize.first,
                originalHeight = imageSize.second,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "등기부등본",
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF253F5A),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        contract.registry_document.firstOrNull()?.let { doc ->
            // 등기부등본에 대한 원본 이미지 크기
            val imageSize = getImageDimensions(analysisResult, "registry_document") ?: defaultImageSize

            DocumentWithNotices(
                documentType = "registry_document",
                imageUrl = doc.imageUrl,
                notices = notices.filter { it.documentType == "registry_document" },
                originalWidth = imageSize.first,
                originalHeight = imageSize.second,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "계약서",
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF253F5A),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        contract.contract.firstOrNull()?.let { doc ->
            // 계약서에 대한 원본 이미지 크기
            val imageSize = getImageDimensions(analysisResult, "contract") ?: defaultImageSize

            DocumentWithNotices(
                documentType = "contract",
                imageUrl = doc.imageUrl,
                notices = notices.filter { it.documentType == "contract" },
                originalWidth = imageSize.first,
                originalHeight = imageSize.second,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            )
        }
    }
}

// 이미지 크기 정보 가져오기 함수 추가
private fun getImageDimensions(analysisResult: Map<String, Any>, documentType: String): Pair<Float, Float>? {
    return try {
        (analysisResult["result"] as? Map<*, *>)?.let { result ->
            (result[documentType] as? Map<*, *>)?.let { docData ->
                (docData["page1"] as? Map<*, *>)?.let { page ->
                    (page["image_dimensions"] as? Map<*, *>)?.let { dimensions ->
                        val width = (dimensions["width"] as? Number)?.toFloat() ?: 1000f
                        val height = (dimensions["height"] as? Number)?.toFloat() ?: 1400f
                        Pair(width, height)
                    }
                }
            }
        }
    } catch (e: Exception) {
        null
    }
}

private fun extractNotices(analysisResult: Map<String, Any>): List<Notice> {
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