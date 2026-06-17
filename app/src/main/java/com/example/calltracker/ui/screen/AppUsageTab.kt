package com.example.calltracker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calltracker.ui.theme.*
import com.example.calltracker.ui.viewmodel.MainViewModel

@Composable
fun AppUsageTab(viewModel: MainViewModel) {
    val appUsages by viewModel.allAppUsage.collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        if (appUsages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No App Usage Tracked Yet", color = TextSecondary, fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(appUsages) { usage ->
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
                                    text = usage.appName.takeIf { it.isNotBlank() } ?: usage.packageName,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = formatTimestampShort(usage.timestamp),
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Package: ${usage.packageName}",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                            
                            if (usage.usageDurationMillis > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val minutes = usage.usageDurationMillis / (1000 * 60)
                                Text(
                                    text = "Duration: $minutes mins",
                                    color = StatusGreen,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
