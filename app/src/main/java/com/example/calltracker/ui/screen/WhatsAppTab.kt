package com.example.calltracker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calltracker.ui.theme.*
import com.example.calltracker.ui.viewmodel.MainViewModel

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



@Composable
fun WhatsAppTab(viewModel: MainViewModel) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Chats", "Call Recordings")

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = AppBackground,
            contentColor = TextPrimary,
            indicator = { tabPositions ->
                if (selectedTabIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = ActivePill
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, color = if (selectedTabIndex == index) TextPrimary else TextSecondary) }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (selectedTabIndex) {
                0 -> WhatsAppChatsList(viewModel)
                1 -> WhatsAppCallRecordingsList(viewModel)
            }
        }
    }
}

@Composable
fun WhatsAppChatsList(viewModel: MainViewModel) {
    val chats by viewModel.allWhatsAppChats.collectAsState(initial = emptyList())
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()) }

    if (chats.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No WhatsApp messages captured yet", color = TextSecondary)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chats) { chat ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = chat.packageName,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = sdf.format(Date(chat.timestamp)),
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = chat.contactName,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        
                        Text(
                            text = chat.messageText,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = chat.status,
                                color = StatusGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WhatsAppCallRecordingsList(viewModel: MainViewModel) {
    val calls by viewModel.allWhatsAppCalls.collectAsState(initial = emptyList())
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()) }

    if (calls.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No communication records available", color = TextSecondary)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(calls) { call ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = call.contactName,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = sdf.format(Date(call.timestamp)),
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        
                        Text(text = call.number, color = TextSecondary, fontSize = 14.sp)
                        Text(text = "Direction: ${call.direction} | Session Type: ${call.sessionType}", color = TextSecondary, fontSize = 14.sp)
                        Text(text = "Duration: ${call.duration}", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
