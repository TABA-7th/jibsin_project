package com.project.jibsin_project.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class DocumentStatus(
    val buildingRegistry: String? = null,
    val registryDocument: String? = null,
    val contract: String? = null
)

class DocumentUploadManager private constructor() {
    private val _documentStatus = MutableStateFlow(DocumentStatus())
    val documentStatus: StateFlow<DocumentStatus> = _documentStatus

    fun updateDocument(type: String, documentId: String) {
        val currentStatus = _documentStatus.value
        _documentStatus.value = when (type) {
            "building_registry" -> currentStatus.copy(buildingRegistry = documentId)
            "registry_document" -> currentStatus.copy(registryDocument = documentId)
            "contract" -> currentStatus.copy(contract = documentId)
            else -> currentStatus
        }
    }

    fun isReadyForAnalysis(): Boolean {
        return with(_documentStatus.value) {
            buildingRegistry != null && registryDocument != null && contract != null
        }
    }

    fun reset() {
        _documentStatus.value = DocumentStatus()
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