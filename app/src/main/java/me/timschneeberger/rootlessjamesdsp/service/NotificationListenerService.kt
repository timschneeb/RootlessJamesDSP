package me.timschneeberger.rootlessjamesdsp.service

import android.service.notification.NotificationListenerService
import android.content.Intent
import android.os.IBinder
import android.service.notification.StatusBarNotification

class NotificationListenerService : NotificationListenerService() {
    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {}
    override fun onNotificationRemoved(sbn: StatusBarNotification) {}
}