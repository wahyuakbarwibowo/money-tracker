package com.aminmart.moneymanager.presentation.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aminmart.moneymanager.MoneyManagerApplication
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.data.backup.BackupManager
import com.aminmart.moneymanager.domain.repository.BackupRepository
import com.aminmart.moneymanager.presentation.viewmodels.SettingsViewModel
import com.google.android.material.switchmaterial.SwitchMaterial
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class DataManagementActivity : AppCompatActivity() {

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var app: MoneyManagerApplication
    private lateinit var viewModel: SettingsViewModel

    private lateinit var toolbar: Toolbar
    private lateinit var switchAutoBackup: SwitchMaterial
    private lateinit var textBackupLocation: TextView
    private lateinit var recyclerBackups: RecyclerView
    private lateinit var viewEmpty: View

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            performBackup()
        } else {
            Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            restoreFromUri(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_management)

        app = application as MoneyManagerApplication
        viewModel = SettingsViewModel(
            app.createBackupUseCase,
            app.restoreBackupUseCase,
            app.getAvailableBackupsUseCase,
            app.autoBackupUseCase,
            app.deleteBackupUseCase
        )

        initViews()
        setupRecyclerView()
        observeData()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar_data_management)
        switchAutoBackup = findViewById(R.id.switch_settings_auto_backup)
        textBackupLocation = findViewById(R.id.text_settings_backup_location)
        recyclerBackups = findViewById(R.id.recycler_settings_backups)
        viewEmpty = findViewById(R.id.view_settings_empty)

        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.data_management)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        textBackupLocation.text = viewModel.getBackupFolder()

        findViewById<View>(R.id.button_settings_backup).setOnClickListener {
            checkPermissionAndBackup()
        }

        findViewById<View>(R.id.button_settings_restore).setOnClickListener {
            showRestoreOptions()
        }

        findViewById<View>(R.id.button_settings_import_csv).setOnClickListener {
            showImportOptions()
        }

        findViewById<View>(R.id.button_settings_export_report).setOnClickListener {
            startActivity(Intent(this, ExportReportActivity::class.java))
        }

        switchAutoBackup.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAutoBackupEnabled(isChecked)
        }
    }

    private fun setupRecyclerView() {
        val adapter = BackupAdapter(
            onItemClick = { path ->
                confirmRestore(path)
            },
            onDeleteClick = { path ->
                confirmDeleteBackup(path)
            }
        )
        recyclerBackups.layoutManager = LinearLayoutManager(this)
        recyclerBackups.adapter = adapter

        viewModel.backups.collectInScope { backups ->
            adapter.submitList(backups)
            viewEmpty.visibility = if (backups.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun observeData() {
        viewModel.autoBackupEnabled.collectInScope { enabled ->
            switchAutoBackup.isChecked = enabled
        }
    }

    private fun checkPermissionAndBackup() {
        // Writes to app-scoped external storage (getExternalFilesDir), which needs
        // no runtime storage permission on any API level.
        performBackup()
    }

    private fun performBackup() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "backup_$timestamp.json"
        val backupDir = File(
            getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "MoneyManagerBackup"
        )
        backupDir.mkdirs()
        val destinationPath = File(backupDir, fileName).absolutePath

        activityScope.launch {
            val result = viewModel.createBackup(BackupRepository.BackupFormat.JSON, destinationPath)
            if (result != null) {
                Toast.makeText(
                    this@DataManagementActivity,
                    getString(R.string.backup_created, fileName),
                    Toast.LENGTH_LONG
                ).show()
                viewModel.loadBackups()
            } else {
                Toast.makeText(this@DataManagementActivity, getString(R.string.backup_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRestoreOptions() {
        val options = arrayOf(getString(R.string.choose_from_backups), getString(R.string.select_file))
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.restore_from))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showBackupsList()
                    1 -> filePickerLauncher.launch("*/*")
                }
            }
            .show()
    }

    private fun showImportOptions() {
        val options = arrayOf(getString(R.string.import_csv_bank_statement), getString(R.string.import_json_backup))
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.import_data))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, ImportCsvActivity::class.java))
                    1 -> filePickerLauncher.launch("application/json")
                }
            }
            .show()
    }

    private fun showBackupsList() {
        val backups = viewModel.backups.value
        if (backups.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_backups_available), Toast.LENGTH_SHORT).show()
            return
        }

        val backupNames = backups.map { File(it).name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_backup))
            .setItems(backupNames) { _, index ->
                confirmRestore(backups[index])
            }
            .show()
    }

    private fun confirmRestore(path: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.restore_backup))
            .setMessage(getString(R.string.restore_backup_message, File(path).name))
            .setPositiveButton(getString(R.string.restore)) { _, _ ->
                restoreBackup(path)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun restoreFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File(cacheDir, "restore_temp.json")
            tempFile.outputStream().use { output ->
                inputStream?.copyTo(output)
            }
            restoreBackup(tempFile.absolutePath)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.failed_read_file, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
        }
    }

    private fun restoreBackup(path: String) {
        activityScope.launch {
            try {
                val success = viewModel.restoreBackup(path)
                if (success) {
                    Toast.makeText(this@DataManagementActivity, getString(R.string.restore_successful), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@DataManagementActivity, getString(R.string.restore_failed), Toast.LENGTH_SHORT).show()
                }
            } catch (e: BackupManager.InvalidBackupException) {
                Toast.makeText(
                    this@DataManagementActivity,
                    e.message ?: getString(R.string.invalid_backup_format),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun confirmDeleteBackup(path: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_backup))
            .setMessage(getString(R.string.delete_item_message, File(path).name))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                activityScope.launch {
                    val success = viewModel.deleteBackup(path)
                    if (success) {
                        Toast.makeText(this@DataManagementActivity, getString(R.string.backup_deleted), Toast.LENGTH_SHORT).show()
                        viewModel.loadBackups()
                    } else {
                        Toast.makeText(this@DataManagementActivity, getString(R.string.failed_to_delete), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
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

    override fun onResume() {
        super.onResume()
        viewModel.loadBackups()
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }

    private fun <T> Flow<T>.collectInScope(action: suspend (T) -> Unit) {
        activityScope.launch {
            collect { action(it) }
        }
    }
}

class BackupAdapter(
    private var backups: List<String> = emptyList(),
    private val onItemClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<BackupAdapter.BackupViewHolder>() {

    class BackupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textName: TextView = itemView.findViewById(R.id.text_backup_name)
        val textDate: TextView = itemView.findViewById(R.id.text_backup_date)
        val buttonDelete: View = itemView.findViewById(R.id.button_backup_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BackupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_backup, parent, false)
        return BackupViewHolder(view)
    }

    override fun onBindViewHolder(holder: BackupViewHolder, position: Int) {
        val path = backups[position]
        val file = File(path)

        holder.textName.text = file.name
        holder.textDate.text = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
            .format(Date(file.lastModified()))

        holder.itemView.setOnClickListener { onItemClick(path) }
        holder.buttonDelete.setOnClickListener { onDeleteClick(path) }
    }

    override fun getItemCount(): Int = backups.size

    fun submitList(newBackups: List<String>) {
        backups = newBackups
        notifyDataSetChanged()
    }
}
