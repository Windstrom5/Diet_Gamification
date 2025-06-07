package com.example.diet_gamification.report

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.diet_gamification.R
import com.example.diet_gamification.model.AccountModel
import com.example.diet_gamification.room.AppDatabase
import com.example.diet_gamification.room.FoodItemEntity
import com.example.diet_gamifikasi.MainActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry // This import is needed for LineChart
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ReportFragment : Fragment() {
    private var accountModel: AccountModel? = null
    private lateinit var expSummary: TextView
    private lateinit var caloriesSummary: TextView
    private lateinit var switchDataButton: Button
    private lateinit var barChart: BarChart
    private lateinit var btnFilter: ImageButton
    private var showingCalories = true
    private var weekData: List<FoodItemEntity> = emptyList()
    private var allWeeks: List<Pair<String, Pair<String, String>>> = emptyList()
    private var selectedChartType = "Bar" // Default chart type
    // At top of class
    private var currentWeekLabel: String = ""
    private var currentDataType: String = "Calories"
    private var currentChartType: String = "Bar"
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        expSummary = view.findViewById(R.id.expSummary)
        caloriesSummary = view.findViewById(R.id.caloriesSummary)
        switchDataButton = view.findViewById(R.id.switchDataButton)
        btnFilter = view.findViewById(R.id.btnFilter)
        barChart = BarChart(requireContext())
        view.findViewById<ViewGroup>(R.id.barChartContainer).addView(barChart)
        generateWeekRanges()
        currentWeekLabel = allWeeks.last().first
        loadWeekDataForDefaultWeek()
        btnFilter.setOnClickListener { showFilterDialog() }
        switchDataButton.setOnClickListener {
            showingCalories = !showingCalories
            updateBarChart()
        }
    }

    private fun showFilterDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_filter, null)
        val weekSelector    = dialogView.findViewById<AutoCompleteTextView>(R.id.filterWeekSelector)
        val dataTypeSelector= dialogView.findViewById<AutoCompleteTextView>(R.id.filterDataType)
        val chartTypeSelector=dialogView.findViewById<AutoCompleteTextView>(R.id.filterChartType)

        // Adapters
        weekSelector.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, allWeeks.map { it.first }))
        dataTypeSelector.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, listOf("Calories", "Workout")))
        chartTypeSelector.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, listOf("Bar", "Line")))

        // Pre‐populate with current state
        weekSelector.setText(currentWeekLabel, false)
        dataTypeSelector.setText(currentDataType, false)
        chartTypeSelector.setText(currentChartType, false)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Filter Options")
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                applyFilters(dialogView)
                // Show toast of applied filter
                Toast.makeText(requireContext(),
                    "Filtered: Week=$currentWeekLabel, Data=$currentDataType, Chart=$currentChartType",
                    Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .create()

        // Also update as soon as user clicks any item
        weekSelector.setOnItemClickListener    { _, _, _, _ -> applyFilters(dialogView) }
        dataTypeSelector.setOnItemClickListener{ _, _, _, _ -> applyFilters(dialogView) }
        chartTypeSelector.setOnItemClickListener{ _, _, _, _ -> applyFilters(dialogView) }

        dialog.show()
    }

    private fun applyFilters(dialogView: View) {
        val weekText = dialogView.findViewById<AutoCompleteTextView>(R.id.filterWeekSelector).text.toString()
        val dataType = dialogView.findViewById<AutoCompleteTextView>(R.id.filterDataType).text.toString()
        val chartType= dialogView.findViewById<AutoCompleteTextView>(R.id.filterChartType).text.toString()

        // Save to fragment state
        currentWeekLabel  = weekText
        currentDataType   = dataType
        currentChartType  = chartType

        // Update logic
        showingCalories   = (dataType == "Calories")
        selectedChartType = chartType

        allWeeks.find { it.first == weekText }?.second?.let { (start, end) ->
            loadWeekData(start, end)
        }
    }

    private fun generateWeekRanges() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val now = Calendar.getInstance()

        allWeeks = mutableListOf()
        while (!calendar.after(now)) {
            val start = calendar.time
            val endCalendar = calendar.clone() as Calendar
            endCalendar.add(Calendar.DAY_OF_YEAR, 6)

            val end = endCalendar.time
            val label = "${displayFormat.format(start)} - ${displayFormat.format(end)}"
            (allWeeks as MutableList).add(label to (sdf.format(start) to sdf.format(end)))

            calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }
    }

    private fun loadWeekDataForDefaultWeek() {
        val (start, end) = allWeeks.last().second
        loadWeekData(start, end)
    }

    private fun loadWeekData(startOfWeek: String, endOfWeek: String) {
        val database = AppDatabase.getInstance(requireContext())
        val foodDao = database.foodItemDao()

        Thread {
            weekData = foodDao.getFoodItemsBetweenDates(startOfWeek, endOfWeek, accountModel?.id)

            val calendar = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            calendar.time = sdf.parse(startOfWeek)!!

            requireActivity().runOnUiThread {
                updateUI(calendar)
            }
        }.start()
    }

    private fun updateUI(startCalendar: Calendar) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val calendar = startCalendar.clone() as Calendar
        val startDate = sdf.format(calendar.time)

        calendar.add(Calendar.DAY_OF_YEAR, 6)
        val endDate = sdf.format(calendar.time)

        val mainActivity = activity as? MainActivity
        val accountId = mainActivity?.currentAccountModel?.id ?: return

        lifecycleScope.launch {
            val xpHistoryList = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(requireContext())
                    .xpHistoryDao()
                    .getXpHistoryForWeek(accountId, startDate, endDate)
            }

            val totalXp = xpHistoryList.sumOf { it.xpGained }

            val groupedByDate = weekData.groupBy { it.date }
            val caloriesPerDay = mutableListOf<Pair<String, Int>>()
            var totalCalories = 0

            val calendarIter = startCalendar.clone() as Calendar
            for (i in 0..6) {
                val date = sdf.format(calendarIter.time)
                val foods = groupedByDate[date] ?: emptyList()
                val dayCalories = foods.sumOf { it.calories }

                caloriesPerDay.add(dayFormat.format(calendarIter.time) to dayCalories)
                totalCalories += dayCalories

                calendarIter.add(Calendar.DAY_OF_YEAR, 1)
            }

            val avgCalories = if (caloriesPerDay.isNotEmpty()) totalCalories / caloriesPerDay.size else 0

            expSummary.text = "Total exp Gained This Week: $totalXp"
            caloriesSummary.text = "Average Calories This Week: $avgCalories kcal"

            setupBarChart(caloriesPerDay)
        }
    }

    private fun setupBarChart(data: List<Pair<String, Int>>) {
        // Remove old chart view
        val container = view?.findViewById<ViewGroup>(R.id.barChartContainer) ?: return
        container.removeAllViews()

        // Choose the correct chart instance
        val chart = if (selectedChartType == "Bar") {
            BarChart(requireContext()).also { Log.d("ChartType", "Bar") }
        } else {
            LineChart(requireContext()).also { Log.d("ChartType", "Line") }
        }

        // Prepare entries
        val entries = data.mapIndexed { index, pair ->
            BarEntry(index.toFloat(), pair.second.toFloat())
        }

        // DataSet & Data
        if (chart is BarChart) {
            val set = BarDataSet(entries, if (showingCalories) "Calories" else "Workout")
            chart.data = BarData(set)
        } else if (chart is LineChart) {
            val lineSet = LineDataSet(entries.map { Entry(it.x, it.y) }, if (showingCalories) "Calories" else "W")
            chart.data = LineData(lineSet)
        }

        // Common styling
        // Description
        chart.description.apply {
            text = if (showingCalories) "Weekly Calories" else "Weekly Workout"
            textSize = 14f
            isEnabled = true
        }

        // X-Axis
        chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(data.map { it.first })
            position = XAxis.XAxisPosition.BOTTOM
            textSize = 12f
            setDrawGridLines(false)
            granularity = 1f
            labelRotationAngle = -45f
            axisMinimum = 0f
            axisMaximum = (data.size - 1).toFloat()
            // Optional axis label
            setLabelCount(data.size, true)
        }
        // Y-Axis (left)
        chart.axisLeft.apply {
            textSize = 12f
            setDrawGridLines(true)
            axisMinimum = 0f
            // add axis label via a custom renderer or just show units as part of description
        }
        // Hide right axis
        chart.axisRight.isEnabled = false

        // Legend
        chart.legend.apply {
            isWordWrapEnabled = true
            textSize = 12f
        }

        // Add chart to container and refresh
        container.addView(chart, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        chart.invalidate()
    }


    private fun updateBarChart() {
        // Handle bar chart updates based on selected data type (Calories/Workout)
        if (showingCalories) {
            barChart.data?.dataSets?.firstOrNull()?.label = "Calories"
        } else {
            barChart.data?.dataSets?.firstOrNull()?.label = "Workout"
        }
        barChart.invalidate()
    }
}
