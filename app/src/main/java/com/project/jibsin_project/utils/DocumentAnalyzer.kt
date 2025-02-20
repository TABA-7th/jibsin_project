package com.project.jibsin_project.utils

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DocumentAnalyzer {
    private val firestore = FirebaseFirestore.getInstance()
    private val contractManager = ContractManager()

    suspend fun startAnalysis(userId: String, documentGroup: DocumentGroup): String {
        val contractId = documentGroup.groupId
        val contract = contractManager.getContract(userId, contractId)
            ?: throw IllegalStateException("Contract not found")

        // 문서 정보 수집
        val buildingRegistryDocs = contractManager.getDocuments(userId, contractId, "building_registry")
        val registryDocs = contractManager.getDocuments(userId, contractId, "registry_document")
        val contractDocs = contractManager.getDocuments(userId, contractId, "contract")

        // 분석 요청 생성
        val analysisRequest = hashMapOf(
            "userId" to userId,
            "contractId" to contractId,
            "buildingRegistryUrl" to (buildingRegistryDocs.firstOrNull()?.imageUrl ?: ""),
            "registryDocumentUrl" to (registryDocs.firstOrNull()?.imageUrl ?: ""),
            "contractUrl" to (contractDocs.firstOrNull()?.imageUrl ?: ""),
            "status" to "pending",
            "createdAt" to System.currentTimeMillis()
        )

        // Firestore에 분석 요청 저장
        val analysisRef = firestore.collection("analyses")
            .add(analysisRequest)
            .await()

        // 계약 상태 업데이트
        contractManager.updateAnalysisResult(
            userId = userId,
            contractId = contractId,
            result = mapOf("analysisId" to analysisRef.id)
        )

        return analysisRef.id
    }

    suspend fun getAnalysisResult(analysisId: String): Map<String, Any>? {
        val result = firestore.collection("analyses")
            .document(analysisId)
            .get()
            .await()

        return result.data
    }
}