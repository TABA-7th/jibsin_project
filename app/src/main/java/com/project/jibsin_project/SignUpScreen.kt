package com.project.jibsin_project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SignUpScreen(onSignUpComplete: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isTermsChecked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 제목
        Text(
            text = "회원가입",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 아이디 입력
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("아이디") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 비밀번호 입력
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("비밀번호") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 약관 체크박스
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isTermsChecked,
                onCheckedChange = { isTermsChecked = it }
            )
            Text("본 어플은 사용자 편의를 위한 AI 기반 계약서 검토 서비스로, " +
                    "법률 자문을 제공하지 않으며 법적 효력을 보장하지 않습니다. " +
                    "최종 계약 전 반드시 법률 전문가와 상담하시길 권장합니다.")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 회원가입 버튼
        Button(
            onClick = {
                if (isTermsChecked) {
                    // 회원가입 완료 로직 추가
                    onSignUpComplete()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isTermsChecked
        ) {
            Text("회원가입")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSignUpScreen() {
    SignUpScreen(onSignUpComplete = {})
}
