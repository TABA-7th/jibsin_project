package com.project.jibsin_project.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID

class FirebaseStorageUtil {
    private val storage = Firebase.storage
    private val storageRef = storage.reference.child("scanned_documents") // Root reference

    init {
        // Storage 버킷 URL 설정 (필요한 경우)
        storage.maxUploadRetryTimeMillis = 50000
    }

    suspend fun uploadImage(bitmap: Bitmap, documentType: String): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val imageRef = storageRef.child("$documentType/${UUID.randomUUID()}.jpg")
        return try {
            imageRef.putBytes(data).await()
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun uploadImageFromUri(uri: Uri, context: Context, documentType: String): String {
        val imageRef = storageRef.child("$documentType/${UUID.randomUUID()}.jpg")
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                imageRef.putStream(stream).await()
                imageRef.downloadUrl.await().toString()
            } ?: throw IllegalStateException("Failed to read image file")
        } catch (e: Exception) {
            throw e
        }
    }
}