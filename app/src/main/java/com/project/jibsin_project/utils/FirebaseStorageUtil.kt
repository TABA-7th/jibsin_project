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

    suspend fun uploadScannedImage(bitmap: Bitmap, documentType: String): String {
        // 스캔 처리
        val scannedBitmap = documentScanner.scanDocument(bitmap)

        // 추가적인 크롭 처리
        val croppedBitmap = cropBitmapEdges(scannedBitmap)

        return uploadImage(croppedBitmap, documentType)
    }

    suspend fun uploadScannedImageFromUri(uri: Uri, context: Context, documentType: String): String {
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: throw IllegalStateException("Failed to read image file")

        return uploadScannedImage(bitmap, documentType)
    }

    private suspend fun uploadImage(bitmap: Bitmap, documentType: String): String {
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

    private fun cropBitmapEdges(bitmap: Bitmap): Bitmap {
        // 여백 완전 제거를 위한 추가 크롭
        val cropMargin = 0
        return Bitmap.createBitmap(
            bitmap,
            cropMargin,
            cropMargin,
            bitmap.width - (2 * cropMargin),
            bitmap.height - (2 * cropMargin)
        )
    }
}