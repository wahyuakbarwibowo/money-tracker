package com.aminmart.moneymanager.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Registers the app's periodic background jobs (idempotent via KEEP). */
object WorkScheduler {

    fun schedule(context: Context) {
        val wm = WorkManager.getInstance(context)

        val dailyCheck = PeriodicWorkRequestBuilder<DailyCheckWorker>(1, TimeUnit.DAYS).build()
        wm.enqueueUniquePeriodicWork(
            DailyCheckWorker.NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyCheck
        )

        val backup = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS).build()
        wm.enqueueUniquePeriodicWork(
            BackupWorker.NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            backup
        )
    }
}
