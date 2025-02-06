package com.project.jibsin_project.chatbot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
        "전세 대출 관련 질문" to listOf("필요한 서류는?", "대출 한도는?"),
        "계약 문제 해결 방법" to listOf("위반 시 대처는?", "해지 조건은?"),
        "기타 문의 사항" to listOf("기본 조건은?", "주의사항은?")
    )
    var showFaq by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

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
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF2F2F2)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showFaq) {
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "자주 묻는 질문",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        faqList.forEach { (question, subQuestions) ->
                            var expanded by remember { mutableStateOf(false) }
                            // 클릭 영역을 항목 필드 전체로 확장
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { expanded = !expanded },
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = question,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null
                                        )
                                    }
                                    if (expanded) {
                                        subQuestions.forEach { subQuestion ->
                                            TextButton(onClick = {
                                                chatMessages.add("$subQuestion")
                                                chatMessages.add("'$subQuestion'에 대한 답변입니다.")
                                                showFaq = false
                                            }) {
                                                Text(subQuestion, color = Color(0xFF253F5A))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showFaq = !showFaq },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "자주 묻는 질문", tint = Color(0xFF253F5A))
                    }
                    OutlinedTextField(
                        value = userMessage,
                        onValueChange = { userMessage = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp, end = 8.dp),
                        placeholder = { Text("AI 챗봇에 무엇이든 물어보세요!") },
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            containerColor = Color.White,
                            focusedBorderColor = Color(0xFF253F5A),
                            unfocusedBorderColor = Color.LightGray
                        ),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (userMessage.text.isNotEmpty()) {
                                        chatMessages.add("${userMessage.text}")
                                        chatMessages.add("질문을 이해했어요! 답변을 준비 중입니다.")
                                        userMessage = TextFieldValue("")
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "전송", tint = Color(0xFF253F5A))
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF9F9F9))
        ) {
            items(chatMessages.size) { index ->
                val message = chatMessages[index]
                ChatBubble(message = message, isUserMessage = index % 2 != 0)
            }
        }

        // 화면 스크롤
        LaunchedEffect(chatMessages.size) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }
}

@Composable
fun ChatBubble(message: String, isUserMessage: Boolean) {
    Box(
        contentAlignment = if (isUserMessage) Alignment.CenterEnd else Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 8.dp)
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

