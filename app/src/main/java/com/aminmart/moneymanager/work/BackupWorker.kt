package com.aminmart.moneymanager.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aminmart.moneymanager.MoneyManagerApplication
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.presentation.notifications.NotificationHelper

/**
 * Periodic JSON auto-backup. Replaces the unreliable "backup on app close"
 * approach with a WorkManager job that survives process death.
 */
class BackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? MoneyManagerApplication ?: return Result.success()

        return try {
            val path = app.autoBackupUseCase()
            if (path != null) {
                NotificationHelper.notify(
                    applicationContext,
                    NotificationHelper.CHANNEL_BACKUP,
                    NOTIF_BACKUP,
                    applicationContext.getString(R.string.notif_backup_title),
                    applicationContext.getString(R.string.notif_backup_body)
                )
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val NAME = "periodic_backup"
        private const val NOTIF_BACKUP = 1003
    }
}
