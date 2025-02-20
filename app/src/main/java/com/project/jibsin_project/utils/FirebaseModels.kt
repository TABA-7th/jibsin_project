package com.project.jibsin_project.utils

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
/*
data class ScannedDocument(
    val type: String,
    val imageUrl: String,
    val uploadDate: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
    val status: String = "scanning",
    val userId: String,
    val groupId: String,  // 문서 그룹 ID 추가
    val pageNumber: Int,  // 페이지 번호 추가
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

    suspend fun createDocumentGroup(groupId: String, userId: String): DocumentGroup {
        val group = DocumentGroup(
            groupId = groupId,
            userId = userId
        )

        try {
            db.collection("document_groups")
                .document(groupId)
                .set(group)
                .await()
            return group
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateDocument(documentId: String, updates: Map<String, Any>) {
        try {
            db.collection("scanned_documents")
                .document(documentId)
                .update(updates)
                .await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun deleteDocument(documentId: String) {
        try {
            db.collection("scanned_documents")
                .document(documentId)
                .delete()
                .await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getDocument(documentId: String): DocumentSnapshot? {
        return try {
            db.collection("scanned_documents")
                .document(documentId)
                .get()
                .await()
        } catch (e: Exception) {
            null
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

    // 문서 그룹의 모든 문서 가져오기
    suspend fun getDocumentsInGroup(groupId: String): List<ScannedDocument> {
        return try {
            db.collection("scanned_documents")
                .whereEqualTo("groupId", groupId)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(ScannedDocument::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 특정 타입의 문서들 가져오기
    suspend fun getDocumentsByType(groupId: String, type: String): List<ScannedDocument> {
        return try {
            db.collection("scanned_documents")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("type", type)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(ScannedDocument::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
*/