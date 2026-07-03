package com.aminmart.moneymanager.data.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.aminmart.moneymanager.domain.model.Account
import com.aminmart.moneymanager.domain.model.Budget
import com.aminmart.moneymanager.domain.model.Debt
import com.aminmart.moneymanager.domain.model.ImportHistory
import com.aminmart.moneymanager.domain.model.RecurringRule
import com.aminmart.moneymanager.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * SQLite Database helper for Money Manager
 */
class MoneyDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "moneymanager.db"
        private const val DATABASE_VERSION = 5

        // Common columns
        private const val COL_ID = "id"
        private const val COL_TYPE = "type"
        private const val COL_AMOUNT = "amount"
        private const val COL_DESCRIPTION = "description"
        private const val COL_CREATED_AT = "created_at"

        // Transaction table
        private const val TABLE_TRANSACTIONS = "transactions"
        private const val COL_CATEGORY = "category"
        private const val COL_DATE = "date"
        private const val COL_IS_RIBA = "is_riba"

        // Budget table
        private const val TABLE_BUDGETS = "budgets"
        private const val COL_MONTHLY_BUDGET = "monthly_budget"
        private const val COL_MONTH = "month"
        private const val COL_SPENT = "spent"

        // Import history table
        private const val TABLE_IMPORT_HISTORY = "import_history"
        private const val COL_FILE_NAME = "file_name"
        private const val COL_IMPORT_DATE = "import_date"
        private const val COL_TRANSACTION_COUNT = "transaction_count"
        private const val COL_STATUS = "status"

        // Debt table
        private const val TABLE_DEBTS = "debts"
        private const val COL_PERSON_NAME = "person_name"
        private const val COL_DUE_DATE = "due_date"
        private const val COL_IS_PAID = "is_paid"
        private const val COL_UPDATED_AT = "updated_at"

        // Recurring rules table
        private const val TABLE_RECURRING = "recurring_rules"
        private const val COL_FREQUENCY = "frequency"
        private const val COL_INTERVAL = "interval_count"
        private const val COL_NEXT_RUN = "next_run"
        private const val COL_ACTIVE = "active"

        // Accounts table
        private const val TABLE_ACCOUNTS = "accounts"
        private const val COL_ACCOUNT_NAME = "name"
        private const val COL_INITIAL_BALANCE = "initial_balance"
        private const val COL_ACCOUNT_ID = "account_id"
        private const val DEFAULT_ACCOUNT_NAME = "Cash"
    }

    override fun onCreate(db: SQLiteDatabase) {
        createTables(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(CREATE_DEBTS_TABLE_SQL)
            db.execSQL("CREATE INDEX idx_debts_type ON $TABLE_DEBTS($COL_TYPE)")
            db.execSQL("CREATE INDEX idx_debts_is_paid ON $TABLE_DEBTS($COL_IS_PAID)")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TABLE_TRANSACTIONS ADD COLUMN $COL_IS_RIBA INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 4) {
            db.execSQL(CREATE_RECURRING_TABLE_SQL)
            db.execSQL("CREATE INDEX idx_recurring_next_run ON $TABLE_RECURRING($COL_NEXT_RUN)")
        }
        if (oldVersion < 5) {
            db.execSQL(CREATE_ACCOUNTS_TABLE_SQL)
            seedDefaultAccount(db)
            db.execSQL("ALTER TABLE $TABLE_TRANSACTIONS ADD COLUMN $COL_ACCOUNT_ID INTEGER NOT NULL DEFAULT 1")
        }
    }

    private fun createTables(db: SQLiteDatabase) {
        // Create transactions table
        db.execSQL("""
            CREATE TABLE $TABLE_TRANSACTIONS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TYPE TEXT NOT NULL,
                $COL_AMOUNT REAL NOT NULL,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_DESCRIPTION TEXT,
                $COL_DATE INTEGER NOT NULL,
                $COL_CREATED_AT INTEGER NOT NULL,
                $COL_IS_RIBA INTEGER NOT NULL DEFAULT 0,
                $COL_ACCOUNT_ID INTEGER NOT NULL DEFAULT 1
            )
        """)

        // Create budgets table
        db.execSQL("""
            CREATE TABLE $TABLE_BUDGETS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_MONTHLY_BUDGET REAL NOT NULL,
                $COL_MONTH TEXT NOT NULL,
                $COL_SPENT REAL DEFAULT 0,
                $COL_CREATED_AT INTEGER NOT NULL,
                UNIQUE($COL_CATEGORY, $COL_MONTH)
            )
        """)

        // Create import history table
        db.execSQL("""
            CREATE TABLE $TABLE_IMPORT_HISTORY (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_FILE_NAME TEXT NOT NULL,
                $COL_IMPORT_DATE INTEGER NOT NULL,
                $COL_TRANSACTION_COUNT INTEGER DEFAULT 0,
                $COL_STATUS TEXT DEFAULT 'SUCCESS'
            )
        """)

        // Create debts table
        db.execSQL(CREATE_DEBTS_TABLE_SQL)

        // Create recurring rules table
        db.execSQL(CREATE_RECURRING_TABLE_SQL)

        // Create accounts table + seed the default account
        db.execSQL(CREATE_ACCOUNTS_TABLE_SQL)
        seedDefaultAccount(db)

        // Create indexes for better performance
        db.execSQL("CREATE INDEX idx_transactions_type ON $TABLE_TRANSACTIONS($COL_TYPE)")
        db.execSQL("CREATE INDEX idx_transactions_date ON $TABLE_TRANSACTIONS($COL_DATE)")
        db.execSQL("CREATE INDEX idx_transactions_category ON $TABLE_TRANSACTIONS($COL_CATEGORY)")
        db.execSQL("CREATE INDEX idx_budgets_month ON $TABLE_BUDGETS($COL_MONTH)")
        db.execSQL("CREATE INDEX idx_debts_type ON $TABLE_DEBTS($COL_TYPE)")
        db.execSQL("CREATE INDEX idx_debts_is_paid ON $TABLE_DEBTS($COL_IS_PAID)")
        db.execSQL("CREATE INDEX idx_recurring_next_run ON $TABLE_RECURRING($COL_NEXT_RUN)")
    }

    private val CREATE_ACCOUNTS_TABLE_SQL = """
        CREATE TABLE $TABLE_ACCOUNTS (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_ACCOUNT_NAME TEXT NOT NULL,
            $COL_INITIAL_BALANCE REAL NOT NULL DEFAULT 0,
            $COL_CREATED_AT INTEGER NOT NULL
        )
    """

    private fun seedDefaultAccount(db: SQLiteDatabase) {
        val values = ContentValues().apply {
            put(COL_ID, Transaction.DEFAULT_ACCOUNT_ID)
            put(COL_ACCOUNT_NAME, DEFAULT_ACCOUNT_NAME)
            put(COL_INITIAL_BALANCE, 0.0)
            put(COL_CREATED_AT, System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_ACCOUNTS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    private val CREATE_RECURRING_TABLE_SQL = """
        CREATE TABLE $TABLE_RECURRING (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_TYPE TEXT NOT NULL,
            $COL_AMOUNT REAL NOT NULL,
            $COL_CATEGORY TEXT NOT NULL,
            $COL_DESCRIPTION TEXT,
            $COL_IS_RIBA INTEGER NOT NULL DEFAULT 0,
            $COL_FREQUENCY TEXT NOT NULL,
            $COL_INTERVAL INTEGER NOT NULL DEFAULT 1,
            $COL_NEXT_RUN INTEGER NOT NULL,
            $COL_ACTIVE INTEGER NOT NULL DEFAULT 1,
            $COL_CREATED_AT INTEGER NOT NULL
        )
    """

    private val CREATE_DEBTS_TABLE_SQL = """
        CREATE TABLE $TABLE_DEBTS (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_PERSON_NAME TEXT NOT NULL,
            $COL_AMOUNT REAL NOT NULL,
            $COL_TYPE TEXT NOT NULL,
            $COL_DUE_DATE INTEGER NOT NULL,
            $COL_DESCRIPTION TEXT,
            $COL_IS_PAID INTEGER NOT NULL DEFAULT 0,
            $COL_CREATED_AT INTEGER NOT NULL,
            $COL_UPDATED_AT INTEGER NOT NULL
        )
    """

    // ==================== Transaction Operations ====================

    fun getAllTransactions(): Flow<List<Transaction>> = flow {
        emit(queryAllTransactions())
    }

    fun getTransactionsByType(type: Transaction.TransactionType): Flow<List<Transaction>> = flow {
        emit(queryTransactionsByType(type))
    }

    fun getTransactionsByMonth(year: Int, month: Int): Flow<List<Transaction>> = flow {
        emit(queryTransactionsByMonth(year, month))
    }

    fun getTransactionsByCategory(category: String): Flow<List<Transaction>> = flow {
        emit(queryTransactionsByCategory(category))
    }

    fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = flow {
        emit(queryRecentTransactions(limit))
    }

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> = flow {
        emit(queryTransactionsByDateRange(startDate, endDate))
    }

    suspend fun getTransactionById(id: Long): Transaction? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            "$COL_ID = ?",
            arrayOf(id.toString()),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                cursorToTransaction(it)
            } else {
                null
            }
        }
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        val db = writableDatabase
        val values = transactionToContentValues(transaction)
        return db.insert(TABLE_TRANSACTIONS, null, values)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        val db = writableDatabase
        val values = transactionToContentValues(transaction)
        db.update(
            TABLE_TRANSACTIONS,
            values,
            "$COL_ID = ?",
            arrayOf(transaction.id.toString())
        )
    }

    suspend fun deleteTransaction(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_TRANSACTIONS, "$COL_ID = ?", arrayOf(id.toString()))
    }

    suspend fun deleteAllTransactions() {
        val db = writableDatabase
        db.delete(TABLE_TRANSACTIONS, null, null)
    }

    suspend fun getTotalIncome(startDate: Long, endDate: Long): Double {
        val db = readableDatabase
        val cursor = db.rawQuery("""
            SELECT SUM($COL_AMOUNT) FROM $TABLE_TRANSACTIONS
            WHERE $COL_TYPE = ? AND $COL_DATE BETWEEN ? AND ?
        """, arrayOf(Transaction.TransactionType.INCOME.name, startDate.toString(), endDate.toString()))
        return cursor.use {
            if (it.moveToFirst()) it.getDouble(0) else 0.0
        }
    }

    suspend fun getTotalExpense(startDate: Long, endDate: Long): Double {
        val db = readableDatabase
        val cursor = db.rawQuery("""
            SELECT SUM($COL_AMOUNT) FROM $TABLE_TRANSACTIONS
            WHERE $COL_TYPE = ? AND $COL_DATE BETWEEN ? AND ?
        """, arrayOf(Transaction.TransactionType.EXPENSE.name, startDate.toString(), endDate.toString()))
        return cursor.use {
            if (it.moveToFirst()) it.getDouble(0) else 0.0
        }
    }

    suspend fun getExpenseByCategory(startDate: Long, endDate: Long): Map<String, Double> {
        val db = readableDatabase
        val cursor = db.rawQuery("""
            SELECT $COL_CATEGORY, SUM($COL_AMOUNT) FROM $TABLE_TRANSACTIONS
            WHERE $COL_TYPE = ? AND $COL_DATE BETWEEN ? AND ?
            GROUP BY $COL_CATEGORY
        """, arrayOf(Transaction.TransactionType.EXPENSE.name, startDate.toString(), endDate.toString()))

        val result = mutableMapOf<String, Double>()
        cursor.use {
            while (it.moveToNext()) {
                result[it.getString(0)] = it.getDouble(1)
            }
        }
        return result
    }

    suspend fun getMonthlyExpenses(months: Int): Map<String, Double> {
        val db = readableDatabase
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.MONTH, -months + 1)
        val startDate = calendar.timeInMillis

        val cursor = db.rawQuery("""
            SELECT strftime('%Y-%m', $COL_DATE / 1000, 'unixepoch') as month, SUM($COL_AMOUNT)
            FROM $TABLE_TRANSACTIONS
            WHERE $COL_TYPE = ? AND $COL_DATE >= ?
            GROUP BY month
            ORDER BY month
        """, arrayOf(Transaction.TransactionType.EXPENSE.name, startDate.toString()))

        val result = mutableMapOf<String, Double>()
        cursor.use {
            while (it.moveToNext()) {
                result[it.getString(0)] = it.getDouble(1)
            }
        }
        return result
    }

    suspend fun transactionExists(date: Long, amount: Double, description: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("""
            SELECT COUNT(*) FROM $TABLE_TRANSACTIONS
            WHERE $COL_DATE = ? AND $COL_AMOUNT = ? AND $COL_DESCRIPTION = ?
        """, arrayOf(date.toString(), amount.toString(), description))
        return cursor.use {
            it.moveToFirst() && it.getInt(0) > 0
        }
    }

    suspend fun insertTransactions(transactions: List<Transaction>): List<Long> {
        val db = writableDatabase
        val ids = mutableListOf<Long>()
        db.beginTransaction()
        try {
            transactions.forEach { transaction ->
                val id = db.insert(TABLE_TRANSACTIONS, null, transactionToContentValues(transaction))
                ids.add(id)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return ids
    }

    // ==================== Budget Operations ====================

    fun getAllBudgets(): Flow<List<Budget>> = flow {
        emit(queryAllBudgets())
    }

    fun getBudgetsByMonth(month: String): Flow<List<Budget>> = flow {
        emit(queryBudgetsByMonth(month))
    }

    suspend fun getBudgetByCategory(category: String, month: String): Budget? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_BUDGETS,
            null,
            "$COL_CATEGORY = ? AND $COL_MONTH = ?",
            arrayOf(category, month),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                cursorToBudget(it)
            } else {
                null
            }
        }
    }

    suspend fun getBudgetById(id: Long): Budget? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_BUDGETS,
            null,
            "$COL_ID = ?",
            arrayOf(id.toString()),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                cursorToBudget(it)
            } else {
                null
            }
        }
    }

    suspend fun insertBudget(budget: Budget): Long {
        val db = writableDatabase
        val values = budgetToContentValues(budget)
        return db.insertWithOnConflict(TABLE_BUDGETS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun updateBudget(budget: Budget) {
        val db = writableDatabase
        val values = budgetToContentValues(budget)
        db.update(
            TABLE_BUDGETS,
            values,
            "$COL_ID = ?",
            arrayOf(budget.id.toString())
        )
    }

    suspend fun deleteBudget(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_BUDGETS, "$COL_ID = ?", arrayOf(id.toString()))
    }

    suspend fun updateBudgetSpent(category: String, month: String, spent: Double) {
        val db = writableDatabase
        val values = ContentValues()
        values.put(COL_SPENT, spent)
        db.update(
            TABLE_BUDGETS,
            values,
            "$COL_CATEGORY = ? AND $COL_MONTH = ?",
            arrayOf(category, month)
        )
    }

    suspend fun getTotalBudgetForMonth(month: String): Double {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT SUM($COL_MONTHLY_BUDGET) FROM $TABLE_BUDGETS WHERE $COL_MONTH = ?",
            arrayOf(month)
        )
        return cursor.use {
            if (it.moveToFirst()) it.getDouble(0) else 0.0
        }
    }

    suspend fun getTotalSpentForMonth(month: String): Double {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT SUM($COL_SPENT) FROM $TABLE_BUDGETS WHERE $COL_MONTH = ?",
            arrayOf(month)
        )
        return cursor.use {
            if (it.moveToFirst()) it.getDouble(0) else 0.0
        }
    }

    // ==================== Import History Operations ====================

    fun getAllImportHistory(): Flow<List<ImportHistory>> = flow {
        emit(queryAllImportHistory())
    }

    suspend fun getImportHistoryById(id: Long): ImportHistory? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_IMPORT_HISTORY,
            null,
            "$COL_ID = ?",
            arrayOf(id.toString()),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                cursorToImportHistory(it)
            } else {
                null
            }
        }
    }

    suspend fun insertImportHistory(history: ImportHistory): Long {
        val db = writableDatabase
        val values = importHistoryToContentValues(history)
        return db.insert(TABLE_IMPORT_HISTORY, null, values)
    }

    suspend fun isFileImported(fileName: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_IMPORT_HISTORY,
            arrayOf(COL_ID),
            "$COL_FILE_NAME = ?",
            arrayOf(fileName),
            null, null, null
        )
        return cursor.use { it.count > 0 }
    }

    suspend fun deleteImportHistory(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_IMPORT_HISTORY, "$COL_ID = ?", arrayOf(id.toString()))
    }

    // ==================== Debt Operations ====================

    fun getAllDebts(): Flow<List<Debt>> = flow {
        emit(queryAllDebts())
    }

    suspend fun getDebtById(id: Long): Debt? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_DEBTS,
            null,
            "$COL_ID = ?",
            arrayOf(id.toString()),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                cursorToDebt(it)
            } else {
                null
            }
        }
    }

    suspend fun insertDebt(debt: Debt): Long {
        val db = writableDatabase
        val values = debtToContentValues(debt)
        return db.insert(TABLE_DEBTS, null, values)
    }

    suspend fun updateDebt(debt: Debt) {
        val db = writableDatabase
        val values = debtToContentValues(debt)
        db.update(
            TABLE_DEBTS,
            values,
            "$COL_ID = ?",
            arrayOf(debt.id.toString())
        )
    }

    suspend fun deleteDebt(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_DEBTS, "$COL_ID = ?", arrayOf(id.toString()))
    }

    // ==================== Account Operations ====================

    fun getAllAccounts(): Flow<List<Account>> = flow {
        emit(queryAccounts())
    }

    private fun queryAccounts(): List<Account> {
        val db = readableDatabase
        val cursor = db.query(TABLE_ACCOUNTS, null, null, null, null, null, "$COL_ID ASC")
        val accounts = mutableListOf<Account>()
        cursor.use {
            while (it.moveToNext()) {
                val account = cursorToAccount(it)
                accounts.add(account.copy(currentBalance = computeBalance(db, account)))
            }
        }
        return accounts
    }

    suspend fun getAccountById(id: Long): Account? {
        val db = readableDatabase
        val cursor = db.query(TABLE_ACCOUNTS, null, "$COL_ID = ?", arrayOf(id.toString()), null, null, null)
        return cursor.use {
            if (it.moveToFirst()) {
                val account = cursorToAccount(it)
                account.copy(currentBalance = computeBalance(db, account))
            } else null
        }
    }

    suspend fun insertAccount(account: Account): Long {
        val db = writableDatabase
        return db.insert(TABLE_ACCOUNTS, null, accountToContentValues(account))
    }

    suspend fun updateAccount(account: Account) {
        val db = writableDatabase
        db.update(TABLE_ACCOUNTS, accountToContentValues(account), "$COL_ID = ?", arrayOf(account.id.toString()))
    }

    suspend fun deleteAccount(id: Long) {
        if (id == Transaction.DEFAULT_ACCOUNT_ID) return // never delete the default account
        val db = writableDatabase
        // Reassign this account's transactions back to the default account.
        ContentValues().apply { put(COL_ACCOUNT_ID, Transaction.DEFAULT_ACCOUNT_ID) }.also { cv ->
            db.update(TABLE_TRANSACTIONS, cv, "$COL_ACCOUNT_ID = ?", arrayOf(id.toString()))
        }
        db.delete(TABLE_ACCOUNTS, "$COL_ID = ?", arrayOf(id.toString()))
    }

    private fun computeBalance(db: SQLiteDatabase, account: Account): Double {
        val cursor = db.rawQuery(
            """
            SELECT
              COALESCE(SUM(CASE WHEN $COL_TYPE = ? THEN $COL_AMOUNT ELSE 0 END), 0) -
              COALESCE(SUM(CASE WHEN $COL_TYPE = ? THEN $COL_AMOUNT ELSE 0 END), 0)
            FROM $TABLE_TRANSACTIONS WHERE $COL_ACCOUNT_ID = ?
            """,
            arrayOf(
                Transaction.TransactionType.INCOME.name,
                Transaction.TransactionType.EXPENSE.name,
                account.id.toString()
            )
        )
        val net = cursor.use { if (it.moveToFirst()) it.getDouble(0) else 0.0 }
        return account.initialBalance + net
    }

    private fun accountToContentValues(account: Account): ContentValues = ContentValues().apply {
        if (account.id != 0L) put(COL_ID, account.id)
        put(COL_ACCOUNT_NAME, account.name)
        put(COL_INITIAL_BALANCE, account.initialBalance)
        put(COL_CREATED_AT, account.createdAt)
    }

    private fun cursorToAccount(cursor: Cursor): Account = Account(
        id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
        name = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACCOUNT_NAME)),
        initialBalance = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_INITIAL_BALANCE)),
        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT))
    )

    // ==================== Recurring Rule Operations ====================

    fun getAllRecurringRules(): Flow<List<RecurringRule>> = flow {
        emit(queryRecurringRules(null, null))
    }

    suspend fun getDueRecurringRules(now: Long): List<RecurringRule> =
        queryRecurringRules("$COL_ACTIVE = 1 AND $COL_NEXT_RUN <= ?", arrayOf(now.toString()))

    suspend fun getRecurringRuleById(id: Long): RecurringRule? =
        queryRecurringRules("$COL_ID = ?", arrayOf(id.toString())).firstOrNull()

    suspend fun insertRecurringRule(rule: RecurringRule): Long {
        val db = writableDatabase
        return db.insert(TABLE_RECURRING, null, recurringToContentValues(rule))
    }

    suspend fun updateRecurringRule(rule: RecurringRule) {
        val db = writableDatabase
        db.update(
            TABLE_RECURRING,
            recurringToContentValues(rule),
            "$COL_ID = ?",
            arrayOf(rule.id.toString())
        )
    }

    suspend fun deleteRecurringRule(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_RECURRING, "$COL_ID = ?", arrayOf(id.toString()))
    }

    private fun queryRecurringRules(selection: String?, args: Array<String>?): List<RecurringRule> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_RECURRING, null, selection, args, null, null, "$COL_NEXT_RUN ASC"
        )
        val rules = mutableListOf<RecurringRule>()
        cursor.use {
            while (it.moveToNext()) rules.add(cursorToRecurringRule(it))
        }
        return rules
    }

    private fun recurringToContentValues(rule: RecurringRule): ContentValues = ContentValues().apply {
        if (rule.id != 0L) put(COL_ID, rule.id)
        put(COL_TYPE, rule.type.name)
        put(COL_AMOUNT, rule.amount)
        put(COL_CATEGORY, rule.category)
        put(COL_DESCRIPTION, rule.description)
        put(COL_IS_RIBA, if (rule.isRiba) 1 else 0)
        put(COL_FREQUENCY, rule.frequency.name)
        put(COL_INTERVAL, rule.intervalCount)
        put(COL_NEXT_RUN, rule.nextRun)
        put(COL_ACTIVE, if (rule.active) 1 else 0)
        put(COL_CREATED_AT, rule.createdAt)
    }

    private fun cursorToRecurringRule(cursor: Cursor): RecurringRule = RecurringRule(
        id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
        type = Transaction.TransactionType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE))),
        amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT)),
        category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)),
        description = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)) ?: "",
        isRiba = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_RIBA)) == 1,
        frequency = RecurringRule.Frequency.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COL_FREQUENCY))),
        intervalCount = cursor.getInt(cursor.getColumnIndexOrThrow(COL_INTERVAL)),
        nextRun = cursor.getLong(cursor.getColumnIndexOrThrow(COL_NEXT_RUN)),
        active = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ACTIVE)) == 1,
        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT))
    )

    suspend fun getTransactionsPage(
        limit: Int,
        offset: Int,
        type: Transaction.TransactionType?,
        category: String?,
        query: String? = null
    ): List<Transaction> {
        val db = readableDatabase
        val whereParts = mutableListOf<String>()
        val args = mutableListOf<String>()
        type?.let {
            whereParts.add("$COL_TYPE = ?")
            args.add(it.name)
        }
        category?.let {
            whereParts.add("$COL_CATEGORY = ?")
            args.add(it)
        }
        query?.takeIf { it.isNotBlank() }?.let {
            whereParts.add("($COL_DESCRIPTION LIKE ? OR $COL_CATEGORY LIKE ?)")
            val like = "%${it.trim()}%"
            args.add(like)
            args.add(like)
        }
        val selection = if (whereParts.isEmpty()) null else whereParts.joinToString(" AND ")
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            selection,
            if (args.isEmpty()) null else args.toTypedArray(),
            null,
            null,
            "$COL_DATE DESC",
            "$offset, $limit"
        )
        return cursorToTransactionList(cursor)
    }

    suspend fun getTransactionsCount(
        type: Transaction.TransactionType?,
        category: String?,
        query: String? = null
    ): Int {
        val db = readableDatabase
        val whereParts = mutableListOf<String>()
        val args = mutableListOf<String>()
        type?.let {
            whereParts.add("$COL_TYPE = ?")
            args.add(it.name)
        }
        category?.let {
            whereParts.add("$COL_CATEGORY = ?")
            args.add(it)
        }
        query?.takeIf { it.isNotBlank() }?.let {
            whereParts.add("($COL_DESCRIPTION LIKE ? OR $COL_CATEGORY LIKE ?)")
            val like = "%${it.trim()}%"
            args.add(like)
            args.add(like)
        }
        val whereClause = if (whereParts.isEmpty()) "" else "WHERE ${whereParts.joinToString(" AND ")}"
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_TRANSACTIONS $whereClause",
            if (args.isEmpty()) null else args.toTypedArray()
        )
        return cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    suspend fun getBudgetsPage(month: String, limit: Int, offset: Int): List<Budget> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_BUDGETS,
            null,
            "$COL_MONTH = ?",
            arrayOf(month),
            null,
            null,
            "$COL_CATEGORY ASC",
            "$offset, $limit"
        )
        return cursorToBudgetList(cursor)
    }

    suspend fun getBudgetsCount(month: String): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_BUDGETS WHERE $COL_MONTH = ?",
            arrayOf(month)
        )
        return cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    suspend fun getDebtsPage(limit: Int, offset: Int): List<Debt> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_DEBTS,
            null,
            null,
            null,
            null,
            null,
            "$COL_DUE_DATE DESC",
            "$offset, $limit"
        )
        return cursorToDebtList(cursor)
    }

    suspend fun getDebtsCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_DEBTS", null)
        return cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }


    // ==================== Helper Methods ====================

    private fun queryAllTransactions(): List<Transaction> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            null, null, null, null,
            "$COL_DATE DESC"
        )
        return cursorToTransactionList(cursor)
    }

    private fun queryTransactionsByType(type: Transaction.TransactionType): List<Transaction> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            "$COL_TYPE = ?",
            arrayOf(type.name),
            null, null,
            "$COL_DATE DESC"
        )
        return cursorToTransactionList(cursor)
    }

    private fun queryTransactionsByMonth(year: Int, month: Int): List<Transaction> {
        val db = readableDatabase
        val startDate = getMonthStart(year, month)
        val endDate = getMonthEnd(year, month)
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            "$COL_DATE >= ? AND $COL_DATE <= ?",
            arrayOf(startDate.toString(), endDate.toString()),
            null, null,
            "$COL_DATE DESC"
        )
        return cursorToTransactionList(cursor)
    }

    private fun queryTransactionsByCategory(category: String): List<Transaction> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            "$COL_CATEGORY = ?",
            arrayOf(category),
            null, null,
            "$COL_DATE DESC"
        )
        return cursorToTransactionList(cursor)
    }

    private fun queryRecentTransactions(limit: Int): List<Transaction> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            null, null, null, null,
            "$COL_DATE DESC",
            limit.toString()
        )
        return cursorToTransactionList(cursor)
    }

    private fun queryTransactionsByDateRange(startDate: Long, endDate: Long): List<Transaction> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            "$COL_DATE >= ? AND $COL_DATE <= ?",
            arrayOf(startDate.toString(), endDate.toString()),
            null, null,
            "$COL_DATE DESC"
        )
        return cursorToTransactionList(cursor)
    }

    private fun queryAllBudgets(): List<Budget> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_BUDGETS,
            null,
            null, null, null, null,
            "$COL_MONTH DESC, $COL_CATEGORY ASC"
        )
        return cursorToBudgetList(cursor)
    }

    private fun queryBudgetsByMonth(month: String): List<Budget> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_BUDGETS,
            null,
            "$COL_MONTH = ?",
            arrayOf(month),
            null, null,
            "$COL_CATEGORY ASC"
        )
        return cursorToBudgetList(cursor)
    }

    private fun queryAllImportHistory(): List<ImportHistory> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_IMPORT_HISTORY,
            null,
            null, null, null, null,
            "$COL_IMPORT_DATE DESC"
        )
        return cursorToImportHistoryList(cursor)
    }

    private fun queryAllDebts(): List<Debt> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_DEBTS,
            null,
            null, null, null, null,
            "$COL_DUE_DATE DESC"
        )
        return cursorToDebtList(cursor)
    }

    private fun cursorToTransactionList(cursor: Cursor): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        cursor.use {
            while (it.moveToNext()) {
                transactions.add(cursorToTransaction(it))
            }
        }
        return transactions
    }

    private fun cursorToTransaction(cursor: Cursor): Transaction {
        return Transaction(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            type = Transaction.TransactionType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE))),
            amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT)),
            category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)),
            description = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)),
            date = cursor.getLong(cursor.getColumnIndexOrThrow(COL_DATE)),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT)),
            isRiba = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_RIBA)) == 1,
            accountId = cursor.getColumnIndex(COL_ACCOUNT_ID).let { idx ->
                if (idx >= 0) cursor.getLong(idx) else Transaction.DEFAULT_ACCOUNT_ID
            }
        )
    }

    private fun cursorToBudgetList(cursor: Cursor): List<Budget> {
        val budgets = mutableListOf<Budget>()
        cursor.use {
            while (it.moveToNext()) {
                budgets.add(cursorToBudget(it))
            }
        }
        return budgets
    }

    private fun cursorToBudget(cursor: Cursor): Budget {
        return Budget(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)),
            monthlyBudget = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_MONTHLY_BUDGET)),
            month = cursor.getString(cursor.getColumnIndexOrThrow(COL_MONTH)),
            spent = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_SPENT)),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT))
        )
    }

    private fun cursorToImportHistoryList(cursor: Cursor): List<ImportHistory> {
        val history = mutableListOf<ImportHistory>()
        cursor.use {
            while (it.moveToNext()) {
                history.add(cursorToImportHistory(it))
            }
        }
        return history
    }

    private fun cursorToImportHistory(cursor: Cursor): ImportHistory {
        return ImportHistory(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            fileName = cursor.getString(cursor.getColumnIndexOrThrow(COL_FILE_NAME)),
            importDate = cursor.getLong(cursor.getColumnIndexOrThrow(COL_IMPORT_DATE)),
            transactionCount = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSACTION_COUNT)),
            status = ImportHistory.ImportStatus.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COL_STATUS)))
        )
    }

    private fun cursorToDebtList(cursor: Cursor): List<Debt> {
        val debts = mutableListOf<Debt>()
        cursor.use {
            while (it.moveToNext()) {
                debts.add(cursorToDebt(it))
            }
        }
        return debts
    }

    private fun cursorToDebt(cursor: Cursor): Debt {
        return Debt(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            personName = cursor.getString(cursor.getColumnIndexOrThrow(COL_PERSON_NAME)),
            amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT)),
            type = Debt.DebtType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE))),
            dueDate = cursor.getLong(cursor.getColumnIndexOrThrow(COL_DUE_DATE)),
            description = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)),
            isPaid = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_PAID)) == 1,
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT)),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_UPDATED_AT))
        )
    }

    private fun transactionToContentValues(transaction: Transaction): ContentValues {
        return ContentValues().apply {
            put(COL_TYPE, transaction.type.name)
            put(COL_AMOUNT, transaction.amount)
            put(COL_CATEGORY, transaction.category)
            put(COL_DESCRIPTION, transaction.description)
            put(COL_DATE, transaction.date)
            put(COL_CREATED_AT, transaction.createdAt)
            put(COL_IS_RIBA, if (transaction.isRiba) 1 else 0)
            put(COL_ACCOUNT_ID, transaction.accountId)
        }
    }

    private fun budgetToContentValues(budget: Budget): ContentValues {
        return ContentValues().apply {
            put(COL_CATEGORY, budget.category)
            put(COL_MONTHLY_BUDGET, budget.monthlyBudget)
            put(COL_MONTH, budget.month)
            put(COL_SPENT, budget.spent)
            put(COL_CREATED_AT, budget.createdAt)
        }
    }

    private fun importHistoryToContentValues(history: ImportHistory): ContentValues {
        return ContentValues().apply {
            put(COL_FILE_NAME, history.fileName)
            put(COL_IMPORT_DATE, history.importDate)
            put(COL_TRANSACTION_COUNT, history.transactionCount)
            put(COL_STATUS, history.status.name)
        }
    }

    private fun debtToContentValues(debt: Debt): ContentValues {
        return ContentValues().apply {
            put(COL_PERSON_NAME, debt.personName)
            put(COL_AMOUNT, debt.amount)
            put(COL_TYPE, debt.type.name)
            put(COL_DUE_DATE, debt.dueDate)
            put(COL_DESCRIPTION, debt.description)
            put(COL_IS_PAID, if (debt.isPaid) 1 else 0)
            put(COL_CREATED_AT, debt.createdAt)
            put(COL_UPDATED_AT, debt.updatedAt)
        }
    }

    private fun getMonthStart(year: Int, month: Int): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getMonthEnd(year: Int, month: Int): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(year, month - 1, 1, 23, 59, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)
        calendar.set(java.util.Calendar.DAY_OF_MONTH, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
        return calendar.timeInMillis
    }
}
