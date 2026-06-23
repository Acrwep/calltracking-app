package com.example.calltracker.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.calltracker.repository.TrackerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class WhatsAppAccessibilityService : AccessibilityService() {

    private val serviceJob = Job()
    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("WhatsAppAccessibility", "Coroutine exception", throwable)
    }
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob + exceptionHandler)
    private lateinit var repository: TrackerRepository

    private var currentChatName: String = "Unknown"

    // WhatsApp Call Tracking State
    private var isCallActive = false
    private var callContactName = "Unknown"
    private var callNumber = "+91 00000 00000" // Hard to get real number from UI, fallback to generic
    private var callDirection = "Unknown"
    private var callSessionType = "Unknown"
    private var callDuration = "00:00"
    private var callStartTime = 0L

    // State to avoid inserting old messages/calls already on screen
    private val processedMessages = mutableSetOf<Int>()
    private val initializedChats = mutableSetOf<String>()
    private val processedCalls = mutableSetOf<Int>()

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("WhatsAppAccessibility", "onCreate called")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        android.util.Log.d("WhatsAppAccessibility", "onServiceConnected called")
        repository = TrackerRepository(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event == null) return
            val packageName = event.packageName?.toString() ?: return
            
            if (packageName != "com.whatsapp" && packageName != "com.whatsapp.w4b") {
                return
            }

            val rootNode = rootInActiveWindow ?: return

            trackWhatsAppCall(rootNode)

            // Extract the contact/group name from the toolbar
            val contactNameNode = findNodeById(rootNode, "com.whatsapp:id/conversation_contact_name")
            if (contactNameNode != null && contactNameNode.text != null) {
                currentChatName = contactNameNode.text.toString()
            }

            if (currentChatName != "Unknown") {
                extractMessagesFromScreen(rootNode)
                extractCallsFromChatScreen(rootNode)
            }
        } catch (e: Exception) {
            android.util.Log.e("WhatsAppAccessibility", "Error in onAccessibilityEvent", e)
        }
    }

    private fun trackWhatsAppCall(rootNode: AccessibilityNodeInfo) {
        try {
            // Log all visible texts for debugging
            val visibleTexts = mutableListOf<String>()
            val viewIds = mutableListOf<String>()
            extractAllTextsAndIds(rootNode, visibleTexts, viewIds)
            
            val joinedTexts = visibleTexts.joinToString(" | ")
            val hasGenericVoiceCallText = visibleTexts.any { it.equals("Voice call", ignoreCase = true) }
            val hasGenericVideoCallText = visibleTexts.any { it.equals("Video call", ignoreCase = true) }
            
            val isVoiceCall = visibleTexts.any { it.equals("WhatsApp Voice Call", ignoreCase = true) } || hasGenericVoiceCallText
            val isVideoCall = visibleTexts.any { it.equals("WhatsApp Video Call", ignoreCase = true) } || hasGenericVideoCallText
            
            val isRinging = visibleTexts.any { it.contains("Ringing", ignoreCase = true) }
            val isCalling = visibleTexts.any { it.contains("Calling", ignoreCase = true) }
            
            val hasEndCallBtnId = viewIds.any { it.contains("end_call_btn") || it.contains("reject_btn") }
            val hasEndCallText = visibleTexts.any { it.equals("End call", ignoreCase = true) }
            val hasEndCallBtn = hasEndCallBtnId || hasEndCallText
            
            // Heuristic to detect if we are in a chat bubble vs a real call screen.
            // Chat bubbles usually have "Type a message", "Message", or an attachment button.
            val isChatScreen = visibleTexts.any { it.equals("Message", ignoreCase = true) } || 
                               viewIds.any { it.contains("entry") || it.contains("conversation_contact_name") }
                               
            var currentDuration = ""
            var currentContact = "Unknown"
            var currentNumber = "Unknown"

            findCallDetailsRecursively(rootNode) { duration, contact, number ->
                if (duration != null) currentDuration = duration
                if (contact != null) currentContact = contact
                if (number != null) currentNumber = number
            }
            
            // We are on a call screen if we see strong indicators AND we are NOT on a chat screen.
            // If we are on a chat screen, the generic "Voice call" text from a bubble shouldn't trigger it.
            val onCallScreen = (!isChatScreen && (isVoiceCall || isVideoCall || currentDuration.isNotEmpty())) || 
                               isRinging || isCalling || hasEndCallBtn
                               
            if (isVoiceCall || isVideoCall || isRinging || isCalling || hasEndCallBtn || currentDuration.isNotEmpty()) {
                android.util.Log.d("WhatsAppAccessibility", "--- CALL DETECTED CHECK ---")
                android.util.Log.d("WhatsAppAccessibility", "Visible Texts: $joinedTexts")
                android.util.Log.d("WhatsAppAccessibility", "Visible View IDs: ${viewIds.joinToString(", ")}")
                android.util.Log.d("WhatsAppAccessibility", "Flags -> isVoiceCall:$isVoiceCall, isVideoCall:$isVideoCall, isRinging:$isRinging, isCalling:$isCalling, hasEndCallBtn:$hasEndCallBtn, isChatScreen:$isChatScreen")
                android.util.Log.d("WhatsAppAccessibility", "Details -> Duration:$currentDuration, Contact:$currentContact")
                android.util.Log.d("WhatsAppAccessibility", "Result -> onCallScreen: $onCallScreen")
            }

            if (onCallScreen) {
                if (!isCallActive) {
                    android.util.Log.d("WhatsAppAccessibility", "STATE TRANSITION: CALL STARTED")
                    isCallActive = true
                    callStartTime = System.currentTimeMillis()
                    callContactName = currentContact
                    callNumber = if (currentNumber != "Unknown") currentNumber else "+91 Unknown"
                    
                    if (isVoiceCall) callSessionType = "Voice Call"
                    if (isVideoCall) callSessionType = "Video Call"
                    
                    if (isCalling) callDirection = "Outgoing"
                    else if (isRinging) callDirection = "Incoming"
                    else callDirection = "Unknown"
                } else {
                    if (currentContact != "Unknown" && callContactName == "Unknown") callContactName = currentContact
                    if (currentNumber != "Unknown" && callNumber.contains("Unknown")) callNumber = currentNumber
                    if (isVoiceCall) callSessionType = "Voice Call"
                    if (isVideoCall) callSessionType = "Video Call"
                    
                    if (isCalling) {
                        callDirection = "Outgoing"
                    } else if (isRinging && callDirection != "Outgoing") {
                        callDirection = "Incoming"
                    }
                    
                    if (currentDuration.isNotEmpty()) callDuration = currentDuration
                }
            } else {
                if (isCallActive) {
                    android.util.Log.d("WhatsAppAccessibility", "STATE TRANSITION: CALL ENDED")
                    val finalDuration = callDuration
                    val finalDirection = if (callDirection == "Unknown") "Incoming" else callDirection
                    
                    val finalContact = if (finalDirection == "Outgoing") {
                        "Me" 
                    } else {
                        if (callContactName == "Unknown") "WhatsApp Caller" else callContactName
                    }
                    
                    val finalSessionType = if (callSessionType == "Unknown") "Voice Call" else callSessionType
                    val finalTimestamp = callStartTime
                    val finalNumber = callNumber
                    
                    android.util.Log.d("WhatsAppAccessibility", "Saving Call: $finalContact | $finalDirection | $finalSessionType | $finalDuration")
                    
                    if (finalDuration.isNotEmpty() && finalDuration != "00:00" && finalDuration != "0:00") {
                        serviceScope.launch {
                            repository.insertWhatsAppCall(
                                contactName = finalContact,
                                number = finalNumber,
                                direction = finalDirection,
                                sessionType = finalSessionType,
                                duration = finalDuration,
                                timestamp = finalTimestamp
                            )
                        }
                    } else {
                        android.util.Log.d("WhatsAppAccessibility", "Call Rejected: Duration was 00:00")
                    }
                    
                    isCallActive = false
                    callContactName = "Unknown"
                    callNumber = "Unknown"
                    callDirection = "Unknown"
                    callSessionType = "Unknown"
                    callDuration = "00:00"
                    callStartTime = 0L
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("WhatsAppAccessibility", "Error tracking call", e)
        }
    }
    
    private fun extractAllTextsAndIds(node: AccessibilityNodeInfo, texts: MutableList<String>, ids: MutableList<String>) {
        if (node.text != null && node.text.toString().isNotBlank()) {
            texts.add(node.text.toString())
        }
        if (node.viewIdResourceName != null) {
            ids.add(node.viewIdResourceName)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                extractAllTextsAndIds(child, texts, ids)
            }
        }
    }

    private fun findCallDetailsRecursively(node: AccessibilityNodeInfo, callback: (String?, String?, String?) -> Unit) {
        val text = node.text?.toString() ?: ""
        
        // Match MM:SS or HH:MM:SS
        if (text.matches(Regex("^\\d{1,2}:\\d{2}(:\\d{2})?\$"))) {
            callback(text, null, null)
        }
        
        // Try to identify phone numbers vs contact names
        if (node.viewIdResourceName?.endsWith("id/name") == true || node.viewIdResourceName?.endsWith("id/title") == true) {
            if (text.isNotEmpty() && !text.matches(Regex(".*\\d{5}.*"))) {
                callback(null, text, null)
            } else if (text.matches(Regex(".*\\d{5}.*"))) {
                callback(null, null, text)
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                findCallDetailsRecursively(child, callback)
            }
        }
    }

    private fun extractMessagesFromScreen(rootNode: AccessibilityNodeInfo) {
        try {
            val messageNodes = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/message_text")
            if (messageNodes.isNullOrEmpty()) return

            val isNewChat = !initializedChats.contains(currentChatName)

            var baseTimestamp = System.currentTimeMillis()

            for (node in messageNodes) {
                val text = node.text?.toString()
                if (text.isNullOrBlank()) continue
                
                // Add 1 millisecond for each message so newer messages (at the bottom) get a strictly larger timestamp
                val msgTimestamp = baseTimestamp++
                
                // Determine if the message is Outgoing by checking for the presence of the status icon (ticks)
                // which are only present in outgoing messages. We check siblings or children of the parent.
                var isOutgoing = false
                var checkParent: AccessibilityNodeInfo? = node.parent
                
                for (i in 0..3) {
                    if (checkParent == null) break
                    val statusNodes = checkParent.findAccessibilityNodeInfosByViewId("com.whatsapp:id/status")
                    if (!statusNodes.isNullOrEmpty()) {
                        isOutgoing = true
                        break
                    }
                    checkParent = checkParent.parent
                }
                
                val direction = if (isOutgoing) "Outgoing" else "Incoming"
                
                val hash = "$currentChatName|$text|$direction".hashCode()
                if (processedMessages.contains(hash)) {
                    continue
                }
                processedMessages.add(hash)
                
                if (isNewChat) {
                    continue // Skip saving old messages on first open
                }
                
                serviceScope.launch {
                    // The TrackerDao checkWhatsAppChatExistsLoose will prevent duplicated inserts 
                    // within a 1-hour window.
                    repository.insertWhatsAppChat(
                        contactName = currentChatName,
                        messageText = text,
                        timestamp = msgTimestamp,
                        direction = direction
                    )
                }
            }
            
            if (isNewChat) {
                initializedChats.add(currentChatName)
            }
        } catch (e: Exception) {
            android.util.Log.e("WhatsAppAccessibility", "Error in extractMessagesFromScreen", e)
        }
    }

    private fun findNodeById(root: AccessibilityNodeInfo, id: String): AccessibilityNodeInfo? {
        try {
            val list = root.findAccessibilityNodeInfosByViewId(id)
            if (!list.isNullOrEmpty()) {
                return list[0]
            }
        } catch (e: Exception) {
            android.util.Log.e("WhatsAppAccessibility", "Error in findNodeById", e)
        }
        return null
    }
    private fun extractCallsFromChatScreen(rootNode: AccessibilityNodeInfo) {
        try {
            val voiceNodes = rootNode.findAccessibilityNodeInfosByText("Voice call") ?: emptyList()
            val videoNodes = rootNode.findAccessibilityNodeInfosByText("Video call") ?: emptyList()
            
            val allCallNodes = voiceNodes + videoNodes
            if (allCallNodes.isEmpty()) return

            val rootRect = android.graphics.Rect()
            rootNode.getBoundsInScreen(rootRect)
            val screenCenter = rootRect.width() / 2

            val isNewChat = !initializedChats.contains(currentChatName)

            var baseTimestamp = System.currentTimeMillis()

            for (node in allCallNodes) {
                val text = node.text?.toString() ?: continue
                
                // We only want exact matches to avoid partial text overlap
                if (text != "Voice call" && text != "Video call") continue
                
                val sessionType = if (text == "Voice call") "Voice Call" else "Video Call"
                
                // Use screen positioning to determine if the bubble is Incoming (Left) or Outgoing (Right)
                val rect = android.graphics.Rect()
                node.getBoundsInScreen(rect)
                val isOutgoing = rect.left > screenCenter
                val direction = if (isOutgoing) "Outgoing" else "Incoming"
                val contactName = if (isOutgoing) "Me" else currentChatName
                
                var durationStr = "00:00"
                var bubbleParent = node.parent
                // Go up a few levels to capture the sibling text nodes within the same chat bubble
                for (i in 0..3) {
                    if (bubbleParent == null) break
                    val extracted = findDurationInBubble(bubbleParent)
                    if (extracted != null) {
                        durationStr = extracted
                        break
                    }
                    bubbleParent = bubbleParent.parent
                }
                
                val finalDuration = parseDuration(durationStr)
                if (finalDuration == "00:00") continue
                
                val hash = "$contactName|$direction|$sessionType|$finalDuration".hashCode()
                if (processedCalls.contains(hash)) {
                    continue
                }
                processedCalls.add(hash)

                if (isNewChat) {
                    continue // Skip saving old call bubbles on first open
                }

                val msgTimestamp = baseTimestamp++
                
                android.util.Log.d("WhatsAppAccessibility", "Chat Bubble Call Detected: $direction | $sessionType | $finalDuration | Outgoing=$isOutgoing")
                
                serviceScope.launch {
                    android.util.Log.d("WhatsAppAccessibility", "Saving Call from Bubble: $contactName | $direction | $sessionType | $finalDuration")
                    repository.insertWhatsAppCall(
                        contactName = contactName,
                        number = "+91 Unknown",
                        direction = direction,
                        sessionType = sessionType,
                        duration = finalDuration,
                        timestamp = msgTimestamp
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("WhatsAppAccessibility", "Error extracting calls from bubbles", e)
        }
    }

    private fun findDurationInBubble(node: AccessibilityNodeInfo): String? {
        val text = node.text?.toString() ?: ""
        if (text.contains("sec", ignoreCase = true) || text.contains("min", ignoreCase = true)) {
            return text
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val res = findDurationInBubble(child)
                if (res != null) return res
            }
        }
        return null
    }

    private fun parseDuration(raw: String): String {
        try {
            var totalSeconds = 0
            val minMatch = Regex("(\\d+)\\s*min").find(raw)
            if (minMatch != null) totalSeconds += minMatch.groupValues[1].toInt() * 60
            
            val secMatch = Regex("(\\d+)\\s*sec").find(raw)
            if (secMatch != null) totalSeconds += secMatch.groupValues[1].toInt()
            
            val mins = totalSeconds / 60
            val secs = totalSeconds % 60
            return String.format("%02d:%02d", mins, secs)
        } catch (e: Exception) {
            return "00:00"
        }
    }

    override fun onInterrupt() {
        android.util.Log.d("WhatsAppAccessibility", "onInterrupt called")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("WhatsAppAccessibility", "onDestroy called")
        serviceJob.cancel()
    }
}
