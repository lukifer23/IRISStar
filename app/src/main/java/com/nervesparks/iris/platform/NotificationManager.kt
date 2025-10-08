package com.nervesparks.iris.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nervesparks.iris.MainActivity
import com.nervesparks.iris.R
import timber.log.Timber

/**
 * Notification manager for handling app notifications and background task feedback
 */
class NotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID_BACKGROUND_TASKS = "background_tasks"
        const val CHANNEL_ID_MODEL_LOADING = "model_loading"
        const val CHANNEL_ID_EXPORTS = "exports"

        const val NOTIFICATION_ID_MODEL_LOADING = 1001
        const val NOTIFICATION_ID_EXPORT_PROGRESS = 1002
        const val NOTIFICATION_ID_BACKGROUND_TASK = 1003
    }

    init {
        createNotificationChannels()
    }

    /**
     * Create notification channels for different types of notifications
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_BACKGROUND_TASKS,
                    "Background Tasks",
                    android.app.NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Notifications for background operations"
                },
                NotificationChannel(
                    CHANNEL_ID_MODEL_LOADING,
                    "Model Loading",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for model loading progress"
                },
                NotificationChannel(
                    CHANNEL_ID_EXPORTS,
                    "Exports",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for export operations"
                }
            )

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    /**
     * Show a notification for model loading progress
     */
    fun showModelLoadingNotification(modelName: String, progress: Int) {
        if (!AndroidPlatformFeatures.hasNotificationPermission(context)) {
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_MODEL_LOADING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Loading Model")
            .setContentText("Loading $modelName... $progress%")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_MODEL_LOADING, notification)
    }

    /**
     * Hide the model loading notification
     */
    fun hideModelLoadingNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_MODEL_LOADING)
    }

    /**
     * Show export progress notification
     */
    fun showExportProgressNotification(fileName: String, progress: Int) {
        if (!AndroidPlatformFeatures.hasNotificationPermission(context)) {
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_EXPORTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Exporting Chat")
            .setContentText("Exporting $fileName... $progress%")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_EXPORT_PROGRESS, notification)
    }

    /**
     * Show export completion notification
     */
    fun showExportCompleteNotification(fileName: String, filePath: String) {
        if (!AndroidPlatformFeatures.hasNotificationPermission(context)) {
            return
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse(filePath)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_EXPORTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Export Complete")
            .setContentText("$fileName exported successfully")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_EXPORT_PROGRESS, notification)
    }

    /**
     * Show background task notification
     */
    fun showBackgroundTaskNotification(title: String, message: String, persistent: Boolean = false) {
        if (!AndroidPlatformFeatures.hasNotificationPermission(context)) {
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BACKGROUND_TASKS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setOngoing(persistent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BACKGROUND_TASK, notification)
    }

    /**
     * Cancel a specific notification
     */
    fun cancelNotification(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
