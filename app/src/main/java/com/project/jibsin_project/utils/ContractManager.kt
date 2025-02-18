package com.project.jibsin_project.utils

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

data class Contract(
    val analysisResult: Map<String, Any>? = null,
    val building_registry: List<DocumentInfo> = listOf(),
    val registry_document: List<DocumentInfo> = listOf(),
    val contract: List<DocumentInfo> = listOf(),
    val createDate: Timestamp = Timestamp.now(),
    val status: String = "pending"
)

data class DocumentInfo(
    val imageUrl: String,
    val pageNumber: Int,
    val uploadDate: Timestamp = Timestamp.now()
)

class ContractManager {
    private val db = Firebase.firestore

    suspend fun createContract(userId: String): String {
        val contractId = generateContractId()
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

    private fun generateContractId(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "CT${timestamp}${random}"
    }

    suspend fun addDocument(
        userId: String,
        contractId: String,
        documentType: String,
        imageUrl: String,
        pageNumber: Int
    ) {
        try {
            val contract = getContract(userId, contractId) ?: throw IllegalStateException("Contract not found")

            val currentDocs = when (documentType) {
                "building_registry" -> contract.building_registry
                "registry_document" -> contract.registry_document
                "contract" -> contract.contract
                else -> throw IllegalArgumentException("Invalid document type")
            }.toMutableList()

            currentDocs.add(DocumentInfo(imageUrl, pageNumber))
            currentDocs.sortBy { it.pageNumber }

            db.collection("users")
                .document(userId)
                .collection("contracts")
                .document(contractId)
                .update(documentType, currentDocs)
                .await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun removeDocument(
        userId: String,
        contractId: String,
        documentType: String,
        pageNumber: Int
    ) {
        try {
            val contract = getContract(userId, contractId) ?: throw IllegalStateException("Contract not found")

            val currentDocs = when (documentType) {
                "building_registry" -> contract.building_registry
                "registry_document" -> contract.registry_document
                "contract" -> contract.contract
                else -> throw IllegalArgumentException("Invalid document type")
            }.toMutableList()

            currentDocs.removeIf { it.pageNumber == pageNumber }
            currentDocs.forEachIndexed { index, doc ->
                if (doc.pageNumber > pageNumber) {
                    currentDocs[index] = doc.copy(pageNumber = doc.pageNumber - 1)
                }
            }

            db.collection("users")
                .document(userId)
                .collection("contracts")
                .document(contractId)
                .update(documentType, currentDocs)
                .await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun reorderDocument(
        userId: String,
        contractId: String,
        documentType: String,
        fromPage: Int,
        toPage: Int
    ) {
        try {
            val contract = getContract(userId, contractId) ?: throw IllegalStateException("Contract not found")

            val currentDocs = when (documentType) {
                "building_registry" -> contract.building_registry
                "registry_document" -> contract.registry_document
                "contract" -> contract.contract
                else -> throw IllegalArgumentException("Invalid document type")
            }.toMutableList()

            val doc = currentDocs.find { it.pageNumber == fromPage } ?: return
            currentDocs.removeIf { it.pageNumber == fromPage }

            if (fromPage < toPage) {
                currentDocs.forEach {
                    if (it.pageNumber in (fromPage + 1)..toPage) {
                        it.copy(pageNumber = it.pageNumber - 1)
                    }
                }
            } else {
                currentDocs.forEach {
                    if (it.pageNumber in toPage until fromPage) {
                        it.copy(pageNumber = it.pageNumber + 1)
                    }
                }
            }

            currentDocs.add(doc.copy(pageNumber = toPage))
            currentDocs.sortBy { it.pageNumber }

            db.collection("users")
                .document(userId)
                .collection("contracts")
                .document(contractId)
                .update(documentType, currentDocs)
                .await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getContract(userId: String, contractId: String): Contract? {
        return try {
            val doc = db.collection("users")
                .document(userId)
                .collection("contracts")
                .document(contractId)
                .get()
                .await()

            doc.toObject(Contract::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getDocuments(userId: String, contractId: String, documentType: String): List<DocumentInfo> {
        val contract = getContract(userId, contractId) ?: return emptyList()

        return when (documentType) {
            "building_registry" -> contract.building_registry
            "registry_document" -> contract.registry_document
            "contract" -> contract.contract
            else -> emptyList()
        }
    }

    suspend fun updateAnalysisResult(
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
                        "status" to "completed"
                    )
                )
                .await()
        } catch (e: Exception) {
            throw e
        }
    }
}