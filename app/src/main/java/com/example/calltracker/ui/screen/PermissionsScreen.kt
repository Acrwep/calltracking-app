package com.example.calltracker.ui.screen

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun PermissionsScreen(onAllPermissionsGranted: (Int, Intent) -> Unit) {
    val context = LocalContext.current
    
    val requiredPermissions = mutableListOf(
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_PHONE_STATE
    )
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    var permissionsGranted by remember { mutableStateOf(false) }
    var usageAccessGranted by remember { mutableStateOf(false) }
    var notificationAccessGranted by remember { mutableStateOf(false) }

    fun checkPermissions() {
        val allStandardGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        permissionsGranted = allStandardGranted

        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        usageAccessGranted = mode == AppOpsManager.MODE_ALLOWED

        notificationAccessGranted = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

        if (permissionsGranted && usageAccessGranted && notificationAccessGranted) {
            // Reverted back to automatically moving on without Screen Capture for now
            onAllPermissionsGranted(Activity.RESULT_CANCELED, Intent())
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        checkPermissions()
    }

    val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    val screenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            onAllPermissionsGranted(result.resultCode, result.data!!)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to CallTracker",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "Please grant the following permissions to enable tracking features.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = {
                permissionLauncher.launch(requiredPermissions.toTypedArray())
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            enabled = !permissionsGranted
        ) {
            Text(if (permissionsGranted) "Standard Permissions Granted" else "Grant Standard Permissions")
        }

        Button(
            onClick = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            enabled = !usageAccessGranted
        ) {
            Text(if (usageAccessGranted) "Usage Access Granted" else "Grant App Usage Access")
        }

        Button(
            onClick = {
                val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            enabled = !notificationAccessGranted
        ) {
            Text(if (notificationAccessGranted) "Notification Access Granted" else "Grant Notification Access")
        }

        if (permissionsGranted && usageAccessGranted && notificationAccessGranted) {
            Button(onClick = {
                onAllPermissionsGranted(Activity.RESULT_CANCELED, Intent())
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Continue to Dashboard")
            }
            
            /* Commented out pending manager approval
            Button(onClick = {
                screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Grant Screen Capture & Continue")
            }
            */
        }
    }
}
