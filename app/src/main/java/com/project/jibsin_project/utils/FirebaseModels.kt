package com.project.jibsin_project.utils

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

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

    suspend fun updateDocumentGroup(groupId: String, updates: Map<String, Any>) {
        try {
            db.collection("document_groups")
                .document(groupId)
                .update(updates)
                .await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getDocumentGroup(groupId: String): DocumentGroup? {
        return try {
            val doc = db.collection("document_groups")
                .document(groupId)
                .get()
                .await()
            doc.toObject(DocumentGroup::class.java)
        } catch (e: Exception) {
            null
        }
    }
}