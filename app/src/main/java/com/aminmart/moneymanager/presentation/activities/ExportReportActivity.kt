package com.aminmart.moneymanager.presentation.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.aminmart.moneymanager.MoneyManagerApplication
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.domain.repository.ExportRepository
import com.aminmart.moneymanager.presentation.viewmodels.ExportReportViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Export Report Activity - Export transactions to CSV/Excel
 */
class ExportReportActivity : AppCompatActivity() {

    private lateinit var app: MoneyManagerApplication
    private lateinit var viewModel: ExportReportViewModel

    private lateinit var toolbar: Toolbar
    private lateinit var dropdownFormat: AutoCompleteTextView
    private lateinit var dropdownPeriod: AutoCompleteTextView
    private lateinit var buttonExport: Button

    private var startDate: Long? = null
    private var endDate: Long? = null
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export_report)

        app = application as MoneyManagerApplication
        viewModel = ExportReportViewModel(
            app.exportToCsvUseCase,
            app.exportToExcelUseCase,
            app.exportReportUseCase
        )

        initViews()
        setupDropdowns()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar_export_report)
        dropdownFormat = findViewById(R.id.dropdown_export_format)
        dropdownPeriod = findViewById(R.id.dropdown_export_period)
        buttonExport = findViewById(R.id.button_export_execute)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "Export Report"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        buttonExport.setOnClickListener {
            checkPermissionAndExport()
        }
    }

    private fun setupDropdowns() {
        // Format dropdown
        val formats = arrayOf("CSV", "Excel (.xlsx)")
        val formatAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, formats)
        dropdownFormat.setAdapter(formatAdapter)
        dropdownFormat.setText(formats[0], false)

        // Period dropdown
        val periods = arrayOf(
            "All Time",
            "Current Month",
            "Last Month",
            "Last 3 Months",
            "Last 6 Months",
            "Current Year",
            "Custom Range"
        )
        val periodAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, periods)
        dropdownPeriod.setAdapter(periodAdapter)
        dropdownPeriod.setText(periods[0], false)

        dropdownPeriod.setOnItemClickListener { _, _, position, _ ->
            setDateRange(position)
        }
    }

    private fun setDateRange(position: Int) {
        val calendar = Calendar.getInstance()
        
        when (position) {
            0 -> { // All Time
                startDate = null
                endDate = null
            }
            1 -> { // Current Month
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                startDate = calendar.timeInMillis
                
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                endDate = calendar.timeInMillis
            }
            2 -> { // Last Month
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                startDate = calendar.timeInMillis
                
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                endDate = calendar.timeInMillis
            }
            3 -> { // Last 3 Months
                calendar.add(Calendar.MONTH, -3)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                startDate = calendar.timeInMillis
                
                endDate = System.currentTimeMillis()
            }
            4 -> { // Last 6 Months
                calendar.add(Calendar.MONTH, -6)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                startDate = calendar.timeInMillis
                
                endDate = System.currentTimeMillis()
            }
            5 -> { // Current Year
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                startDate = calendar.timeInMillis
                
                endDate = System.currentTimeMillis()
            }
            6 -> { // Custom Range - would show date picker dialog
                showCustomDateRangePicker()
            }
        }
    }

    private fun showCustomDateRangePicker() {
        // Implementation for custom date range picker
        Toast.makeText(this, "Custom range - Select start date", Toast.LENGTH_SHORT).show()
    }

    private fun checkPermissionAndExport() {
        // Writes to app-scoped external storage (getExternalFilesDir), which needs
        // no runtime storage permission on any API level.
        performExport()
    }

    private fun performExport() {
        val format = dropdownFormat.text.toString()
        val isExcel = format.contains("Excel")
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val extension = if (isExcel) "xlsx" else "csv"
        val fileName = "money_report_$timestamp.$extension"
        
        val exportDir = File(
            getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "MoneyManagerExport"
        )
        exportDir.mkdirs()
        val destinationPath = File(exportDir, fileName).absolutePath

        activityScope.launch {
            val result = if (isExcel) {
                viewModel.exportToExcel(destinationPath, startDate, endDate)
            } else {
                viewModel.exportToCsv(destinationPath, startDate, endDate)
            }

            if (result != null) {
                Toast.makeText(
                    this@ExportReportActivity,
                    "Report exported: $fileName",
                    Toast.LENGTH_LONG
                ).show()
                
                // Share the file
                shareFile(result)
            } else {
                Toast.makeText(
                    this@ExportReportActivity,
                    "Export failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun shareFile(filePath: String) {
        try {
            val file = File(filePath)
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(android.content.Intent.createChooser(shareIntent, "Share Report"))
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to share: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }
}
