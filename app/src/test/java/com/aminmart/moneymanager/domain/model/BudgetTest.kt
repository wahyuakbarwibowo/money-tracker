package com.aminmart.moneymanager.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetTest {

    private fun budget(monthly: Double, spent: Double) =
        Budget(category = "Food", monthlyBudget = monthly, month = "2026-06", spent = spent)

    @Test
    fun remaining_isBudgetMinusSpent() {
        assertEquals(400.0, budget(1000.0, 600.0).remaining, 0.0001)
    }

    @Test
    fun remaining_canGoNegative_whenOverspent() {
        assertEquals(-50.0, budget(100.0, 150.0).remaining, 0.0001)
    }

    @Test
    fun percentageUsed_isSpentOverBudget() {
        assertEquals(0.6f, budget(1000.0, 600.0).percentageUsed, 0.0001f)
    }

    @Test
    fun percentageUsed_isZero_whenBudgetIsZero() {
        assertEquals(0f, budget(0.0, 500.0).percentageUsed, 0.0001f)
    }

    @Test
    fun isOverBudget_trueWhenSpentExceedsBudget() {
        assertTrue(budget(100.0, 150.0).isOverBudget)
        assertFalse(budget(100.0, 100.0).isOverBudget)
    }

    @Test
    fun isNearLimit_trueBetween80And100Percent() {
        assertTrue(budget(1000.0, 850.0).isNearLimit)
    }

    @Test
    fun isNearLimit_falseAtOrAboveBudget() {
        assertFalse(budget(1000.0, 1000.0).isNearLimit)
        assertFalse(budget(1000.0, 1200.0).isNearLimit)
    }

    @Test
    fun isNearLimit_falseBelow80Percent() {
        assertFalse(budget(1000.0, 700.0).isNearLimit)
    }
}
