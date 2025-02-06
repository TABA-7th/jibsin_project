package com.project.jibsin_project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onNavigateToSignUp: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isAutoLoginChecked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(80.dp)) // 로고 상단 여백
        // 로고
        Icon(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .padding(bottom = 40.dp), // 아이디 입력란과의 공백 증가
            tint = Color.Unspecified
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
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF253F5A), // 테두리 색상
                cursorColor = Color(0xFF253F5A), // 커서 색상
                focusedLabelColor = Color(0xFF253F5A), // 포커스된 라벨 색상
                unfocusedLabelColor = Color.Gray // 비포커스 상태 라벨 색상
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 자동 로그인 및 비밀번호 찾기
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isAutoLoginChecked,
                onCheckedChange = { isAutoLoginChecked = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF253F5A) // 시그니처 색상
                )
            )
            Text("자동 로그인", modifier = Modifier.weight(1f))
            TextButton(
                onClick = { /* 비밀번호 찾기 로직 */ },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF253F5A)) // 시그니처 색상
            ) {
                Text("비밀번호 찾기")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 로그인 버튼
        Button(
            onClick = { /* 로그인 로직 */ },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF253F5A)), // 시그니처 색상
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("로그인", color = Color.White)
        }

        Spacer(modifier = Modifier.height(32.dp)) // 로그인 버튼과 다른 방법으로 로그인 사이 공백 증가

        // 다른 방법으로 로그인
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(
                modifier = Modifier.weight(1f),
                color = Color.Gray,
                thickness = 1.dp
            )
            Text(
                "다른 방법으로 로그인",
                modifier = Modifier.padding(horizontal = 8.dp),
                color = Color.Gray
            )
            Divider(
                modifier = Modifier.weight(1f),
                color = Color.Gray,
                thickness = 1.dp
            )
        }

        // 카카오 로그인 버튼
        Button(
            onClick = { /* 카카오 로그인 로직 */ },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE812)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_kakao_logo),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Kakao로 로그인하기", color = Color.Black)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 회원가입
        TextButton(
            onClick = onNavigateToSignUp,
            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF253F5A)) // 시그니처 색상
        ) {
            Text("회원가입")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLoginScreen() {
    LoginScreen(onNavigateToSignUp = {})
}
