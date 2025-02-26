package com.project.jibsin_project.utils

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.project.jibsin_project.history.WarningItem
import com.project.jibsin_project.history.WarningLevel
import com.project.jibsin_project.scan.components.Notice
import com.project.jibsin_project.utils.BoundingBox
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// 기본 데이터 클래스들
data class DocumentInfo(
    val imageUrl: String = "",
    val pageNumber: Int = 0,
    val uploadDate: Timestamp = Timestamp.now()
)

enum class ContractStatus {
    SCANNING,   // 문서 스캔 중
    PENDING,    // 분석 대기 중
    COMPLETE    // 분석 완료
}

data class Contract(
    val analysisResult: Map<String, Any>? = null,
    val analysisId: String? = null,  // 분석 ID
    val building_registry: List<DocumentInfo> = listOf(),
    val registry_document: List<DocumentInfo> = listOf(),
    val contract: List<DocumentInfo> = listOf(),
    val analysisStatus: String = "processing",
    val createDate: Timestamp = Timestamp.now()
)

class FirestoreUtil {
    private val db = Firebase.firestore

    private fun generateContractId(userId: String): String {
        val timestamp = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyMMdd", Locale.getDefault())
        val date = dateFormat.format(Date(timestamp))
        val random = (1000..9999).random()
        return "${userId}-CT-${date}${random}"
    }

    suspend fun createNewContract(userId: String): String {
        val contractId = generateContractId(userId)
        val contract = Contract()

        try {
            db.collection("users")
                .document(userId)
                .collection("contracts")
                .document(contractId)
                .set(contract)
                .await()

            return contractId
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun addDocumentToContract(
        userId: String,
        contractId: String,
        documentType: String,
        imageUrl: String,
        pageNumber: Int
    ) {
        try {
            val contractRef = db.collection("users")
                .document(userId)
                .collection("contracts")
                .document(contractId)

            val contract = contractRef.get().await().toObject(Contract::class.java)
                ?: throw IllegalStateException("Contract not found")

            val newDoc = DocumentInfo(
                imageUrl = imageUrl,
                pageNumber = pageNumber,
                uploadDate = Timestamp.now()
            )

            val updatedDocs = when (documentType) {
                "building_registry" -> contract.building_registry
                "registry_document" -> contract.registry_document
                "contract" -> contract.contract
                else -> throw IllegalArgumentException("Invalid document type")
            }.toMutableList()

            // 페이지 번호에 해당하는 문서가 있으면 교체, 없으면 추가
            val existingIndex = updatedDocs.indexOfFirst { it.pageNumber == pageNumber }
            if (existingIndex != -1) {
                updatedDocs[existingIndex] = newDoc
            } else {
                updatedDocs.add(newDoc)
            }
            updatedDocs.sortBy { it.pageNumber }

            contractRef.update(documentType, updatedDocs).await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun removeDocumentFromContract(
        userId: String,
        contractId: String,
        documentType: String,
        pageNumber: Int,
        firebaseStorageUtil: FirebaseStorageUtil
    ) {
        try {
            val contractRef = db.collection("users")
                .document(userId)
                .collection("contracts")
                .document(contractId)

            val contract = contractRef.get().await().toObject(Contract::class.java)
                ?: throw IllegalStateException("Contract not found")

            val updatedDocs = when (documentType) {
                "building_registry" -> contract.building_registry
                "registry_document" -> contract.registry_document
                "contract" -> contract.contract
                else -> throw IllegalArgumentException("Invalid document type")
            }.toMutableList()

            // 삭제할 문서 제거
            updatedDocs.removeIf { it.pageNumber == pageNumber }

            // Storage에서 현재 파일 삭제
            firebaseStorageUtil.deleteDocument(
                contractId = contractId,
                documentType = documentType,
                pageNumber = pageNumber
            )

            // 남은 문서들의 페이지 번호와 파일 이름 업데이트
            for (i in pageNumber..updatedDocs.size) {
                val doc = updatedDocs.find { it.pageNumber == i + 1 } ?: continue

                // Storage의 파일 이름 업데이트
                val newUrl = firebaseStorageUtil.updatePageNumber(
                    contractId = contractId,
                    documentType = documentType,
                    oldPageNumber = i + 1,
                    newPageNumber = i
                )

                // DocumentInfo 업데이트
                val docIndex = updatedDocs.indexOfFirst { it.pageNumber == i + 1 }
                if (docIndex != -1) {
                    updatedDocs[docIndex] = doc.copy(
                        pageNumber = i,
                        imageUrl = newUrl
                    )
                }
            }

            // 문서 순서대로 정렬
            updatedDocs.sortBy { it.pageNumber }

            // Contract 업데이트
            val updateData = when (documentType) {
                "building_registry" -> mapOf("building_registry" to updatedDocs)
                "registry_document" -> mapOf("registry_document" to updatedDocs)
                "contract" -> mapOf("contract" to updatedDocs)
                else -> throw IllegalArgumentException("Invalid document type")
            }

            contractRef.update(updateData).await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateContractAnalysisResult(
        userId: String,
        contractId: String,
        result: Map<String, Any>
    ) {
        try {
            db.collection("users")
                .document(userId)
                .collection("contracts")
                .document(contractId)
                .update(
                    mapOf(
                        "analysisResult" to result,
                        "status" to ContractStatus.COMPLETE.name
                    )
                )
                .await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateContractStatus(
        userId: String,
        contractId: String,
        status: ContractStatus
    ) {
        try {
            db.collection("users")
                .document(userId)
                .collection("contracts")
                .document(contractId)
                .update("status", status.name)
                .await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getContract(userId: String, contractId: String): Contract? {
        return try {
            db.collection("users")
                .document(userId)
                .collection("contracts")
                .document(contractId)
                .get()
                .await()
                .toObject(Contract::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun reorderDocumentInContract(
        userId: String,
        contractId: String,
        documentType: String,
        fromPage: Int,
        toPage: Int
    ) {
        try {
            val contractRef = db.collection("users")
                .document(userId)
                .collection("contracts")
                .document(contractId)

            val contract = contractRef.get().await().toObject(Contract::class.java)
                ?: throw IllegalStateException("Contract not found")

            val docs = when (documentType) {
                "building_registry" -> contract.building_registry
                "registry_document" -> contract.registry_document
                "contract" -> contract.contract
                else -> throw IllegalArgumentException("Invalid document type")
            }.toMutableList()

            val doc = docs.find { it.pageNumber == fromPage }
                ?: throw IllegalStateException("Document not found")

            docs.removeIf { it.pageNumber == fromPage }

            // 페이지 번호 조정
            if (fromPage < toPage) {
                docs.forEach {
                    if (it.pageNumber in (fromPage + 1)..toPage) {
                        it.copy(pageNumber = it.pageNumber - 1)
                    }
                }
            } else {
                docs.forEach {
                    if (it.pageNumber in toPage until fromPage) {
                        it.copy(pageNumber = it.pageNumber + 1)
                    }
                }
            }

            docs.add(doc.copy(pageNumber = toPage))
            docs.sortBy { it.pageNumber }

            contractRef.update(documentType, docs).await()
        } catch (e: Exception) {
            throw e
        }
    }

    // 계약서 목록 조회
    suspend fun getAllContracts(userId: String): List<Pair<String, Contract>> {
        return try {
            db.collection("users")
                .document(userId)
                .collection("contracts")
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(Contract::class.java)?.let {
                        Pair(doc.id, it)
                    }
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 계약서 삭제
    suspend fun deleteContract(userId: String, contractId: String) {
        try {
            db.collection("users")
                .document(userId)
                .collection("contracts")
                .document(contractId)
                .delete()
                .await()
        } catch (e: Exception) {
            throw e
        }
    }

    // 계약서 문서 검색
    suspend fun searchContracts(
        userId: String,
        query: String,
        status: ContractStatus? = null
    ): List<Pair<String, Contract>> {
        return try {
            val collectionRef = db.collection("users")
                .document(userId)
                .collection("contracts")

            val firestoreQuery = if (status != null) {
                collectionRef.whereEqualTo("status", status.name)
            } else {
                collectionRef
            }

            firestoreQuery.get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(Contract::class.java)?.let {
                        if (doc.id.contains(query, ignoreCase = true)) {
                            Pair(doc.id, it)
                        } else {
                            null
                        }
                    }
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 계약서 배치(batch) 업데이트
    suspend fun batchUpdateContracts(
        userId: String,
        updates: List<Triple<String, String, Any>>  // (contractId, field, value)
    ) {
        try {
            db.runBatch { batch ->
                updates.forEach { (contractId, field, value) ->
                    val docRef = db.collection("users")
                        .document(userId)
                        .collection("contracts")
                        .document(contractId)
                    batch.update(docRef, field, value)
                }
            }.await()
        } catch (e: Exception) {
            throw e
        }
    }

    // 건축물대장 특정 페이지 분석 데이터 가져오기
    suspend fun getBuildingRegistryAnalysis(
        userId: String,
        contractId: String,
        pageNumber: Int
    ): Pair<List<BoundingBox>, Pair<Float, Float>> {
        return try {
            val result = mutableListOf<BoundingBox>()
            var imageWidth = 1f
            var imageHeight = 1f

            val snapshot = db.collection("users")
                .document(userId)
                .collection("contracts")
                .document(contractId)
                .collection("AI_analysis")
                .get()
                .await()

            for (doc in snapshot.documents) {
                val analysisData = doc.data ?: continue
                val resultData = analysisData["result"] as? Map<*, *> ?: continue
                val buildingRegistry = resultData["building_registry"] as? Map<*, *> ?: continue

                // 요청된 페이지 데이터 가져오기
                val pageName = "page$pageNumber"
                val pageData = buildingRegistry[pageName] as? Map<*, *> ?: continue

                // 이미지 크기 정보 가져오기
                val imageDimensions = pageData["image_dimensions"] as? Map<*, *>
                if (imageDimensions != null) {
                    imageWidth = (imageDimensions["width"] as? Number)?.toFloat() ?: 1f
                    imageHeight = (imageDimensions["height"] as? Number)?.toFloat() ?: 1f
                }

                // 페이지 내부의 모든 섹션 순회하며 바운딩 박스 찾기
                for ((_, sectionData) in pageData) {
                    if (sectionData is Map<*, *>) {
                        val bboxData = sectionData["bounding_box"] as? Map<*, *>
                        if (bboxData != null) {
                            result.add(
                                BoundingBox(
                                    x1 = (bboxData["x1"] as? Number)?.toInt() ?: 0,
                                    y1 = (bboxData["y1"] as? Number)?.toInt() ?: 0,
                                    x2 = (bboxData["x2"] as? Number)?.toInt() ?: 0,
                                    y2 = (bboxData["y2"] as? Number)?.toInt() ?: 0
                                )
                            )
                        }
                    }
                }
            }

            println("건축물대장 페이지 $pageNumber 이미지 크기: width=$imageWidth, height=$imageHeight")
            println("건축물대장 페이지 $pageNumber 바운딩 박스 리스트: $result")

            Pair(result, Pair(imageWidth, imageHeight))
        } catch (e: Exception) {
            println("건축물대장 페이지 $pageNumber 데이터 가져오는 중 오류 발생: ${e.message}")
            Pair(emptyList(), Pair(1f, 1f))
        }
    }

    // 등기부등본 특정 페이지 분석 데이터 가져오기
    suspend fun getRegistryDocumentAnalysis(
        userId: String,
        contractId: String,
        pageNumber: Int
    ): Pair<List<BoundingBox>, Pair<Float, Float>> {
        return try {
            val result = mutableListOf<BoundingBox>()
            var imageWidth = 1f
            var imageHeight = 1f

            val snapshot = db.collection("users")
                .document(userId)
                .collection("contracts")
                .document(contractId)
                .collection("AI_analysis")
                .get()
                .await()

            for (doc in snapshot.documents) {
                val analysisData = doc.data ?: continue
                val resultData = analysisData["result"] as? Map<*, *> ?: continue
                val registryDocument = resultData["registry_document"] as? Map<*, *> ?: continue

                // 요청된 페이지 데이터 가져오기
                val pageName = "page$pageNumber"
                val pageData = registryDocument[pageName] as? Map<*, *> ?: continue

                // 이미지 크기 정보 가져오기
                val imageDimensions = pageData["image_dimensions"] as? Map<*, *>
                if (imageDimensions != null) {
                    imageWidth = (imageDimensions["width"] as? Number)?.toFloat() ?: 1f
                    imageHeight = (imageDimensions["height"] as? Number)?.toFloat() ?: 1f
                }

                // 페이지 내부의 모든 섹션 순회하며 바운딩 박스 찾기
                for ((_, sectionData) in pageData) {
                    if (sectionData is Map<*, *>) {
                        val bboxData = sectionData["bounding_box"] as? Map<*, *>
                        if (bboxData != null) {
                            result.add(
                                BoundingBox(
                                    x1 = (bboxData["x1"] as? Number)?.toInt() ?: 0,
                                    y1 = (bboxData["y1"] as? Number)?.toInt() ?: 0,
                                    x2 = (bboxData["x2"] as? Number)?.toInt() ?: 0,
                                    y2 = (bboxData["y2"] as? Number)?.toInt() ?: 0
                                )
                            )
                        }
                    }
                }
            }

            println("등기부등본 페이지 $pageNumber 이미지 크기: width=$imageWidth, height=$imageHeight")
            println("등기부등본 페이지 $pageNumber 바운딩 박스 리스트: $result")

            Pair(result, Pair(imageWidth, imageHeight))
        } catch (e: Exception) {
            println("등기부등본 페이지 $pageNumber 데이터 가져오는 중 오류 발생: ${e.message}")
            Pair(emptyList(), Pair(1f, 1f))
        }
    }

    // 계약서 특정 페이지 분석 데이터 가져오기
    suspend fun getContractAnalysis(
        userId: String,
        contractId: String,
        pageNumber: Int
    ): Pair<List<BoundingBox>, Pair<Float, Float>> {
        return try {
            val result = mutableListOf<BoundingBox>()
            var imageWidth = 1f
            var imageHeight = 1f

            val snapshot = db.collection("users")
                .document(userId)
                .collection("contracts")
                .document(contractId)
                .collection("AI_analysis")
                .get()
                .await()

            for (doc in snapshot.documents) {
                val analysisData = doc.data ?: continue
                val resultData = analysisData["result"] as? Map<*, *> ?: continue
                val contractDocument = resultData["contract"] as? Map<*, *> ?: continue

                // 요청된 페이지 데이터 가져오기
                val pageName = "page$pageNumber"
                val pageData = contractDocument[pageName] as? Map<*, *> ?: continue

                // 이미지 크기 정보 가져오기
                val imageDimensions = pageData["image_dimensions"] as? Map<*, *>
                if (imageDimensions != null) {
                    imageWidth = (imageDimensions["width"] as? Number)?.toFloat() ?: 1f
                    imageHeight = (imageDimensions["height"] as? Number)?.toFloat() ?: 1f
                }

                // 페이지 내부의 모든 섹션 순회하며 바운딩 박스 찾기
                for ((_, sectionData) in pageData) {
                    if (sectionData is Map<*, *>) {
                        val bboxData = sectionData["bounding_box"] as? Map<*, *>
                        if (bboxData != null) {
                            result.add(
                                BoundingBox(
                                    x1 = (bboxData["x1"] as? Number)?.toInt() ?: 0,
                                    y1 = (bboxData["y1"] as? Number)?.toInt() ?: 0,
                                    x2 = (bboxData["x2"] as? Number)?.toInt() ?: 0,
                                    y2 = (bboxData["y2"] as? Number)?.toInt() ?: 0
                                )
                            )
                        }
                    }
                }
            }

            println("계약서 페이지 $pageNumber 이미지 크기: width=$imageWidth, height=$imageHeight")
            println("계약서 페이지 $pageNumber 바운딩 박스 리스트: $result")

            Pair(result, Pair(imageWidth, imageHeight))
        } catch (e: Exception) {
            println("계약서 페이지 $pageNumber 데이터 가져오는 중 오류 발생: ${e.message}")
            Pair(emptyList(), Pair(1f, 1f))
        }
    }

    // AI 분석 결과에서 알림 데이터 직접 가져오기
    suspend fun getAIAnalysisNotices(
        userId: String,
        contractId: String,
        documentType: String,
        pageNumber: Int
    ): List<Notice> {
        return try {
            val notices = mutableListOf<Notice>()

            // AI_analysis 컬렉션에서 문서 가져오기
            val snapshot = db.collection("users")
                .document(userId)
                .collection("contracts")
                .document(contractId)
                .collection("AI_analysis")
                .get()
                .await()

            println("AI_analysis 문서 수: ${snapshot.documents.size}")

            // 각 분석 문서에서 알림 데이터 추출
            for (doc in snapshot.documents) {
                println("분석 문서 ID: ${doc.id}")
                val resultData = doc.data?.get("result") as? Map<*, *> ?: continue

                val docTypeData = resultData[documentType] as? Map<*, *> ?: continue
                val pageName = "page$pageNumber"
                val pageData = docTypeData[pageName] as? Map<*, *> ?: continue

                println("페이지 데이터 키: ${pageData.keys.joinToString()}")

                // 페이지 내 모든 섹션에서 알림 데이터 찾기
                pageData.forEach { (sectionKey, sectionValue) ->
                    if (sectionValue is Map<*, *> && sectionKey.toString() != "image_dimensions") {
                        // notice 필드가 있는지 확인
                        val noticeText = sectionValue["notice"] as? String
                        if (!noticeText.isNullOrEmpty()) {
                            val boundingBox = sectionValue["bounding_box"] as? Map<*, *>
                            if (boundingBox != null) {
                                notices.add(
                                    Notice(
                                        documentType = documentType,
                                        boundingBox = BoundingBox(
                                            x1 = (boundingBox["x1"] as? Number)?.toInt() ?: 0,
                                            x2 = (boundingBox["x2"] as? Number)?.toInt() ?: 0,
                                            y1 = (boundingBox["y1"] as? Number)?.toInt() ?: 0,
                                            y2 = (boundingBox["y2"] as? Number)?.toInt() ?: 0
                                        ),
                                        notice = noticeText,
                                        text = sectionValue["text"] as? String ?: "",
                                        solution = sectionValue["solution"] as? String ?: ""
                                    )
                                )
                                println("알림 추가: $noticeText")
                            }
                        }
                    }
                }
            }

            println("찾은 알림 수: ${notices.size}")
            notices
        } catch (e: Exception) {
            println("알림 데이터 가져오는 중 오류 발생: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    // 등기부등본의 발급일자/열람일시 데이터 가져오기
    suspend fun getRegistryDocumentDates(userId: String, contractId: String): List<DateInfo> {
        val dates = mutableListOf<DateInfo>()

        try {
            // AI 분석 결과 조회
            val analysisPath = "users/$userId/contracts/$contractId/AI_analysis"
            val analysisDocs = db.collection(analysisPath).get().await()

            for (doc in analysisDocs.documents) {
                val data = doc.data ?: continue
                val result = data["result"] as? Map<String, Any> ?: continue

                val registryDocument = result["registry_document"] as? Map<String, Any> ?: continue

                // 모든 페이지 순회
                for ((pageKey, pageValue) in registryDocument) {
                    if (!pageKey.toString().startsWith("page")) continue

                    val page = pageValue as? Map<String, Any> ?: continue

                    // 발급일자 또는 열람일시 찾기
                    for ((key, value) in page) {
                        if (key == "발급일자" || key == "열람일시" || key.toString().contains("발급") || key.toString().contains("열람")) {
                            val field = value as? Map<String, Any> ?: continue
                            val text = field["text"] as? String ?: continue
                            val boundingBox = extractBoundingBox(field["bounding_box"] as? Map<String, Any>)

                            dates.add(DateInfo(key.toString(), text, boundingBox))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("등기부등본 발급일자 조회 오류: ${e.message}")
        }

        return dates
    }

    // 건축물대장의 발급일자 데이터 가져오기
    suspend fun getBuildingRegistryDates(userId: String, contractId: String): List<DateInfo> {
        val dates = mutableListOf<DateInfo>()

        try {
            // AI 분석 결과 조회
            val analysisPath = "users/$userId/contracts/$contractId/AI_analysis"
            val analysisDocs = db.collection(analysisPath).get().await()

            for (doc in analysisDocs.documents) {
                val data = doc.data ?: continue
                val result = data["result"] as? Map<String, Any> ?: continue

                val buildingRegistry = result["building_registry"] as? Map<String, Any> ?: continue

                // 모든 페이지 순회
                for ((pageKey, pageValue) in buildingRegistry) {
                    if (!pageKey.toString().startsWith("page")) continue

                    val page = pageValue as? Map<String, Any> ?: continue

                    // 발급일자 찾기
                    for ((key, value) in page) {
                        if (key == "발급일자" || key.toString().contains("발급")) {
                            val field = value as? Map<String, Any> ?: continue
                            val text = field["text"] as? String ?: continue
                            val boundingBox = extractBoundingBox(field["bounding_box"] as? Map<String, Any>)

                            dates.add(DateInfo(key.toString(), text, boundingBox))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("건축물대장 발급일자 조회 오류: ${e.message}")
        }

        return dates
    }

    // BoundingBox 추출 헬퍼 함수
    private fun extractBoundingBox(boxMap: Map<String, Any>?): BoundingBox {
        if (boxMap == null) return BoundingBox(0, 0, 0, 0)

        return BoundingBox(
            x1 = (boxMap["x1"] as? Number)?.toInt() ?: 0,
            y1 = (boxMap["y1"] as? Number)?.toInt() ?: 0,
            x2 = (boxMap["x2"] as? Number)?.toInt() ?: 0,
            y2 = (boxMap["y2"] as? Number)?.toInt() ?: 0
        )
    }

    // 발급일자 정보를 담는 데이터 클래스
    data class DateInfo(
        val key: String,
        val text: String,
        val boundingBox: BoundingBox
    )

    // 경고 항목 저장하기
    suspend fun saveWarning(userId: String, warning: WarningItem) {
        try {
            val warningMap = mapOf(
                "id" to warning.id,
                "level" to warning.level.name,
                "message" to warning.message,
                "solution" to warning.solution,
                "source" to warning.source,
                "date" to Timestamp(warning.date),
                "contractId" to warning.contractId
            )

            db.collection("users")
                .document(userId)
                .collection("warnings")
                .document(warning.id)
                .set(warningMap)
                .await()
        } catch (e: Exception) {
            throw e
        }
    }

    // 경고 내역 불러오기
    suspend fun getWarnings(userId: String): List<WarningItem> {
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("warnings")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null

                try {
                    WarningItem(
                        id = data["id"] as? String ?: doc.id,
                        level = WarningLevel.valueOf(data["level"] as? String ?: WarningLevel.WARNING.name),
                        message = data["message"] as? String ?: "",
                        solution = data["solution"] as? String ?: "",
                        source = data["source"] as? String ?: "",
                        date = (data["date"] as? Timestamp)?.toDate() ?: Date(),
                        contractId = data["contractId"] as? String ?: ""
                    )
                } catch (e: Exception) {
                    println("경고 항목 파싱 중 오류: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            println("경고 내역 불러오기 오류: ${e.message}")
            emptyList()
        }
    }

    // 특정 계약에 관련된 경고만 불러오기
    suspend fun getWarningsByContract(userId: String, contractId: String): List<WarningItem> {
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("warnings")
                .whereEqualTo("contractId", contractId)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null

                try {
                    WarningItem(
                        id = data["id"] as? String ?: doc.id,
                        level = WarningLevel.valueOf(data["level"] as? String ?: WarningLevel.WARNING.name),
                        message = data["message"] as? String ?: "",
                        solution = data["solution"] as? String ?: "",
                        source = data["source"] as? String ?: "",
                        date = (data["date"] as? Timestamp)?.toDate() ?: Date(),
                        contractId = data["contractId"] as? String ?: ""
                    )
                } catch (e: Exception) {
                    println("경고 항목 파싱 중 오류: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            println("계약별 경고 내역 불러오기 오류: ${e.message}")
            emptyList()
        }
    }

    // 경고 삭제하기
    suspend fun deleteWarning(userId: String, warningId: String) {
        try {
            db.collection("users")
                .document(userId)
                .collection("warnings")
                .document(warningId)
                .delete()
                .await()
        } catch (e: Exception) {
            throw e
        }
    }

    // 문서 분석 중 발견된 경고를 자동으로 저장
    suspend fun saveWarningsFromAnalysis(userId: String, contractId: String, analysisResult: Map<String, Any>) {
        try {
            // 분석 결과에서 경고 추출
            val warnings = extractWarningsFromAnalysis(contractId, analysisResult)

            // 각 경고를 저장
            warnings.forEach { warning ->
                saveWarning(userId, warning)
            }
        } catch (e: Exception) {
            println("분석 결과에서 경고 저장 중 오류: ${e.message}")
        }
    }

    // 분석 결과에서 경고 추출하기
    private fun extractWarningsFromAnalysis(contractId: String, analysisResult: Map<String, Any>): List<WarningItem> {
        val warnings = mutableListOf<WarningItem>()

        try {
            // 분석 결과 구조에 맞게 경고 추출 로직 구현
            // 예시로는 단순한 구조를 가정함

            // 1. 직접적인 warnings 키에서 추출
            (analysisResult["warnings"] as? List<*>)?.forEach { warningData ->
                if (warningData is Map<*, *>) {
                    val message = warningData["message"] as? String ?: warningData["text"] as? String ?: ""
                    val solution = warningData["solution"] as? String ?: ""
                    val source = warningData["source"] as? String ?: "분석 결과"
                    val level = if (warningData["level"] == "danger" || warningData["level"] == "DANGER") {
                        WarningLevel.DANGER
                    } else {
                        WarningLevel.WARNING
                    }

                    val warningId = UUID.randomUUID().toString()

                    warnings.add(
                        WarningItem(
                            id = warningId,
                            level = level,
                            message = message,
                            solution = solution,
                            source = source,
                            date = Date(),
                            contractId = contractId
                        )
                    )
                }
            }

            // 2. 중첩된 구조에서도 검색
            (analysisResult["result"] as? Map<*, *>)?.let { result ->
                // 각 문서 타입별로 처리
                val documentTypes = listOf("building_registry", "registry_document", "contract")

                for (docType in documentTypes) {
                    (result[docType] as? Map<*, *>)?.let { docTypeData ->
                        // 각 페이지별로 처리
                        docTypeData.keys.forEach { pageKey ->
                            if (pageKey.toString().startsWith("page")) {
                                (docTypeData[pageKey] as? Map<*, *>)?.let { pageData ->
                                    // 알림 데이터 찾기
                                    val notices = pageData["notices"] as? List<*>
                                        ?: pageData["warnings"] as? List<*>
                                        ?: emptyList<Any>()

                                    notices.forEach { noticeData ->
                                        if (noticeData is Map<*, *>) {
                                            val message = noticeData["notice"] as? String
                                                ?: noticeData["message"] as? String
                                                ?: noticeData["text"] as? String
                                                ?: ""

                                            if (message.isNotEmpty() && message != "문제 없음") {
                                                val solution = noticeData["solution"] as? String ?: ""
                                                val documentSource = when(docType) {
                                                    "building_registry" -> "${contractId.split("-").last()} 건축물대장"
                                                    "registry_document" -> "${contractId.split("-").last()} 등기부등본"
                                                    "contract" -> "${contractId.split("-").last()} 계약서"
                                                    else -> "${contractId.split("-").last()} $docType"
                                                }

                                                // 위험 수준 결정 (특정 키워드에 따라 DANGER로 설정)
                                                val dangerKeywords = listOf(
                                                    "보증금", "일치하지 않", "소유자", "임대인", "근저당", "압류",
                                                    "가압류", "가처분", "경매", "위험"
                                                )

                                                val isDanger = dangerKeywords.any { keyword ->
                                                    message.contains(keyword)
                                                }

                                                val level = if (isDanger) WarningLevel.DANGER else WarningLevel.WARNING
                                                val warningId = UUID.randomUUID().toString()

                                                warnings.add(
                                                    WarningItem(
                                                        id = warningId,
                                                        level = level,
                                                        message = message,
                                                        solution = solution,
                                                        source = documentSource,
                                                        date = Date(),
                                                        contractId = contractId
                                                    )
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

        } catch (e: Exception) {
            println("경고 추출 중 오류: ${e.message}")
        }

        return warnings
    }

    suspend fun deleteAllWarnings(userId: String) {
        try {
            // 1. 모든 경고 내역 가져오기
            val warnings = getWarnings(userId)

            // 2. 경고가 없으면 바로 리턴
            if (warnings.isEmpty()) {
                return
            }

            // 3. 배치 삭제 수행 (Firestore는 한 번에 최대 500개까지 배치 작업 가능)
            val batchSize = 450 // 안전을 위해 450개로 제한

            // 경고를 배치 크기에 맞게 나누어 삭제
            warnings.chunked(batchSize).forEach { chunk ->
                val batch = db.batch()

                // 각 경고에 대한 삭제 작업을 배치에 추가
                chunk.forEach { warning ->
                    val warningRef = db.collection("users")
                        .document(userId)
                        .collection("warnings")
                        .document(warning.id)

                    batch.delete(warningRef)
                }

                // 배치 실행
                batch.commit().await()
            }
        } catch (e: Exception) {
            println("모든 경고 삭제 중 오류: ${e.message}")
            throw e
        }
    }
}