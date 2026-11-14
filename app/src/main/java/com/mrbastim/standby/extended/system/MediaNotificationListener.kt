package com.mrbastim.standby.extended.system

import android.service.notification.NotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Android requires an enabled notification listener to discover other apps' media sessions.
 * Notification contents are neither read nor stored.
 */
class MediaNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        connectionChanges.update { it + 1 }
    }

    override fun onListenerDisconnected() {
        connectionChanges.update { it + 1 }
        super.onListenerDisconnected()
    }

    companion object {
        private val connectionChanges = MutableStateFlow(0)
        val changes = connectionChanges.asStateFlow()
    }
}
