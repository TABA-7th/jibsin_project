package com.project.jibsin_project.scan.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.jibsin_project.utils.Contract

// 사용 예시
@Composable
fun DocumentAnalysisScreen(
    analysisResult: Map<String, Any>,
    contract: Contract
) {
    val notices = mutableListOf<Notice>()

    // 분석 결과에서 notice와 solution 데이터 추출
    (analysisResult["result"] as? Map<*, *>)?.let { result ->
        (result["building_registry"] as? Map<*, *>)?.let { registry ->
            (registry["page1"] as? Map<*, *>)?.let { page ->
                // 각 섹션별 notice와 solution 처리
                extractNotices(page, notices)
            }
        }

        (result["contract"] as? Map<*, *>)?.let { contract ->
            (contract["page1"] as? Map<*, *>)?.let { page ->
                extractNotices(page, notices)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Contract에서 해당 문서의 이미지 URL 가져오기
        contract.building_registry.firstOrNull()?.let { doc ->
            DocumentPreviewWithNotices(
                imageUrl = doc.imageUrl,
                notices = notices,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun extractNotices(page: Map<*, *>, notices: MutableList<Notice>) {
    page.forEach { (key, value) ->
        if (value is Map<*, *>) {
            val boundingBox = value["bounding_box"] as? Map<*, *>
            val notice = value["notice"] as? String
            val solution = value["solution"] as? String

            if (boundingBox != null && !notice.isNullOrEmpty()) {
                notices.add(
                    Notice(
                        boundingBox = BoundingBox(
                            x1 = (boundingBox["x1"] as? Number)?.toInt() ?: 0,
                            x2 = (boundingBox["x2"] as? Number)?.toInt() ?: 0,
                            y1 = (boundingBox["y1"] as? Number)?.toInt() ?: 0,
                            y2 = (boundingBox["y2"] as? Number)?.toInt() ?: 0
                        ),
                        notice = notice,
                        solution = solution ?: "해결 방법이 제공되지 않았습니다."
                    )
                )
            }
        }
    }
}