package com.project.jibsin_project

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("계약서 업로드", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { /* 햄버거 메뉴 로직 */ }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFFF9F9F9))
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF9F9F9))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .padding(bottom = 70.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HomeCard(
                        icon = painterResource(id = R.drawable.ic_calendar),
                        label = "월세",
                        onClick = { /* 월세 화면 이동 */ }
                    )
                    HomeCard(
                        icon = painterResource(id = R.drawable.ic_home),
                        label = "전세",
                        onClick = { /* 전세 화면 이동 */ }
                    )
                }
            }

            // 챗봇 버튼
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 64.dp), // 아래로 더 띄움
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 말풍선과 챗봇 아이콘
                Box(
                    modifier = Modifier
                        .size(80.dp) // 말풍선 크기
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(40.dp)) // 그림자 효과
                        .background(Color.White, shape = RoundedCornerShape(40.dp)), // 흰색 배경
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_ai_chatbot),
                        contentDescription = "AI 챗봇",
                        tint = Color.Unspecified, // 이미지 색상 변경 없이 원래대로 표시
                        modifier = Modifier.size(60.dp) // 챗봇 아이콘 크기
                    )

                }

                // AI 챗봇 텍스트
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "AI 챗봇",
                    fontSize = 15.sp,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun HomeCard(icon: Painter, label: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .size(140.dp)
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewHomeScreen() {
    HomeScreen()
}