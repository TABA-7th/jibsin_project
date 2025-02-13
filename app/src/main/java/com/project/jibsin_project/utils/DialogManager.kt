package com.project.jibsin_project.utils

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("오류") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("확인", color = Color(0xFF253F5A))
            }
        }
    )
}

@Composable
fun ProgressDialog(
    message: String = "처리 중입니다..."
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("잠시만 기다려주세요") },
        text = { Text(message) },
        confirmButton = { }
    )
}