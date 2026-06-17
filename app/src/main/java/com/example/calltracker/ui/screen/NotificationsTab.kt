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
fun NotificationsTab(viewModel: MainViewModel) {
    val notifications by viewModel.allNotifications.collectAsState(initial = emptyList())
    val searchQuery by viewModel.notificationSearchQuery.collectAsState()
    val currentFilter by viewModel.notificationAppFilter.collectAsState()

    // Extract dynamic filters based on app names in notifications
    val appNames = notifications.map { it.appName }.distinct().sorted()
    val filters = mutableListOf("All").apply { addAll(appNames) }

    val filteredNotifications = notifications.filter { notif ->
        val matchesSearch = notif.appName.contains(searchQuery, ignoreCase = true) ||
                (notif.title?.contains(searchQuery, ignoreCase = true) == true) ||
                (notif.text?.contains(searchQuery, ignoreCase = true) == true) ||
                (notif.bigText?.contains(searchQuery, ignoreCase = true) == true)
        val matchesFilter = currentFilter == "All" || notif.appName == currentFilter
        matchesSearch && matchesFilter
    }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.notificationSearchQuery.value = it },
            placeholder = { Text("Search Notifications...", color = TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.notificationSearchQuery.value = "" }) {
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
                val count = if (filter == "All") notifications.size else notifications.count { it.appName == filter }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) ActivePill else Color.Transparent)
                        .border(
                            1.dp,
                            if (isSelected) Color.Transparent else TextSecondary.copy(alpha = 0.5f),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { viewModel.notificationAppFilter.value = filter }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "$filter ($count)",
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Notification List
        if (filteredNotifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No notifications found.", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredNotifications, key = { it.id }) { notif ->
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
                                    text = notif.appName,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = formatTimestampShort(notif.timestamp),
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (!notif.title.isNullOrBlank()) {
                                Text(
                                    text = notif.title,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            }
                            
                            val displayMessage = notif.bigText?.takeIf { it.isNotBlank() } ?: notif.text
                            if (!displayMessage.isNullOrBlank()) {
                                Text(
                                    text = displayMessage,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    maxLines = 5,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (notif.isRemoved) "Removed" else "Active",
                                    color = if (notif.isRemoved) StatusRed else StatusGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = notif.packageName,
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
