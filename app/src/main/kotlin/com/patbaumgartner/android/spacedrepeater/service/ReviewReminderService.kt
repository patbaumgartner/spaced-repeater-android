package com.patbaumgartner.android.spacedrepeater.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.patbaumgartner.android.spacedrepeater.R
import com.patbaumgartner.android.spacedrepeater.data.local.card.CardStateDao
import com.patbaumgartner.android.spacedrepeater.ui.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

class ReviewReminderService(
    private val context: Context,
    private val workManager: WorkManager,
) {
    /**
     * Schedules (or reschedules) the review-reminder worker to fire at [dueAtMillis].
     * The worker itself checks the current due count and notification permission at execution time,
     * so a non-positive due count here does not prevent future reminders from being delivered.
     */
    fun scheduleNext(dueAtMillis: Long) {
        val delay = (dueAtMillis - System.currentTimeMillis()).coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<ReviewReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(
            "review-reminder",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    /** Cancels any pending reminder; used when the learner disables reminders. */
    fun cancel() {
        workManager.cancelUniqueWork("review-reminder")
    }
}

@HiltWorker
class ReviewReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val cardStateDao: CardStateDao,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val dueCount = cardStateDao.dueStates(now).size
        if (dueCount > 0 && hasPermission()) {
            postNotification(dueCount)
        }
        return Result.success()
    }

    @Suppress("InlinedApi")
    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun postNotification(dueCount: Int) {
        val channelId = "review_reminders"
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                channelId,
                applicationContext.getString(R.string.channel_reminders),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(applicationContext.resources.getQuantityString(R.plurals.reminder_title, dueCount, dueCount))
            .setContentText(applicationContext.getString(R.string.reminder_text))
            .setColor(applicationContext.getColor(R.color.notification_accent))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        nm.notify(1, notification)
    }
}
