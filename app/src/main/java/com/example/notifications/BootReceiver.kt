package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.SecretaryDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = SecretaryDatabase.getDatabase(context)
                    val tasks = db.taskDao().getAllTasks().firstOrNull() ?: emptyList()
                    val now = System.currentTimeMillis()
                    for (task in tasks) {
                        if (task.status != "COMPLETED" && (task.deadlineMillis ?: 0L) > now) {
                            SecretaryNotificationManager.scheduleTaskReminder(context, task)
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
