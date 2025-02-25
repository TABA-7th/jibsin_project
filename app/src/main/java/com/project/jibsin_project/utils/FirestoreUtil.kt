package com.project.jibsin_project.utils

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.project.jibsin_project.utils.BoundingBox
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    suspend fun getBuildingRegistryAnalysis(userId: String, contractId: String): Pair<List<BoundingBox>, Pair<Float, Float>> {
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
                val page1 = buildingRegistry["page1"] as? Map<*, *> ?: continue

                // 이미지 사이즈 가져오기
                val imageDimensions = page1["image_dimensions"] as? Map<*, *>
                if (imageDimensions != null) {
                    imageWidth = (imageDimensions["width"] as? Number)?.toFloat() ?: 1f
                    imageHeight = (imageDimensions["height"] as? Number)?.toFloat() ?: 1f
                }

                // 🔥 "page1" 내부의 모든 키를 순회하면서 bounding_box 탐색
                for ((_, sectionData) in page1) {
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

            // 🔥 최종 가져온 데이터 로그
            println("🔥 Firestore에서 가져온 이미지 크기: width=$imageWidth, height=$imageHeight")
            println("🔥 Firestore에서 가져온 바운딩 박스 리스트: $result")

            Pair(result, Pair(imageWidth, imageHeight))
        } catch (e: Exception) {
            println("🔥 Firestore에서 데이터 가져오는 중 오류 발생: ${e.message}")
            Pair(emptyList(), Pair(1f, 1f))
        }
    }

        // 등기부등본 분석 데이터 가져오기
    suspend fun getRegistryDocumentAnalysis(userId: String, contractId: String): Pair<List<BoundingBox>, Pair<Float, Float>> {
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

                // 첫 페이지 데이터 가져오기
                val page1 = registryDocument["page1"] as? Map<*, *> ?: continue

                // 이미지 크기 정보 가져오기
                val imageDimensions = page1["image_dimensions"] as? Map<*, *>
                if (imageDimensions != null) {
                    imageWidth = (imageDimensions["width"] as? Number)?.toFloat() ?: 1f
                    imageHeight = (imageDimensions["height"] as? Number)?.toFloat() ?: 1f
                }

                // 페이지 내부의 모든 섹션 순회하며 바운딩 박스 찾기
                for ((_, sectionData) in page1) {
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

            println("등기부등본 이미지 크기: width=$imageWidth, height=$imageHeight")
            println("등기부등본 바운딩 박스 리스트: $result")

            Pair(result, Pair(imageWidth, imageHeight))
        } catch (e: Exception) {
            println("등기부등본 데이터 가져오는 중 오류 발생: ${e.message}")
            Pair(emptyList(), Pair(1f, 1f))
        }
    }

    // 계약서 분석 데이터 가져오기
    suspend fun getContractAnalysis(userId: String, contractId: String): Pair<List<BoundingBox>, Pair<Float, Float>> {
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

                // 첫 페이지 데이터 가져오기
                val page1 = contractDocument["page1"] as? Map<*, *> ?: continue

                // 이미지 크기 정보 가져오기
                val imageDimensions = page1["image_dimensions"] as? Map<*, *>
                if (imageDimensions != null) {
                    imageWidth = (imageDimensions["width"] as? Number)?.toFloat() ?: 1f
                    imageHeight = (imageDimensions["height"] as? Number)?.toFloat() ?: 1f
                }

                // 페이지 내부의 모든 섹션 순회하며 바운딩 박스 찾기
                for ((_, sectionData) in page1) {
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

            println("계약서 이미지 크기: width=$imageWidth, height=$imageHeight")
            println("계약서 바운딩 박스 리스트: $result")

            Pair(result, Pair(imageWidth, imageHeight))
        } catch (e: Exception) {
            println("계약서 데이터 가져오는 중 오류 발생: ${e.message}")
            Pair(emptyList(), Pair(1f, 1f))
        }
    }
}