package com.project.jibsin_project.utils

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DocumentAnalyzer {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun startAnalysis(documents: DocumentGroup): String {
        // DocumentGroup을 DocumentStatus로 변환
        val documentStatus = DocumentStatus(
            buildingRegistry = documents.documentSets["building_registry"]?.firstOrNull(),
            registryDocument = documents.documentSets["registry_document"]?.firstOrNull(),
            contract = documents.documentSets["contract"]?.firstOrNull()
        )

        // 분석 요청 생성
        val analysisRequest = hashMapOf(
            "buildingRegistryId" to documentStatus.buildingRegistry,
            "registryDocumentId" to documentStatus.registryDocument,
            "contractId" to documentStatus.contract,
            "status" to "pending",
            "createdAt" to System.currentTimeMillis()
        )

        // Firestore에 분석 요청 저장
        val analysisRef = firestore.collection("analyses")
            .add(analysisRequest)
            .await()

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