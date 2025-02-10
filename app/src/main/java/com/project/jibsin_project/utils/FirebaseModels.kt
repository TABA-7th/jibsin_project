package com.project.jibsin_project.utils

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

data class ScannedDocument(
    val type: String,
    val imageUrl: String,
    val uploadDate: Timestamp = Timestamp.now(),
    val status: String = "scanning",
    val userId: String,  // 실제 구현시 로그인한 사용자 ID를 사용
    val result: Map<String, Any>? = null
)

class FirestoreUtil {
    private val db = Firebase.firestore

    suspend fun saveScannedDocument(document: ScannedDocument): String {
        return try {
            val docRef = db.collection("scanned_documents").add(document).await()
            docRef.id
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateAnalysisResult(documentId: String, result: Map<String, Any>) {
        try {
            db.collection("scanned_documents")
                .document(documentId)
                .update(mapOf(
                    "status" to "completed",
                    "result" to result
                ))
                .await()
        } catch (e: Exception) {
            throw e
        }
    }
}