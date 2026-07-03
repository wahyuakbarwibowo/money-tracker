package com.aminmart.moneymanager.presentation.activities

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.aminmart.moneymanager.MoneyManagerApplication
import com.aminmart.moneymanager.R
import com.aminmart.moneymanager.presentation.ui.CurrencyFormatter
import com.aminmart.moneymanager.presentation.viewmodels.StatisticsPeriod
import com.aminmart.moneymanager.presentation.viewmodels.StatisticsViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.NumberFormat
import java.util.Locale

/**
 * Statistics Activity - Display charts and graphs
 */
class StatisticsActivity : BottomNavigationActivity() {

    private lateinit var app: MoneyManagerApplication
    private lateinit var viewModel: StatisticsViewModel

    private lateinit var toolbar: Toolbar
    private lateinit var spinnerPeriod: Spinner
    private lateinit var chartPie: PieChart
    private lateinit var chartBar: BarChart
    private lateinit var textPeriodSummary: TextView
    private lateinit var textTotalExpense: TextView
    private lateinit var textCategoryCount: TextView
    private lateinit var textAverageExpense: TextView
    private lateinit var textTopCategory: TextView
    private lateinit var textTopCategoryValue: TextView
    private lateinit var textPeakMonth: TextView
    private lateinit var cardPie: View
    private lateinit var cardBar: View
    private lateinit var cardInsights: View
    private lateinit var viewEmpty: View

    private val currencyFormat = CurrencyFormatter
    private val chartColors by lazy {
        listOf(
            getColor(R.color.chart_blue),
            getColor(R.color.chart_green),
            getColor(R.color.chart_orange),
            getColor(R.color.chart_red),
            getColor(R.color.chart_purple),
            getColor(R.color.chart_teal)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        app = application as MoneyManagerApplication
        viewModel = StatisticsViewModel(
            app.getExpenseByCategoryUseCase,
            app.getMonthlyExpensesUseCase
        )

        initViews()
        setupCharts()
        observeData()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar_statistics)
        spinnerPeriod = findViewById(R.id.spinner_statistics_period)
        chartPie = findViewById(R.id.chart_statistics_pie)
        chartBar = findViewById(R.id.chart_statistics_bar)
        textPeriodSummary = findViewById(R.id.text_statistics_period_summary)
        textTotalExpense = findViewById(R.id.text_statistics_total_expense)
        textCategoryCount = findViewById(R.id.text_statistics_category_count)
        textAverageExpense = findViewById(R.id.text_statistics_average_expense)
        textTopCategory = findViewById(R.id.text_statistics_top_category)
        textTopCategoryValue = findViewById(R.id.text_statistics_top_category_value)
        textPeakMonth = findViewById(R.id.text_statistics_peak_month)
        cardPie = findViewById(R.id.card_statistics_pie)
        cardBar = findViewById(R.id.card_statistics_bar)
        cardInsights = findViewById(R.id.card_statistics_insights)
        viewEmpty = findViewById(R.id.view_statistics_empty)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "Statistics"
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        toolbar.navigationIcon = null
        setupBottomNavigation(R.id.nav_statistics)

        setupPeriodSpinner()
    }

    private fun setupPeriodSpinner() {
        val periods = resources.getStringArray(R.array.statistics_periods)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, periods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPeriod.adapter = adapter

        spinnerPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val period = when (position) {
                    0 -> StatisticsPeriod.CURRENT_MONTH
                    1 -> StatisticsPeriod.LAST_MONTH
                    2 -> StatisticsPeriod.LAST_3_MONTHS
                    3 -> StatisticsPeriod.LAST_6_MONTHS
                    4 -> StatisticsPeriod.CURRENT_YEAR
                    else -> StatisticsPeriod.CURRENT_MONTH
                }
                viewModel.setPeriod(period)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupCharts() {
        chartPie.description = Description().apply { text = "" }
        chartPie.setUsePercentValues(true)
        chartPie.isDrawHoleEnabled = true
        chartPie.holeRadius = 64f
        chartPie.setHoleColor(getColor(R.color.white))
        chartPie.setTransparentCircleColor(getColor(R.color.primary_light))
        chartPie.setTransparentCircleAlpha(60)
        chartPie.setTransparentCircleRadius(68f)
        chartPie.setEntryLabelColor(getColor(R.color.text_primary))
        chartPie.setEntryLabelTextSize(11f)
        chartPie.setDrawRoundedSlices(true)
        chartPie.setExtraOffsets(8f, 8f, 8f, 8f)
        chartPie.legend.apply {
            orientation = Legend.LegendOrientation.VERTICAL
            horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            verticalAlignment = Legend.LegendVerticalAlignment.CENTER
            textColor = getColor(R.color.text_secondary)
            yEntrySpace = 8f
        }

        chartBar.description = Description().apply { text = "" }
        chartBar.setDrawValueAboveBar(true)
        chartBar.setPinchZoom(false)
        chartBar.setDrawGridBackground(false)
        chartBar.setFitBars(true)
        chartBar.setExtraOffsets(8f, 8f, 8f, 12f)
        chartBar.xAxis.apply {
            setDrawGridLines(false)
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            labelRotationAngle = -20f
            textColor = getColor(R.color.text_secondary)
        }
        chartBar.axisLeft.apply {
            setDrawGridLines(true)
            gridColor = getColor(R.color.divider)
            axisMinimum = 0f
            textColor = getColor(R.color.text_secondary)
        }
        chartBar.axisRight.isEnabled = false
        chartBar.legend.apply {
            orientation = Legend.LegendOrientation.HORIZONTAL
            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            textColor = getColor(R.color.text_secondary)
        }
    }

    private fun observeData() {
        viewModel.categoryData.collectInScope { data ->
            if (data.isEmpty()) {
                viewEmpty.visibility = View.VISIBLE
                cardPie.visibility = View.GONE
                cardBar.visibility = View.GONE
                cardInsights.visibility = View.GONE
            } else {
                viewEmpty.visibility = View.GONE
                cardPie.visibility = View.VISIBLE
                cardBar.visibility = View.VISIBLE
                cardInsights.visibility = View.VISIBLE
                updatePieChart(data)
            }
            updateSummary()
        }

        viewModel.monthlyData.collectInScope { data ->
            if (data.isEmpty()) {
                cardBar.visibility = View.GONE
            } else {
                cardBar.visibility = View.VISIBLE
                updateBarChart(data)
            }
            updateSummary()
        }

        viewModel.uiState.collectInScope { _ ->
            // Handle loading state if needed
        }
    }

    private fun updateSummary() {
        val totalExpense = viewModel.getTotalExpense()
        val topCategory = viewModel.getTopCategory()
        val peakMonth = viewModel.getPeakMonth()

        textPeriodSummary.text = "Snapshot for ${viewModel.getSelectedPeriodLabel()}"
        textTotalExpense.text = currencyFormat.format(totalExpense)
        textCategoryCount.text = viewModel.getCategoryCount().toString()
        textAverageExpense.text = currencyFormat.format(viewModel.getAverageMonthlyExpense())
        textTopCategory.text = topCategory?.first ?: "No category yet"
        textTopCategoryValue.text = topCategory?.let {
            val percentage = (viewModel.getCategoryPercentage(it.first) * 100).toInt()
            "${currencyFormat.format(it.second)} • $percentage%"
        } ?: "-"
        textPeakMonth.text = peakMonth?.let {
            "Peak month: ${it.first} • ${currencyFormat.format(it.second)}"
        } ?: "Peak month: -"

        chartPie.centerText = if (totalExpense > 0) {
            "Total\n${currencyFormat.format(totalExpense)}"
        } else {
            "No Data"
        }
    }

    private fun updatePieChart(data: Map<String, Double>) {
        val entries = data
            .toList()
            .sortedByDescending { it.second }
            .map { (category, amount) ->
            PieEntry(amount.toFloat(), category)
        }

        val dataSet = PieDataSet(entries, "Expense by Category").apply {
            colors = chartColors
            sliceSpace = 3f
            selectionShift = 6f
            valueTextColor = getColor(R.color.white)
            valueTextSize = 11f
        }

        chartPie.data = PieData(dataSet).apply {
            setValueTextColor(getColor(R.color.white))
            setValueTextSize(11f)
            setValueFormatter(PercentFormatter(chartPie))
        }
        chartPie.animateY(700)
        chartPie.invalidate()
    }

    private fun updateBarChart(data: Map<String, Double>) {
        val entries = data.entries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.value.toFloat())
        }

        val dataSet = BarDataSet(entries, "Monthly Expense").apply {
            color = getColor(R.color.chart_blue)
            valueTextColor = getColor(R.color.text_secondary)
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val amount = value.toDouble()
                    return when {
                        amount >= 1_000_000 -> "${(amount / 1_000_000).toInt()}jt"
                        amount >= 1_000 -> "${(amount / 1_000).toInt()}k"
                        else -> amount.toInt().toString()
                    }
                }
            }
        }

        chartBar.data = BarData(dataSet).apply {
            barWidth = 0.8f
        }
        chartBar.axisLeft.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val amount = value.toDouble()
                return when {
                    amount >= 1_000_000 -> "${(amount / 1_000_000).toInt()}jt"
                    amount >= 1_000 -> "${(amount / 1_000).toInt()}k"
                    else -> amount.toInt().toString()
                }
            }
        }

        chartBar.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index < data.keys.size) data.keys.elementAt(index) else ""
            }
        }

        chartBar.animateY(700)
        chartBar.invalidate()
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
        viewModel.loadStatistics()
    }

    override fun onDestroy() {
        if (::viewModel.isInitialized) {
            viewModel.clear()
        }
        super.onDestroy()
    }
}
