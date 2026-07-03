package com.aminmart.moneymanager.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aminmart.moneymanager.MoneyManagerApplication
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.presentation.notifications.NotificationHelper
import com.aminmart.moneymanager.presentation.ui.CurrencyFormatter
import kotlinx.coroutines.flow.first

/**
 * Periodic check that posts reminder notifications:
 *  - debts that are unpaid and due within [DUE_SOON_DAYS] days (or overdue),
 *  - budgets at/over their near-limit threshold for the current month.
 */
class DailyCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? MoneyManagerApplication ?: return Result.success()

        app.recurringUseCases.generateDue()
        checkDebts(app)
        checkBudgets(app)
        return Result.success()
    }

    private suspend fun checkDebts(app: MoneyManagerApplication) {
        val now = System.currentTimeMillis()
        val threshold = now + DUE_SOON_DAYS * DAY_MS

        val dueSoon = app.debtUseCases.getAllDebts().first()
            .filter { !it.isPaid && it.dueDate <= threshold }

        if (dueSoon.isEmpty()) return

        val total = dueSoon.sumOf { it.amount }
        val title = applicationContext.getString(R.string.notif_debt_due_title)
        val message = applicationContext.getString(
            R.string.notif_debt_due_body,
            dueSoon.size,
            CurrencyFormatter.format(total)
        )
        NotificationHelper.notify(
            applicationContext, NotificationHelper.CHANNEL_REMINDERS, NOTIF_DEBT, title, message
        )
    }

    private suspend fun checkBudgets(app: MoneyManagerApplication) {
        val flagged = app.getCurrentMonthBudgetsUseCase().first()
            .filter { it.isNearLimit || it.isOverBudget }

        if (flagged.isEmpty()) return

        val title = applicationContext.getString(R.string.notif_budget_alert_title)
        val message = flagged.joinToString("\n") { b ->
            val pct = (b.percentageUsed * 100).toInt()
            applicationContext.getString(R.string.notif_budget_alert_line, b.category, pct)
        }
        NotificationHelper.notify(
            applicationContext, NotificationHelper.CHANNEL_REMINDERS, NOTIF_BUDGET, title, message
        )
    }

    companion object {
        const val NAME = "daily_check"
        private const val DUE_SOON_DAYS = 3
        private const val DAY_MS = 24L * 60 * 60 * 1000
        private const val NOTIF_DEBT = 1001
        private const val NOTIF_BUDGET = 1002
    }
}
