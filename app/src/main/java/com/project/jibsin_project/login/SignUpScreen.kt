package com.project.jibsin_project.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
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
        Spacer(modifier = Modifier.height(50.dp)) // 상단 여백

        // 제목
        Text(
            text = "회원가입",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 24.dp),
            color = Color(0xFF253F5A)
        )

        // 아이디 입력
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("아이디") },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF253F5A), // 테두리 색상
                cursorColor = Color(0xFF253F5A), // 커서 색상
                focusedLabelColor = Color(0xFF253F5A), // 포커스된 라벨 색상
                unfocusedLabelColor = Color.Gray // 비포커스 상태 라벨 색상
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 1.dp)
        )

        // 비밀번호 입력
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("비밀번호") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF253F5A), // 테두리 색상
                cursorColor = Color(0xFF253F5A), // 커서 색상
                focusedLabelColor = Color(0xFF253F5A), // 포커스된 라벨 색상
                unfocusedLabelColor = Color.Gray // 비포커스 상태 라벨 색상
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        )

        // 약관 체크박스와 문구
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = isTermsChecked,
                onCheckedChange = { isTermsChecked = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF253F5A),
                    uncheckedColor = Color(0xFF253F5A)
                )
            )
            Spacer(modifier = Modifier.width(0.dp))
            Text(
                text = "본 어플은 사용자 편의를 위한 AI 기반 계약서 검토 서비스로, 법률 자문을 제공하지 않으며 법적 효력을 보장하지 않습니다. 최종 계약 전 반드시 법률 전문가와 상담하시길 권장합니다.",
                modifier = Modifier.weight(1f),
                fontSize = 12.sp,
                textAlign = TextAlign.Start,
                color = Color.DarkGray
            )
        }

        // 회원가입 버튼
        Button(
            onClick = {
                if (isTermsChecked) {
                    // 회원가입 완료 로직 추가
                    onSignUpComplete()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = isTermsChecked,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF253F5A))
        ) {
            Text("회원가입", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSignUpScreen() {
    SignUpScreen(onSignUpComplete = {})
}
