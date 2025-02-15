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
        groupId: String = UUID.randomUUID().toString(),
        pageNumber: Int = 1
    ): String {
        val scannedBitmap = documentScanner.scanDocument(bitmap, documentType)
        val croppedBitmap = cropBitmapEdges(scannedBitmap)
        return uploadImage(croppedBitmap, documentType, groupId, pageNumber)
    }

    suspend fun uploadScannedImageFromUri(
        uri: Uri,
        context: Context,
        documentType: String,
        groupId: String = UUID.randomUUID().toString(),
        pageNumber: Int = 1
    ): String {
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: throw IllegalStateException("Failed to read image file")

        return uploadScannedImage(bitmap, documentType, groupId, pageNumber)
    }

    private suspend fun uploadImage(
        bitmap: Bitmap,
        documentType: String,
        groupId: String,
        pageNumber: Int
    ): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val fileName = "${documentType}_page${pageNumber}.jpg"
        val imageRef = storageRef.child("$groupId/$fileName")

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

    suspend fun updatePageNumber(
        groupId: String,
        documentType: String,
        oldPageNumber: Int,
        newPageNumber: Int
    ): String {
        val oldRef = storageRef.child("$groupId/${documentType}_page${oldPageNumber}.jpg")
        val newRef = storageRef.child("$groupId/${documentType}_page${newPageNumber}.jpg")

        try {
            // 기존 파일의 URL 가져오기
            val originalUrl = oldRef.downloadUrl.await().toString()

            // 파일 복사
            val bytes = oldRef.getBytes(10L * 1024 * 1024).await() // 10MB limit
            newRef.putBytes(bytes).await()

            // 기존 파일 삭제
            oldRef.delete().await()

            // 새 파일의 URL 반환
            return newRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun deleteDocument(groupId: String, documentType: String, pageNumber: Int) {
        val ref = storageRef.child("$groupId/${documentType}_page${pageNumber}.jpg")
        try {
            // 파일이 존재하는지 먼저 확인
            try {
                ref.metadata.await()
            } catch (e: Exception) {
                // 파일이 없으면 그냥 반환
                return
            }

            // 파일이 존재하면 삭제 시도
            ref.delete().await()
        } catch (e: Exception) {
            // 삭제 실패해도 크래시 방지
            e.printStackTrace()
        }
    }
}