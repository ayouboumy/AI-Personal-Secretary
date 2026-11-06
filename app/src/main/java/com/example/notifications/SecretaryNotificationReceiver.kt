package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SecretaryNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("taskId") ?: return
        val taskTitle = intent.getStringExtra("taskTitle") ?: "Important Task"
        val priority = intent.getStringExtra("priority") ?: "HIGH"

        SecretaryNotificationManager.showNotification(context, taskId, taskTitle, priority)
    }
}
