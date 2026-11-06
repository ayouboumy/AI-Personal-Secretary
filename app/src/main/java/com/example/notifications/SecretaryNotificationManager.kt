package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.SecretaryDatabase
import com.example.data.local.entity.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val DEADLINES_CHANNEL_ID = "channel_deadlines"
const val BRIEFING_CHANNEL_ID = "channel_briefing"

object SecretaryNotificationManager {

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val deadlinesChannel = NotificationChannel(
                DEADLINES_CHANNEL_ID,
                context.getString(R.string.channel_deadlines_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_deadlines_desc)
                enableVibration(true)
            }

            val briefingChannel = NotificationChannel(
                BRIEFING_CHANNEL_ID,
                context.getString(R.string.channel_briefing_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_briefing_desc)
            }

            notificationManager.createNotificationChannel(deadlinesChannel)
            notificationManager.createNotificationChannel(briefingChannel)
        }
    }

    fun scheduleTaskReminder(context: Context, task: TaskEntity) {
        val deadlineMillis = task.deadlineMillis ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Calculate reminder time (e.g. 1 day or reminderDaysBefore before deadline)
        val advanceMillis = task.reminderDaysBefore * 24 * 60 * 60 * 1000L
        val triggerTime = (deadlineMillis - advanceMillis).coerceAtLeast(System.currentTimeMillis() + 5000L)

        val intent = Intent(context, SecretaryNotificationReceiver::class.java).apply {
            putExtra("taskId", task.id)
            putExtra("taskTitle", task.title)
            putExtra("deadlineType", task.deadlineType)
            putExtra("priority", task.priority)
        }

        val requestCode = task.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun cancelTaskReminder(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, SecretaryNotificationReceiver::class.java)
        val requestCode = taskId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    fun showNotification(context: Context, taskId: String, taskTitle: String, priority: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        CoroutineScope(Dispatchers.IO).launch {
            val db = SecretaryDatabase.getDatabase(context)
            val profile = db.userDao().getUserProfileSync()
            val protectPreviews = profile?.protectNotificationPreviews ?: true

            val contentTitle = if (protectPreviews) "AI Secretary Alert" else "Deadline Reminder: $taskTitle"
            val contentText = if (protectPreviews) {
                "You have an upcoming work deadline scheduled today. Tap to view."
            } else {
                "Priority: $priority - Required deadline is approaching."
            }

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                taskId.hashCode(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, DEADLINES_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(taskId.hashCode(), notification)
        }
    }
}
