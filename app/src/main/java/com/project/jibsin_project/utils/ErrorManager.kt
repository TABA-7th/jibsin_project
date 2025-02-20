package com.project.jibsin_project.utils

sealed class UploadError : Exception() {
    object NetworkError : UploadError()
    object StorageError : UploadError()
    object InvalidDocumentError : UploadError()
    data class UnknownError(override val message: String?) : UploadError()
}

fun UploadError.toUserMessage(): String = when (this) {
    is UploadError.NetworkError -> "네트워크 연결을 확인해주세요."
    is UploadError.StorageError -> "저장소 오류가 발생했습니다. 다시 시도해주세요."
    is UploadError.InvalidDocumentError -> "올바르지 않은 문서입니다. 다시 확인해주세요."
    is UploadError.UnknownError -> message ?: "알 수 없는 오류가 발생했습니다."
}