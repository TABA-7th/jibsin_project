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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class ChatBotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatBotScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBotScreen() {
    var userMessage by remember { mutableStateOf(TextFieldValue("")) }
    val chatMessages = remember { mutableStateListOf("안녕하세요! 무엇을 도와드릴까요?") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 챗봇", fontSize = 20.sp, color = Color.White) },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color(0xFF253F5A) // 배경 색상
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

            // 입력 필드 및 전송 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userMessage,
                    onValueChange = { userMessage = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    placeholder = { Text("메시지를 입력하세요") },
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
                            chatMessages.add("챗봇: 질문을 이해했어요!")
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
    ChatBotScreen()
}
