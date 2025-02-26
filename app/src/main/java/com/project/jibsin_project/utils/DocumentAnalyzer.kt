package com.project.jibsin_project.utils

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DocumentAnalyzer {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun startAnalysis(userId: String, contractId: String): String {
        // Contract 정보 조회
        val contract = firestore.collection("users")
            .document(userId)
            .collection("contracts")
            .document(contractId)
            .get()
            .await()
            .toObject(Contract::class.java) ?: throw IllegalStateException("Contract not found")

        // 분석 요청 생성
        val analysisRequest = hashMapOf(
            "userId" to userId,
            "contractId" to contractId,
            "buildingRegistry" to (contract.building_registry.firstOrNull()?.imageUrl),
            "registryDocument" to (contract.registry_document.firstOrNull()?.imageUrl),
            "contract" to (contract.contract.firstOrNull()?.imageUrl),
            "status" to "pending",
            "createdAt" to System.currentTimeMillis()
        )

        // Firestore에 분석 요청 저장
        val analysisRef = firestore.collection("analyses")
            .add(analysisRequest)
            .await()

        // Contract 상태 업데이트
        firestore.collection("users")
            .document(userId)
            .collection("contracts")
            .document(contractId)
            .update("status", ContractStatus.PENDING.name)
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

    suspend fun updateAnalysisResult(
        userId: String,
        contractId: String,
        analysisId: String,
        result: Map<String, Any>
    ) {
        // 분석 결과 업데이트
        firestore.collection("analyses")
            .document(analysisId)
            .update(
                mapOf(
                    "result" to result,
                    "status" to "completed"
                )
            )
            .await()

        // Contract 분석 결과 업데이트
        firestore.collection("users")
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
    }

    suspend fun updateAnalysisStatus(userId: String, contractId: String): String {
        try {
            // 분석 요청 ID 조회
            val contract = firestore.collection("users")
                .document(userId)
                .collection("contracts")
                .document(contractId)
                .get()
                .await()
                .toObject(Contract::class.java) ?: throw IllegalStateException("Contract not found")

            val analysisId = contract.analysisId
                ?: throw IllegalStateException("Analysis ID not found")

            // 분석 상태 조회
            val analysis = firestore.collection("analyses")
                .document(analysisId)
                .get()
                .await()

            val status = analysis.getString("status") ?: "pending"

            // 계약서 상태 업데이트
            if (status == "completed" && contract.analysisStatus != "COMPLETE") {
                // 분석 결과 가져오기
                val result = analysis.get("result") as? Map<String, Any> ?: emptyMap()

                // 계약 업데이트
                firestore.collection("users")
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
            }

            return status
        } catch (e: Exception) {
            println("분석 상태 업데이트 중 오류: ${e.message}")
            return "error"
        }
    }

}