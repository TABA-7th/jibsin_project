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
}