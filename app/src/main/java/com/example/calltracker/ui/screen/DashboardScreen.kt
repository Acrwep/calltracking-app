package com.example.calltracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.calltracker.ui.theme.ActivePill
import com.example.calltracker.ui.theme.AppBackground
import com.example.calltracker.ui.theme.TextPrimary
import com.example.calltracker.ui.theme.TextSecondary
import com.example.calltracker.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel = viewModel()) {
    var selectedTab by remember { mutableStateOf("SMS") }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CallTracker", color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground),
                actions = {
                    IconButton(onClick = { 
                        viewModel.triggerManualSync() 
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextPrimary)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = AppBackground,
                contentColor = TextSecondary
            ) {
                NavigationBarItem(
                    selected = selectedTab == "SMS",
                    onClick = { selectedTab = "SMS" },
                    icon = { Icon(Icons.Default.Mail, contentDescription = "SMS") },
                    label = { Text("SMS") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TextPrimary,
                        unselectedIconColor = TextSecondary,
                        selectedTextColor = TextPrimary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = ActivePill
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == "Calls",
                    onClick = { selectedTab = "Calls" },
                    icon = { Icon(Icons.Default.Call, contentDescription = "Calls") },
                    label = { Text("Calls") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TextPrimary,
                        unselectedIconColor = TextSecondary,
                        selectedTextColor = TextPrimary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = ActivePill
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == "Recordings",
                    onClick = { selectedTab = "Recordings" },
                    icon = { Icon(Icons.Default.Mic, contentDescription = "Recordings") },
                    label = { Text("Recordings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TextPrimary,
                        unselectedIconColor = TextSecondary,
                        selectedTextColor = TextPrimary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = ActivePill
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == "Apps",
                    onClick = { selectedTab = "Apps" },
                    icon = { Icon(Icons.Default.Apps, contentDescription = "App Usage") },
                    label = { Text("App Usage") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TextPrimary,
                        unselectedIconColor = TextSecondary,
                        selectedTextColor = TextPrimary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = ActivePill
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == "Notifications",
                    onClick = { selectedTab = "Notifications" },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Notifications") },
                    label = { Text("Notifs") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TextPrimary,
                        unselectedIconColor = TextSecondary,
                        selectedTextColor = TextPrimary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = ActivePill
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == "WhatsApp",
                    onClick = { selectedTab = "WhatsApp" },
                    icon = { Icon(Icons.Default.Chat, contentDescription = "WhatsApp") },
                    label = { Text("WhatsApp") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TextPrimary,
                        unselectedIconColor = TextSecondary,
                        selectedTextColor = TextPrimary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = ActivePill
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    viewModel.triggerManualSync()
                    coroutineScope.launch {
                        delay(1000)
                        isRefreshing = false
                    }
                }
            ) {
                when (selectedTab) {
                    "SMS" -> SmsTab(viewModel)
                    "Calls" -> CallsTab(viewModel)
                    "Recordings" -> RecordingsTab(viewModel)
                    "Apps" -> AppUsageTab(viewModel)
                    "Notifications" -> NotificationsTab(viewModel)
                    "WhatsApp" -> WhatsAppTab(viewModel)
                }
            }
        }
    }
}

fun formatTimestampShort(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
