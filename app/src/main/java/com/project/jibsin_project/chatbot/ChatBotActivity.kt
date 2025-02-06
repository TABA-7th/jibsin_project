package com.project.jibsin_project.chatbot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class ChatBotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatBotScreen(onBackClick = { finish() })
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBotScreen(onBackClick: () -> Unit) {
    var userMessage by remember { mutableStateOf(TextFieldValue("")) }
    val chatMessages = remember { mutableStateListOf("안녕하세요! 😊 어떻게 도와드릴까요?") }
    val faqList = listOf(
        "전세 대출 관련 질문" to "전세 대출을 받을 때 필요한 서류는 무엇인가요?",
        "계약 문제 해결 방법" to "계약 위반 시 어떻게 대응해야 하나요?",
        "기타 문의 사항" to "임대차 계약의 기본 조건은 무엇인가요?"
    )
    var isFaqVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 챗봇", fontSize = 20.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로가기", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color(0xFF253F5A)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF9F9F9))
        ) {
            // 채팅 메시지 표시
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                for (message in chatMessages) {
                    ChatBubble(message = message, isUserMessage = chatMessages.indexOf(message) % 2 != 0)
                }
            }

            // 자주 묻는 질문 표시
            if (isFaqVisible) {
                Divider(color = Color.LightGray, thickness = 1.dp)
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "자주 묻는 질문",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    for ((question, answer) in faqList) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = question,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = answer,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 입력 필드 및 전송 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { isFaqVisible = !isFaqVisible },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "자주 묻는 질문 토글",
                        tint = Color(0xFF253F5A)
                    )
                }
                OutlinedTextField(
                    value = userMessage,
                    onValueChange = { userMessage = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    placeholder = { Text("AI 챗봇에 무엇이든 물어보세요!") },
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = Color.White,
                        focusedBorderColor = Color(0xFF253F5A),
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                FloatingActionButton(
                    onClick = {
                        if (userMessage.text.isNotEmpty()) {
                            chatMessages.add("나: ${userMessage.text}")
                            chatMessages.add("챗봇: 질문을 이해했어요! 답변을 준비 중입니다.")
                            userMessage = TextFieldValue("")
                        }
                    },
                    containerColor = Color(0xFF253F5A)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: String, isUserMessage: Boolean) {
    Box(
        contentAlignment = if (isUserMessage) Alignment.CenterEnd else Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUserMessage) Color(0xFF253F5A) else Color.White
            ),
            modifier = Modifier.widthIn(max = 250.dp)
        ) {
            Text(
                text = message,
                color = if (isUserMessage) Color.White else Color.Black,
                fontSize = 14.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewChatBotScreen() {
    ChatBotScreen(onBackClick = {})
}
