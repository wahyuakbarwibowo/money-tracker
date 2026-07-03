package com.aminmart.moneymanager

import android.app.Application
import android.content.Context
import com.aminmart.moneymanager.data.backup.BackupManager
import com.aminmart.moneymanager.data.database.MoneyDatabase
import com.aminmart.moneymanager.data.datasource.ExportManager
import com.aminmart.moneymanager.data.importer.CsvImporter
import com.aminmart.moneymanager.data.repository.BudgetRepositoryImpl
import com.aminmart.moneymanager.data.repository.DebtRepositoryImpl
import com.aminmart.moneymanager.data.repository.AccountRepositoryImpl
import com.aminmart.moneymanager.data.repository.ImportHistoryRepositoryImpl
import com.aminmart.moneymanager.data.repository.RecurringRepositoryImpl
import com.aminmart.moneymanager.data.repository.TransactionRepositoryImpl
import com.aminmart.moneymanager.domain.repository.BackupRepository
import com.aminmart.moneymanager.domain.repository.BudgetRepository
import com.aminmart.moneymanager.domain.repository.CsvImportRepository
import com.aminmart.moneymanager.domain.repository.DebtRepository
import com.aminmart.moneymanager.domain.repository.ExportRepository
import com.aminmart.moneymanager.domain.repository.ImportHistoryRepository
import com.aminmart.moneymanager.domain.repository.TransactionRepository
import com.aminmart.moneymanager.domain.usecase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.aminmart.moneymanager.presentation.notifications.NotificationHelper
import com.aminmart.moneymanager.presentation.ui.ThemePreference
import com.aminmart.moneymanager.work.WorkScheduler

/**
 * Application class - Entry point for the app
 * Initializes dependencies and provides access to use cases
 */
class MoneyManagerApplication : Application() {

    // Database
    lateinit var database: MoneyDatabase
        private set

    // Repositories
    lateinit var transactionRepository: TransactionRepository
        private set
    lateinit var budgetRepository: BudgetRepository
        private set
    lateinit var importHistoryRepository: ImportHistoryRepository
        private set
    lateinit var csvImportRepository: CsvImportRepository
        private set
    lateinit var backupRepository: BackupRepository
        private set
    lateinit var exportRepository: ExportRepository
        private set
    lateinit var debtRepository: DebtRepository
        private set
    lateinit var recurringRepository: RecurringRepositoryImpl
        private set
    lateinit var accountRepository: AccountRepositoryImpl
        private set

    // Use Cases - Transactions
    lateinit var getAllTransactionsUseCase: GetAllTransactionsUseCase
        private set
    lateinit var getRecentTransactionsUseCase: GetRecentTransactionsUseCase
        private set
    lateinit var getTransactionsByMonthUseCase: GetTransactionsByMonthUseCase
        private set
    lateinit var getTransactionsByCategoryUseCase: GetTransactionsByCategoryUseCase
        private set
    lateinit var getTransactionsPageUseCase: GetTransactionsPageUseCase
        private set
    lateinit var getTransactionsCountUseCase: GetTransactionsCountUseCase
        private set
    lateinit var addTransactionUseCase: AddTransactionUseCase
        private set
    lateinit var updateTransactionUseCase: UpdateTransactionUseCase
        private set
    lateinit var deleteTransactionUseCase: DeleteTransactionUseCase
        private set
    lateinit var getTransactionByIdUseCase: GetTransactionByIdUseCase
        private set
    lateinit var getDashboardStatsUseCase: GetDashboardStatsUseCase
        private set
    lateinit var getExpenseByCategoryUseCase: GetExpenseByCategoryUseCase
        private set
    lateinit var getMonthlyExpensesUseCase: GetMonthlyExpensesUseCase
        private set
    lateinit var syncBudgetSpentUseCase: SyncBudgetSpentUseCase
        private set
    lateinit var getRibaReportUseCase: GetRibaReportUseCase
        private set

    // Use Cases - Budget
    lateinit var getAllBudgetsUseCase: GetAllBudgetsUseCase
        private set
    lateinit var getCurrentMonthBudgetsUseCase: GetCurrentMonthBudgetsUseCase
        private set
    lateinit var saveBudgetUseCase: SaveBudgetUseCase
        private set
    lateinit var deleteBudgetUseCase: DeleteBudgetUseCase
        private set
    lateinit var getBudgetByCategoryUseCase: GetBudgetByCategoryUseCase
        private set
    lateinit var getBudgetsPageUseCase: GetBudgetsPageUseCase
        private set
    lateinit var getBudgetsCountUseCase: GetBudgetsCountUseCase
        private set
    lateinit var rolloverBudgetsUseCase: RolloverBudgetsUseCase
        private set

    // Use Cases - Import
    lateinit var importCsvUseCase: ImportCsvUseCase
        private set
    lateinit var validateCsvUseCase: ValidateCsvUseCase
        private set
    lateinit var getCsvPreviewUseCase: GetCsvPreviewUseCase
        private set

    // Use Cases - Export
    lateinit var exportToCsvUseCase: ExportToCsvUseCase
        private set
    lateinit var exportToExcelUseCase: ExportToExcelUseCase
        private set
    lateinit var exportReportUseCase: ExportReportUseCase
        private set

    // Use Cases - Backup
    lateinit var createBackupUseCase: CreateBackupUseCase
        private set
    lateinit var restoreBackupUseCase: RestoreBackupUseCase
        private set
    lateinit var getAvailableBackupsUseCase: GetAvailableBackupsUseCase
        private set
    lateinit var autoBackupUseCase: AutoBackupUseCase
        private set
    lateinit var deleteBackupUseCase: DeleteBackupUseCase
        private set

    // Use Cases - Debt
    lateinit var debtUseCases: DebtUseCases
        private set

    // Use Cases - Recurring
    lateinit var recurringUseCases: RecurringUseCases
        private set

    // Use Cases - Accounts
    lateinit var accountUseCases: AccountUseCases
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        ThemePreference.apply(this)
        initializeDependencies()
        NotificationHelper.ensureChannels(this)
        WorkScheduler.schedule(this)

        // Catch up any due recurring transactions on launch.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            recurringUseCases.generateDue()
        }
    }

    private fun initializeDependencies() {
        // Initialize database
        database = MoneyDatabase(this)

        // Initialize repositories
        transactionRepository = TransactionRepositoryImpl(database)
        budgetRepository = BudgetRepositoryImpl(database)
        importHistoryRepository = ImportHistoryRepositoryImpl(database)
        csvImportRepository = CsvImporter(transactionRepository)
        backupRepository = BackupManager(this, transactionRepository, budgetRepository)
        exportRepository = ExportManager(this, transactionRepository)
        debtRepository = DebtRepositoryImpl(database)
        recurringRepository = RecurringRepositoryImpl(database)
        accountRepository = AccountRepositoryImpl(database)

        // Initialize Use Cases - Transactions
        syncBudgetSpentUseCase = SyncBudgetSpentUseCase(transactionRepository, budgetRepository)
        getAllTransactionsUseCase = GetAllTransactionsUseCase(transactionRepository)
        getRecentTransactionsUseCase = GetRecentTransactionsUseCase(transactionRepository)
        getTransactionsByMonthUseCase = GetTransactionsByMonthUseCase(transactionRepository)
        getTransactionsByCategoryUseCase = GetTransactionsByCategoryUseCase(transactionRepository)
        getTransactionsPageUseCase = GetTransactionsPageUseCase(transactionRepository)
        getTransactionsCountUseCase = GetTransactionsCountUseCase(transactionRepository)
        addTransactionUseCase = AddTransactionUseCase(transactionRepository, syncBudgetSpentUseCase)
        updateTransactionUseCase = UpdateTransactionUseCase(transactionRepository, syncBudgetSpentUseCase)
        deleteTransactionUseCase = DeleteTransactionUseCase(transactionRepository, syncBudgetSpentUseCase)
        getTransactionByIdUseCase = GetTransactionByIdUseCase(transactionRepository)
        getDashboardStatsUseCase = GetDashboardStatsUseCase(transactionRepository, budgetRepository)
        getExpenseByCategoryUseCase = GetExpenseByCategoryUseCase(transactionRepository)
        getMonthlyExpensesUseCase = GetMonthlyExpensesUseCase(transactionRepository)
        getRibaReportUseCase = GetRibaReportUseCase(transactionRepository)

        // Initialize Use Cases - Budget
        getAllBudgetsUseCase = GetAllBudgetsUseCase(budgetRepository)
        getCurrentMonthBudgetsUseCase = GetCurrentMonthBudgetsUseCase(budgetRepository)
        saveBudgetUseCase = SaveBudgetUseCase(budgetRepository)
        deleteBudgetUseCase = DeleteBudgetUseCase(budgetRepository)
        getBudgetByCategoryUseCase = GetBudgetByCategoryUseCase(budgetRepository)
        getBudgetsPageUseCase = GetBudgetsPageUseCase(budgetRepository)
        getBudgetsCountUseCase = GetBudgetsCountUseCase(budgetRepository)
        rolloverBudgetsUseCase = RolloverBudgetsUseCase(budgetRepository)

        // Initialize Use Cases - Import
        importCsvUseCase = ImportCsvUseCase(csvImportRepository, importHistoryRepository)
        validateCsvUseCase = ValidateCsvUseCase(csvImportRepository)
        getCsvPreviewUseCase = GetCsvPreviewUseCase(csvImportRepository)

        // Initialize Use Cases - Export
        exportToCsvUseCase = ExportToCsvUseCase(exportRepository)
        exportToExcelUseCase = ExportToExcelUseCase(exportRepository)
        exportReportUseCase = ExportReportUseCase(exportRepository)

        // Initialize Use Cases - Backup
        createBackupUseCase = CreateBackupUseCase(backupRepository)
        restoreBackupUseCase = RestoreBackupUseCase(backupRepository)
        getAvailableBackupsUseCase = GetAvailableBackupsUseCase(backupRepository)
        autoBackupUseCase = AutoBackupUseCase(backupRepository)
        deleteBackupUseCase = DeleteBackupUseCase(backupRepository)

        // Initialize Use Cases - Debt
        debtUseCases = DebtUseCases(
            getAllDebts = GetAllDebts(debtRepository),
            getDebtsPage = GetDebtsPage(debtRepository),
            getDebtsCount = GetDebtsCount(debtRepository),
            getDebtById = GetDebtById(debtRepository),
            addDebt = AddDebt(debtRepository),
            updateDebt = UpdateDebt(debtRepository),
            deleteDebt = DeleteDebt(debtRepository)
        )

        // Initialize Use Cases - Recurring
        recurringUseCases = RecurringUseCases(
            getRules = GetRecurringRulesUseCase(recurringRepository),
            addRule = AddRecurringRuleUseCase(recurringRepository),
            deleteRule = DeleteRecurringRuleUseCase(recurringRepository),
            generateDue = GenerateDueTransactionsUseCase(recurringRepository, addTransactionUseCase)
        )

        // Initialize Use Cases - Accounts
        accountUseCases = AccountUseCases(
            getAccounts = GetAccountsUseCase(accountRepository),
            addAccount = AddAccountUseCase(accountRepository),
            deleteAccount = DeleteAccountUseCase(accountRepository)
        )
    }

    companion object {
        lateinit var instance: MoneyManagerApplication
            private set

        fun getAppContext(): Context {
            return instance.applicationContext
        }
    }
}
