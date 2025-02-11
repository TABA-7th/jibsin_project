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
    private val storageRef = storage.reference.child("scanned_documents")
    private val documentScanner = DocumentScanner()

    init {
        storage.maxUploadRetryTimeMillis = 50000
    }

    suspend fun uploadScannedImage(bitmap: Bitmap, documentType: String): String {
        val scannedBitmap = documentScanner.scanDocument(bitmap)
        return uploadImage(scannedBitmap, documentType)
    }

    suspend fun uploadScannedImageFromUri(uri: Uri, context: Context, documentType: String): String {
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            android.graphics.BitmapFactory.decodeStream(it)
        } ?: throw IllegalStateException("Failed to read image file")

        return uploadScannedImage(bitmap, documentType)
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
}