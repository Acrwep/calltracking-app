package com.example.calltracker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calltracker.ui.theme.*
import com.example.calltracker.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsTab(viewModel: MainViewModel) {
    val smsLogs by viewModel.allSmsLogs.collectAsState(initial = emptyList())
    val searchQuery by viewModel.smsSearchQuery.collectAsState()
    val currentFilter by viewModel.smsFilter.collectAsState()

    val filters = listOf("All", "Received", "Sent", "Draft", "Failed")

    val filteredLogs = smsLogs.filter { log ->
        val matchesSearch = log.senderNumber.contains(searchQuery, ignoreCase = true) ||
                (log.contactName?.contains(searchQuery, ignoreCase = true) == true) ||
                log.messageBody.contains(searchQuery, ignoreCase = true)
        val matchesFilter = currentFilter == "All" || 
                (currentFilter == "Received" && log.type.equals("Inbox", ignoreCase = true)) || 
                log.type.equals(currentFilter, ignoreCase = true)
        matchesSearch && matchesFilter
    }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.smsSearchQuery.value = it },
            placeholder = { Text("Search SMS...", color = TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.smsSearchQuery.value = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TextSecondary,
                unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = TextPrimary,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )

        // Filter Chips
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { filter ->
                val isSelected = currentFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) ActivePill else Color.Transparent)
                        .border(
                            1.dp,
                            if (isSelected) Color.Transparent else TextSecondary.copy(alpha = 0.5f),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { viewModel.smsFilter.value = filter }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = filter,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // SMS List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredLogs) { log ->
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
                                text = log.contactName ?: log.senderNumber,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = formatTimestampShort(log.timestamp),
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = log.messageBody,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Status: ${log.type}",
                                color = if (log.type.equals("Inbox", true)) StatusGreen else TextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "SMS",
                                color = TextSecondary,
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
