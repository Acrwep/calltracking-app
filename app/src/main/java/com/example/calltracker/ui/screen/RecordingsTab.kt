package com.example.calltracker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
fun RecordingsTab(viewModel: MainViewModel) {
    val recordings by viewModel.allRecordings.collectAsState(initial = emptyList())
    val currentlyPlaying by viewModel.currentlyPlayingPath.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        
        Text(
            text = "Call Recordings",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(recordings) { recording ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = recording.relatedPhoneNumber ?: "Unknown",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = recording.relatedPhoneNumber ?: "Unknown",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            
                            Text(
                                text = formatTimestampShort(recording.timestamp),
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isBlocked = recording.durationSeconds == 0L
                            IconButton(
                                onClick = { viewModel.playAudio(recording.filePath) },
                                enabled = !isBlocked
                            ) {
                                Icon(
                                    imageVector = if (currentlyPlaying == recording.filePath) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Stop",
                                    tint = if (isBlocked) TextSecondary else TextPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = if (isBlocked) "Status: BLOCKED BY OS" else "Duration: ${recording.durationSeconds}s",
                                color = if (isBlocked) StatusRed else TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
