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
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import java.util.Calendar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calltracker.ui.theme.*
import com.example.calltracker.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsTab(viewModel: MainViewModel) {
    val callLogs by viewModel.allCallLogs.collectAsState(initial = emptyList())
    val searchQuery by viewModel.callsSearchQuery.collectAsState()
    val currentTimeFilter by viewModel.callsTimeFilter.collectAsState()
    val currentTypeFilter by viewModel.callsTypeFilter.collectAsState()

    val timeFilters = listOf("All time", "Today", "Last 7 days")
    val typeFilters = listOf("All", "Incoming", "Outgoing", "Missed")

    val filteredLogs = callLogs.filter { log ->
        val matchesSearch = log.phoneNumber.contains(searchQuery, ignoreCase = true) ||
                (log.contactName?.contains(searchQuery, ignoreCase = true) == true)
        val matchesType = currentTypeFilter == "All" || log.callType.equals(currentTypeFilter, ignoreCase = true)
        
        val matchesTime = when (currentTimeFilter) {
            "Today" -> {
                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                log.timestamp >= todayStart
            }
            "Last 7 days" -> {
                val sevenDaysAgo = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -7)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                log.timestamp >= sevenDaysAgo
            }
            else -> true
        }
        
        matchesSearch && matchesType && matchesTime
    }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.callsSearchQuery.value = it },
            placeholder = { Text("Search by contact or number...", color = TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.callsSearchQuery.value = "" }) {
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

        // Time Filters
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(timeFilters) { filter ->
                FilterChipCustom(
                    text = filter,
                    isSelected = currentTimeFilter == filter,
                    onClick = { viewModel.callsTimeFilter.value = filter }
                )
            }
        }

        // Type Filters
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(typeFilters) { filter ->
                FilterChipCustom(
                    text = filter,
                    isSelected = currentTypeFilter == filter,
                    onClick = { viewModel.callsTypeFilter.value = filter }
                )
            }
        }

        // Call List
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
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon based on type
                        val icon = when (log.callType.lowercase()) {
                            "incoming" -> Icons.Default.CallReceived
                            "outgoing" -> Icons.Default.CallMade
                            "missed" -> Icons.Default.CallMissed
                            else -> Icons.Default.CallReceived
                        }
                        val iconColor = when (log.callType.lowercase()) {
                            "outgoing" -> StatusBlue
                            "incoming" -> StatusBlue
                            "missed" -> TextSecondary
                            else -> TextSecondary
                        }

                        Icon(
                            imageVector = icon,
                            contentDescription = log.callType,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = log.contactName ?: log.phoneNumber, // Would map to contact name if available
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = log.phoneNumber,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Format duration "MM:SS"
                            val mins = log.durationSeconds / 60
                            val secs = log.durationSeconds % 60
                            Text(
                                text = String.format("%02d:%02d", mins, secs),
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Text(
                            text = formatTimestampShort(log.timestamp),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChipCustom(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) ActivePill else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) Color.Transparent else TextSecondary.copy(alpha = 0.5f),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) TextPrimary else TextSecondary,
            fontSize = 14.sp
        )
    }
}
