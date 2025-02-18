package com.project.jibsin_project.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class DocumentGroup(
    val documentSets: Map<String, List<String>> = mapOf(),
    val groupId: String = ""
)

class DocumentUploadManager private constructor() {
    private val contractManager = ContractManager()
    private val _documentStatus = MutableStateFlow(DocumentGroup())
    val documentStatus: StateFlow<DocumentGroup> = _documentStatus

    private var currentContractId: String? = null
    private var currentUserId: String? = null

    // 새로운 계약을 생성하고 현재 작업 중인 계약으로 설정
    suspend fun createNewContract(userId: String) {
        currentUserId = userId
        currentContractId = contractManager.createContract(userId)
        _documentStatus.value = DocumentGroup(groupId = currentContractId ?: "")
    }

    // 현재 작업 중인 계약 ID를 반환
    fun getCurrentContractId(): String {
        return currentContractId ?: throw IllegalStateException("업로드 실패: Contract not found")
    }

    // 현재 작업 중인 사용자 ID를 반환
    fun getCurrentUserId(): String {
        return currentUserId ?: throw IllegalStateException("User not found")
    }

    // 문서를 추가하고 로컬 상태 업데이트
    suspend fun addDocument(type: String, imageUrl: String, pageNumber: Int) {
        val contractId = getCurrentContractId()
        val userId = getCurrentUserId()

        try {
            contractManager.addDocument(
                userId = userId,
                contractId = contractId,
                documentType = type,
                imageUrl = imageUrl,
                pageNumber = pageNumber
            )

            // Update local status
            val currentDocs = _documentStatus.value.documentSets.toMutableMap()
            val typeDocuments = currentDocs.getOrDefault(type, listOf()).toMutableList()
            typeDocuments.add(imageUrl)
            currentDocs[type] = typeDocuments
            _documentStatus.value = DocumentGroup(documentSets = currentDocs, groupId = contractId)
        } catch (e: Exception) {
            throw e
        }
    }

    // 문서를 삭제하고 로컬 상태를 업데이트(삭제 후 나머지 문서들의 페이지 번호를 재정렬)
    suspend fun removeDocument(type: String, pageNumber: Int) {
        val contractId = getCurrentContractId()
        val userId = getCurrentUserId()

        try {
            contractManager.removeDocument(
                userId = userId,
                contractId = contractId,
                documentType = type,
                pageNumber = pageNumber
            )

            // Update local status
            val documents = contractManager.getDocuments(userId, contractId, type)
            val currentDocs = _documentStatus.value.documentSets.toMutableMap()
            currentDocs[type] = documents.map { it.imageUrl }
            _documentStatus.value = DocumentGroup(documentSets = currentDocs, groupId = contractId)
        } catch (e: Exception) {
            throw e
        }
    }

    // 문서의 순서를 변경하고 로컬 상태를 업데이트(Storage의 파일명과 Contract의 문서 정보도 함께 업데이트)
    suspend fun reorderDocuments(
        type: String,
        fromIndex: Int,
        toIndex: Int,
        firebaseStorageUtil: FirebaseStorageUtil
    ) {
        val contractId = getCurrentContractId()
        val userId = getCurrentUserId()

        try {
            contractManager.reorderDocument(
                userId = userId,
                contractId = contractId,
                documentType = type,
                fromPage = fromIndex + 1,
                toPage = toIndex + 1
            )

            val newUrl = firebaseStorageUtil.updatePageNumber(
                contractId = contractId,
                documentType = type,
                oldPageNumber = fromIndex + 1,
                newPageNumber = toIndex + 1
            )

            // Update local status
            val documents = contractManager.getDocuments(userId, contractId, type)
            val currentDocs = _documentStatus.value.documentSets.toMutableMap()
            currentDocs[type] = documents.map { it.imageUrl }
            _documentStatus.value = DocumentGroup(documentSets = currentDocs, groupId = contractId)
        } catch (e: Exception) {
            throw e
        }
    }

    // 모든 필요한 문서가 업로드되었는지 확인
    fun isReadyForAnalysis(): Boolean {
        return with(_documentStatus.value.documentSets) {
            get("building_registry")?.isNotEmpty() == true &&
                    get("registry_document")?.isNotEmpty() == true &&
                    get("contract")?.isNotEmpty() == true
        }
    }

    // 현재 상태를 초기화(새로운 계약 작업을 시작할 때 사용)
    fun reset() {
        currentContractId = null
        currentUserId = null
        _documentStatus.value = DocumentGroup()
    }

    companion object {
        @Volatile
        private var instance: DocumentUploadManager? = null

        fun getInstance(): DocumentUploadManager {
            return instance ?: synchronized(this) {
                instance ?: DocumentUploadManager().also { instance = it }
            }
        }
    }
}