package com.project.jibsin_project.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID

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
        userId: String,
        contractId: String,
        pageNumber: Int
    ): String {
        val scannedBitmap = documentScanner.scanDocument(bitmap, documentType)
        val croppedBitmap = cropBitmapEdges(scannedBitmap)

        val baos = ByteArrayOutputStream()
        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val fileName = "${documentType}_page${pageNumber}.jpg"
        val imageRef = storageRef.child("$contractId/$fileName")

        return try {
            imageRef.putBytes(data).await()
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun uploadScannedImageFromUri(
        uri: Uri,
        context: Context,
        documentType: String,
        userId: String,
        contractId: String,
        pageNumber: Int
    ): String {
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: throw IllegalStateException("Failed to read image file")

        return uploadScannedImage(bitmap, documentType, userId, contractId, pageNumber)
    }

    private fun cropBitmapEdges(bitmap: Bitmap): Bitmap {
        val cropMargin = 0
        return Bitmap.createBitmap(
            bitmap,
            cropMargin,
            cropMargin,
            bitmap.width - (2 * cropMargin),
            bitmap.height - (2 * cropMargin)
        )
    }

    suspend fun deleteDocument(contractId: String, documentType: String, pageNumber: Int) {
        val ref = storageRef.child("$contractId/${documentType}_page${pageNumber}.jpg")
        try {
            try {
                ref.metadata.await()
            } catch (e: Exception) {
                return
            }
            ref.delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
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

    suspend fun getDownloadUrl(contractId: String, documentType: String, pageNumber: Int): String? {
        return try {
            val ref = storageRef.child("$contractId/${documentType}_page${pageNumber}.jpg")
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            null
        }
    }
}