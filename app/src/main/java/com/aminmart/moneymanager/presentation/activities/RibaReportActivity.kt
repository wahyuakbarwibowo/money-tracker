package com.aminmart.moneymanager.presentation.activities

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aminmart.moneymanager.MoneyManagerApplication
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.presentation.adapters.TransactionAdapter
import com.aminmart.moneymanager.presentation.ui.CurrencyFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Read-only report listing transactions flagged as riba, with a total.
 */
class RibaReportActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var textTotal: TextView
    private lateinit var textCount: TextView
    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: View
    private lateinit var adapter: TransactionAdapter

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_riba_report)

        val app = application as MoneyManagerApplication

        toolbar = findViewById(R.id.toolbar_riba)
        textTotal = findViewById(R.id.text_riba_total)
        textCount = findViewById(R.id.text_riba_count)
        recycler = findViewById(R.id.recycler_riba)
        emptyView = findViewById(R.id.view_riba_empty)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        adapter = TransactionAdapter(
            onItemClick = {},
            onItemLongClick = {}
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        scope.launch {
            app.getRibaReportUseCase().collect { report ->
                textTotal.text = CurrencyFormatter.format(report.totalAmount)
                textCount.text = getString(R.string.riba_count_format, report.count)
                adapter.submitList(report.transactions)
                val empty = report.transactions.isEmpty()
                emptyView.visibility = if (empty) View.VISIBLE else View.GONE
                recycler.visibility = if (empty) View.GONE else View.VISIBLE
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
