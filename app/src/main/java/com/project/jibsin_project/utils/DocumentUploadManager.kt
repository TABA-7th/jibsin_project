package com.project.jibsin_project.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

data class DocumentStatus(
    val buildingRegistry: String? = null,
    val registryDocument: String? = null,
    val contract: String? = null
)

class DocumentUploadManager private constructor() {
    private val _documentStatus = MutableStateFlow(DocumentGroup())
    val documentStatus: StateFlow<DocumentGroup> = _documentStatus

    private var currentGroupId: String = UUID.randomUUID().toString()

    fun updateDocuments(type: String, documentIds: List<String>) {
        val currentStatus = _documentStatus.value
        val updatedSets = currentStatus.documentSets.toMutableMap()
        updatedSets[type] = documentIds

        _documentStatus.value = currentStatus.copy(
            documentSets = updatedSets,
            groupId = currentGroupId
        )
    }

    fun addDocument(type: String, documentId: String) {
        val currentStatus = _documentStatus.value
        val currentDocs = currentStatus.documentSets[type] ?: listOf()
        val updatedDocs = currentDocs + documentId

        updateDocuments(type, updatedDocs)
    }

    fun removeDocument(type: String, documentId: String) {
        val currentStatus = _documentStatus.value
        val currentDocs = currentStatus.documentSets[type] ?: listOf()
        val updatedDocs = currentDocs - documentId

        updateDocuments(type, updatedDocs)
    }

    fun isReadyForAnalysis(): Boolean {
        return with(_documentStatus.value.documentSets) {
            get("building_registry")?.isNotEmpty() == true &&
                    get("registry_document")?.isNotEmpty() == true &&
                    get("contract")?.isNotEmpty() == true
        }
    }

    fun reset() {
        currentGroupId = UUID.randomUUID().toString()
        _documentStatus.value = DocumentGroup(groupId = currentGroupId)
    }

    fun getCurrentGroupId(): String = currentGroupId

    companion object {
        @Volatile
        private var instance: DocumentUploadManager? = null

        fun getInstance(): DocumentUploadManager {
            return instance ?: synchronized(this) {
                instance ?: DocumentUploadManager().also { instance = it }
            }
        }
    }

    suspend fun reorderDocuments(
        type: String,
        fromIndex: Int,
        toIndex: Int,
        firebaseStorageUtil: FirebaseStorageUtil,
        firestoreUtil: FirestoreUtil
    ) {
        val currentStatus = _documentStatus.value
        val currentDocs = currentStatus.documentSets[type]?.toMutableList() ?: return

        if (fromIndex < 0 || toIndex < 0 || fromIndex >= currentDocs.size || toIndex >= currentDocs.size) {
            return
        }

        // 문서 ID 순서 변경
        val movedDoc = currentDocs.removeAt(fromIndex)
        currentDocs.add(toIndex, movedDoc)

        // Storage 파일 이름 업데이트
        try {
            // 영향을 받는 범위의 모든 문서 업데이트
            val start = minOf(fromIndex, toIndex)
            val end = maxOf(fromIndex, toIndex)

            for (i in start..end) {
                val docId = currentDocs[i]
                val doc = firestoreUtil.getDocument(docId)
                val oldPageNumber = doc?.getLong("pageNumber")?.toInt() ?: continue
                val newPageNumber = i + 1

                if (oldPageNumber != newPageNumber) {
                    // Storage 파일 이름 업데이트
                    val newUrl = firebaseStorageUtil.updatePageNumber(
                        currentGroupId,
                        type,
                        oldPageNumber,
                        newPageNumber
                    )

                    // Firestore 문서 업데이트
                    firestoreUtil.updateDocument(docId, mapOf(
                        "pageNumber" to newPageNumber,
                        "imageUrl" to newUrl
                    ))
                }
            }
        } catch (e: Exception) {
            throw e
        }

        // 문서 상태 업데이트
        updateDocuments(type, currentDocs)
    }

    suspend fun removeDocumentWithStorage(
        type: String,
        documentId: String,
        firebaseStorageUtil: FirebaseStorageUtil,
        firestoreUtil: FirestoreUtil
    ) {
        val currentStatus = _documentStatus.value
        val currentDocs = currentStatus.documentSets[type]?.toMutableList() ?: return
        val index = currentDocs.indexOf(documentId)

        if (index != -1) {
            try {
                // Firestore에서 문서 정보 가져오기
                val doc = firestoreUtil.getDocument(documentId)
                val pageNumber = doc?.getLong("pageNumber")?.toInt()

                // Storage에서 파일 삭제
                if (pageNumber != null) {
                    firebaseStorageUtil.deleteDocument(currentGroupId, type, pageNumber)
                }

                // Firestore에서 문서 삭제
                firestoreUtil.deleteDocument(documentId)

                // 문서 목록에서 제거
                val updatedDocs = currentDocs - documentId

                // 나머지 문서들의 페이지 번호 업데이트
                for (i in (index until updatedDocs.size)) {
                    val remainingDocId = updatedDocs[i]
                    val newPageNumber = i + 1

                    // Storage 파일 이름 업데이트
                    val newUrl = firebaseStorageUtil.updatePageNumber(
                        currentGroupId,
                        type,
                        i + 2,  // 기존 페이지 번호
                        newPageNumber
                    )

                    // Firestore 문서 업데이트
                    firestoreUtil.updateDocument(remainingDocId, mapOf(
                        "pageNumber" to newPageNumber,
                        "imageUrl" to newUrl
                    ))
                }

                // 상태 업데이트
                updateDocuments(type, updatedDocs)
            } catch (e: Exception) {
                throw e
            }
        }
    }
}