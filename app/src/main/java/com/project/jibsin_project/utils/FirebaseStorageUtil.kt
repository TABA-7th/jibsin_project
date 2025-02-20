package com.project.jibsin_project.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class FirebaseStorageUtil {
    private val storage = Firebase.storage
    private val storageRef = storage.reference.child("scanned_documents")
    private val documentScanner = DocumentScanner()

    init {
        storage.maxUploadRetryTimeMillis = 50000
    }

    suspend fun uploadScannedImage(
        bitmap: Bitmap,
        documentType: String,
        contractId: String,
        pageNumber: Int
    ): String {
        // 문서 스캔 처리
        val scannedBitmap = documentScanner.scanDocument(bitmap, documentType)
        return uploadImage(scannedBitmap, documentType, contractId, pageNumber)
    }

    suspend fun uploadScannedImageFromUri(
        uri: Uri,
        context: Context,
        documentType: String,
        contractId: String,
        pageNumber: Int
    ): String {
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: throw IllegalStateException("Failed to read image file")

        return uploadScannedImage(bitmap, documentType, contractId, pageNumber)
    }

    private suspend fun uploadImage(
        bitmap: Bitmap,
        documentType: String,
        contractId: String,
        pageNumber: Int
    ): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        // 파일 경로 구조: scanned_documents/{contractId}/{documentType}_page{number}.jpg
        val fileName = "${documentType}_page${pageNumber}.jpg"
        val imageRef = storageRef.child("$contractId/$fileName")

        return try {
            imageRef.putBytes(data).await()
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updatePageNumber(
        contractId: String,
        documentType: String,
        oldPageNumber: Int,
        newPageNumber: Int
    ): String {
        val oldRef = storageRef.child("$contractId/${documentType}_page${oldPageNumber}.jpg")
        val newRef = storageRef.child("$contractId/${documentType}_page${newPageNumber}.jpg")

        try {
            val bytes = oldRef.getBytes(10L * 1024 * 1024).await()
            newRef.putBytes(bytes).await()
            oldRef.delete().await()
            return newRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun deleteDocument(
        contractId: String,
        documentType: String,
        pageNumber: Int
    ) {
        val ref = storageRef.child("$contractId/${documentType}_page${pageNumber}.jpg")
        try {
            ref.delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}