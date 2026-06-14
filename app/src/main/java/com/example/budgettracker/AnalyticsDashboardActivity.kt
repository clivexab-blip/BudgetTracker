package com.example.budgettracker

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.budgettracker.model.Budget
import com.example.budgettracker.model.Transactions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: DatabaseReference

    private val transactionsList = mutableListOf<Transactions>()
    private val categoryBudgetsMap = mutableMapOf<String, Double>()
    private var minTargetGoal = 0.0
    private var maxBudgetGoal = 0.0

    // Two independent Month State selectors ("ALL_TIME" handles cumulative math)
    private var selectedPieMonthSortKey: String = "ALL_TIME"
    private var selectedBarMonthSortKey: String = "ALL_TIME"

    private val availableMonthsList = mutableListOf<String>()
    private val monthNameToSortKeyMap = mutableMapOf<String, String>()

    private lateinit var tvTotalSpentAnalytics: TextView
    private lateinit var tvTotalIncomeAnalytics: TextView
    private lateinit var tvTransactionCount: TextView

    private lateinit var tvBadgeIcon: TextView
    private lateinit var tvBadgeTitle: TextView
    private lateinit var tvBadgeDesc: TextView

    private lateinit var spinnerPieMonth: Spinner
    private lateinit var spinnerBarMonth: Spinner

    private lateinit var tvPieMinGoal: TextView
    private lateinit var tvPieMaxGoal: TextView
    private lateinit var pieChartView: PieChartView
    private lateinit var llLegend: LinearLayout
    private lateinit var barChartView: BarChartView
    private lateinit var llBarLegend: LinearLayout

    private lateinit var tvPredictiveInsight: TextView
    private lateinit var tvSmartRecommendation: TextView
    private lateinit var tvDailySpendingLimit: TextView

    private lateinit var btnNavHome: LinearLayout
    private lateinit var btnNavCategory: LinearLayout
    private lateinit var btnNavAdd: LinearLayout
    private lateinit var btnNavHistory: LinearLayout
    private lateinit var btnNavBudget: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_analytics_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().reference

        initializeViews()
        setupBottomNavigation()
        loadCompleteFinancialData()
    }

    private fun initializeViews() {
        tvTotalSpentAnalytics = findViewById(R.id.tvTotalSpentAnalytics)
        tvTotalIncomeAnalytics = findViewById(R.id.tvTotalIncomeAnalytics)
        tvTransactionCount = findViewById(R.id.tvTransactionCount)

        tvBadgeIcon = findViewById(R.id.tvBadgeIcon)
        tvBadgeTitle = findViewById(R.id.tvBadgeTitle)
        tvBadgeDesc = findViewById(R.id.tvBadgeDesc)

        spinnerPieMonth = findViewById(R.id.spinnerPieMonth)
        spinnerBarMonth = findViewById(R.id.spinnerBarMonth)

        tvPieMinGoal = findViewById(R.id.tvPieMinGoal)
        tvPieMaxGoal = findViewById(R.id.tvPieMaxGoal)
        pieChartView = findViewById(R.id.pieChartView)
        llLegend = findViewById(R.id.llLegend)
        barChartView = findViewById(R.id.barChartView)
        llBarLegend = findViewById(R.id.llBarLegend)

        tvPredictiveInsight = findViewById(R.id.tvPredictiveInsight)
        tvSmartRecommendation = findViewById(R.id.tvSmartRecommendation)
        tvDailySpendingLimit = findViewById(R.id.tvDailySpendingLimit)

        btnNavHome = findViewById(R.id.btnNavHome)
        btnNavCategory = findViewById(R.id.btnNavCategory)
        btnNavAdd = findViewById(R.id.btnNavAdd)
        btnNavHistory = findViewById(R.id.btnNavHistory)
        btnNavBudget = findViewById(R.id.btnNavBudget)
    }

    private fun populateFilterSpinnerOptions() {
        val sortKeyFormatter = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val displayNameFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        val uniqueMonthKeysSet = mutableSetOf<String>()

        for (txn in transactionsList) {
            val txDate = parseTransactionDate(txn.transaction_date)
            uniqueMonthKeysSet.add(sortKeyFormatter.format(txDate))
        }

        val sortedKeysList = uniqueMonthKeysSet.toList().sortedDescending()

        availableMonthsList.clear()
        monthNameToSortKeyMap.clear()

        // Inject the explicit global "All Months" tracking wildcard
        availableMonthsList.add("All Months")
        monthNameToSortKeyMap["All Months"] = "ALL_TIME"

        for (key in sortedKeysList) {
            try {
                val parsedDate = sortKeyFormatter.parse(key)
                val displayString = displayNameFormatter.format(parsedDate ?: Date())
                availableMonthsList.add(displayString)
                monthNameToSortKeyMap[displayString] = key
            } catch (e: Exception) {
                availableMonthsList.add(key)
                monthNameToSortKeyMap[key] = key
            }
        }

        val adapterPie = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, availableMonthsList)
        val adapterBar = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, availableMonthsList)

        spinnerPieMonth.adapter = adapterPie
        spinnerBarMonth.adapter = adapterBar

        // Start out viewing everything comprehensively across logs
        spinnerPieMonth.setSelection(0)
        spinnerBarMonth.setSelection(0)

        selectedPieMonthSortKey = "ALL_TIME"
        selectedBarMonthSortKey = "ALL_TIME"

        // Independent Pie Spinner Execution
        spinnerPieMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedPieMonthSortKey = monthNameToSortKeyMap[availableMonthsList[position]] ?: "ALL_TIME"
                processAndRenderAnalytics()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Independent Bar Spinner Execution
        spinnerBarMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedBarMonthSortKey = monthNameToSortKeyMap[availableMonthsList[position]] ?: "ALL_TIME"
                processAndRenderAnalytics()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadCompleteFinancialData() {
        val uid = auth.currentUser?.uid ?: return

        db.child("goals").child(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                minTargetGoal = snapshot.child("min_goal").getValue(Double::class.java) ?: 0.0
                maxBudgetGoal = snapshot.child("max_goal").getValue(Double::class.java) ?: 0.0
                tvPieMinGoal.text = "Min Target: ZAR %.2f".format(minTargetGoal)
                tvPieMaxGoal.text = "Max Budget: ZAR %.2f".format(maxBudgetGoal)
                processAndRenderAnalytics()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        db.child("budgets").child(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(budgetSnapshot: DataSnapshot) {
                categoryBudgetsMap.clear()
                for (child in budgetSnapshot.children) {
                    val budgetItem = child.getValue(Budget::class.java) ?: continue
                    if (budgetItem.category != null) {
                        categoryBudgetsMap[budgetItem.category.category_id] = budgetItem.budget_limit
                    }
                }
                processAndRenderAnalytics()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        db.child("transactions").child(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(txSnapshot: DataSnapshot) {
                transactionsList.clear()
                for (child in txSnapshot.children) {
                    val txn = child.getValue(Transactions::class.java) ?: continue
                    transactionsList.add(txn)
                }
                populateFilterSpinnerOptions()
                processAndRenderAnalytics()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun parseTransactionDate(dateStr: String?): Date {
        if (dateStr.isNullOrEmpty()) return Date()
        val structuralPatterns = listOf("yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy", "yyyy/MM/dd", "MM/dd/yyyy")
        for (pattern in structuralPatterns) {
            try {
                val formatter = SimpleDateFormat(pattern, Locale.getDefault())
                formatter.isLenient = false
                return formatter.parse(dateStr) ?: continue
            } catch (e: Exception) {}
        }
        return Date()
    }

    private fun processAndRenderAnalytics() {
        if (selectedPieMonthSortKey.isEmpty() || selectedBarMonthSortKey.isEmpty()) return

        val sortKeyFormatter = SimpleDateFormat("yyyy-MM", Locale.getDefault())

        // --- 1. EVALUATE PIE CHART DATA STRUCTURES ---
        var totalSpentPieMonth = 0.0
        val categoryTotalsMap = mutableMapOf<String, Double>()
        val categoryIdToNameMap = mutableMapOf<String, String>()
        val categorySpentByIdMap = mutableMapOf<String, Double>()

        for (txn in transactionsList) {
            val transactionDate = parseTransactionDate(txn.transaction_date)
            val txnMonthKey = sortKeyFormatter.format(transactionDate)

            if (selectedPieMonthSortKey == "ALL_TIME" || txnMonthKey == selectedPieMonthSortKey) {
                if (txn.transaction_type == "Expense") {
                    totalSpentPieMonth += txn.transaction_amount
                    val catId = txn.category?.category_id ?: "unassigned"
                    val catName = txn.category?.category_name ?: "Unassigned"

                    categoryTotalsMap[catName] = categoryTotalsMap.getOrDefault(catName, 0.0) + txn.transaction_amount
                    categorySpentByIdMap[catId] = categorySpentByIdMap.getOrDefault(catId, 0.0) + txn.transaction_amount
                    categoryIdToNameMap[catId] = catName
                }
            }
        }

        // --- 2. EVALUATE BAR CHART DATA STRUCTURES ---
        var totalBarIncome = 0.0
        var totalBarExpense = 0.0
        var barTransactionCount = 0

        for (txn in transactionsList) {
            val transactionDate = parseTransactionDate(txn.transaction_date)
            val txnMonthKey = sortKeyFormatter.format(transactionDate)

            if (selectedBarMonthSortKey == "ALL_TIME" || txnMonthKey == selectedBarMonthSortKey) {
                barTransactionCount++
                if (txn.transaction_type == "Income") {
                    totalBarIncome += txn.transaction_amount
                } else if (txn.transaction_type == "Expense") {
                    totalBarExpense += txn.transaction_amount
                }
            }
        }

        // Bind information views
        tvTotalSpentAnalytics.text = "ZAR %.2f".format(totalBarExpense)
        tvTotalIncomeAnalytics.text = "ZAR %.2f".format(totalBarIncome)
        tvTransactionCount.text = "$barTransactionCount transactions"

        evaluateGamificationStatus(barTransactionCount, totalBarExpense)
        calculateDeepInsights(totalBarExpense, categorySpentByIdMap, categoryIdToNameMap)

        renderPieChartLayer(categoryTotalsMap, categoryIdToNameMap)
        renderBarChartLayer(totalBarIncome, totalBarExpense)
    }

    private fun calculateDeepInsights(totalSpent: Double, categorySpent: Map<String, Double>, categoryNames: Map<String, String>) {
        if (maxBudgetGoal <= 0) {
            tvPredictiveInsight.text = "Configure maximum goals in Budget tab to enable tracking."
            tvSmartRecommendation.text = "No metrics available."
            tvDailySpendingLimit.text = "Guidance paused."
            return
        }

        if (selectedBarMonthSortKey == "ALL_TIME") {
            tvPredictiveInsight.text = "📊 Cumulative View: Tracking lifetime historical performance metrics summary directly."
            tvPredictiveInsight.setTextColor(Color.parseColor("#1E442F"))
            tvSmartRecommendation.text = "💡 Advice: Filter down to a specific month for localized run-rate insight generation."
            tvDailySpendingLimit.text = "🎯 Daily target guidelines are paused during full lifecycle tracking modes."
            return
        }

        val currentMonthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val daysInMonthAnalysis = if (selectedBarMonthSortKey == currentMonthKey) {
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        } else {
            30
        }

        val dailyBurnRate = totalSpent / daysInMonthAnalysis
        val projectedExpenses = dailyBurnRate * 30

        if (projectedExpenses > maxBudgetGoal) {
            tvPredictiveInsight.text = "⚠️ High Burn Velocity: Projected to reach ZAR %.2f, breaching limits!".format(projectedExpenses)
            tvPredictiveInsight.setTextColor(Color.parseColor("#EB2718"))
        } else {
            tvPredictiveInsight.text = "✅ Balanced Pace: Trajectory tracking around ZAR %.2f, safely inside thresholds.".format(projectedExpenses)
            tvPredictiveInsight.setTextColor(Color.parseColor("#1E442F"))
        }

        var overBudgetCategoryMessage = ""
        for ((catId, spent) in categorySpent) {
            val limit = categoryBudgetsMap[catId] ?: 0.0
            if (limit > 0.0 && spent > limit) {
                val name = categoryNames[catId] ?: "Category"
                overBudgetCategoryMessage += "$name (Over by ZAR %.2f). ".format(spent - limit)
            }
        }

        val remainingBudget = maxBudgetGoal - totalSpent
        if (remainingBudget > 0) {
            val daysLeft = (30 - daysInMonthAnalysis).coerceAtLeast(1)
            val baselineDailyCeiling = remainingBudget / daysLeft

            if (overBudgetCategoryMessage.isNotEmpty()) {
                tvSmartRecommendation.text = "💡 Warning: $overBudgetCategoryMessage Freeze outlays here to stabilize balances."
            } else {
                tvSmartRecommendation.text = "💡 Good Management: All custom categories are running within assigned constraints."
            }
            tvDailySpendingLimit.text = "🎯 Suggested Safety Cap: Keep outlays below ZAR %.2f/day for the next $daysLeft days.".format(baselineDailyCeiling)
        } else {
            tvSmartRecommendation.text = "🚨 Critical: Absolute monthly budget ceiling breached. Halt non-essential outlays."
            tvDailySpendingLimit.text = "🎯 Suggested Safety Cap: ZAR 0.00 / day (Limit Blown)"
        }
    }

    private fun renderPieChartLayer(categoryData: Map<String, Double>, categoryIdToNameMap: Map<String, String>) {
        val colorPalette = listOf("#1E442F", "#4CAF50", "#FF9800", "#E91E63", "#9C27B0", "#03A9F4", "#E53935", "#00ACC1")
        val pieSlicesList = arrayListOf<PieChartView.Slice>()

        llLegend.removeAllViews()
        var colorIndex = 0

        val nameToIdMap = categoryIdToNameMap.entries.associate { it.value to it.key }

        for ((categoryName, accumulatedAmount) in categoryData) {
            val colorStr = colorPalette[colorIndex % colorPalette.size]
            val colorInt = Color.parseColor(colorStr)

            pieSlicesList.add(PieChartView.Slice(accumulatedAmount.toFloat(), colorInt))

            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 8, 0, 8) }
            }

            val colorIndicator = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(28, 28).apply { setMargins(4, 0, 16, 0) }
                setBackgroundColor(colorInt)
            }

            val targetCatId = nameToIdMap[categoryName] ?: "unassigned"
            val budgetLimitVal = categoryBudgetsMap[targetCatId] ?: 0.0

            val textLabel = TextView(this).apply {
                text = if (budgetLimitVal > 0.0 && selectedPieMonthSortKey != "ALL_TIME") {
                    "$categoryName — ZAR %.2f (Limit: ZAR %.2f)".format(accumulatedAmount, budgetLimitVal)
                } else {
                    "$categoryName — ZAR %.2f".format(accumulatedAmount)
                }
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTextColor(Color.parseColor("#333333"))
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            rowLayout.addView(colorIndicator)
            rowLayout.addView(textLabel)
            llLegend.addView(rowLayout)

            colorIndex++
        }

        pieChartView.setSlices(pieSlicesList)
        pieChartView.setCenterLabel(if (selectedPieMonthSortKey == "ALL_TIME") "All" else "Month")
    }

    private fun renderBarChartLayer(monthTotalIncome: Double, monthTotalExpense: Double) {
        val displayLabelFormatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val sortKeyFormatter = SimpleDateFormat("yyyy-MM", Locale.getDefault())

        val localizedMonthLabel = if (selectedBarMonthSortKey == "ALL_TIME") {
            "All Months"
        } else {
            var label = selectedBarMonthSortKey
            try {
                val parsedDate = sortKeyFormatter.parse(selectedBarMonthSortKey)
                if (parsedDate != null) {
                    label = displayLabelFormatter.format(parsedDate)
                }
            } catch (e: Exception) {}
            label
        }

        val multiValueBarsList = arrayListOf<BarChartView.Bar>()
        multiValueBarsList.add(
            BarChartView.Bar(
                label = localizedMonthLabel,
                incomeAmount = monthTotalIncome.toFloat(),
                expenseAmount = monthTotalExpense.toFloat()
            )
        )
        barChartView.setBars(multiValueBarsList)

        llBarLegend.removeAllViews()

        val monthRowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 4, 0, 4) }
        }

        val tvMonthHeader = TextView(this).apply {
            text = localizedMonthLabel
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.parseColor("#1E442F"))
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val tvFinancialBreakdown = TextView(this).apply {
            text = "Income: ZAR %.2f   |   Expenses: ZAR %.2f".format(monthTotalIncome, monthTotalExpense)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(Color.parseColor("#555555"))
            setPadding(0, 4, 0, 0)
        }

        monthRowLayout.addView(tvMonthHeader)
        monthRowLayout.addView(tvFinancialBreakdown)
        llBarLegend.addView(monthRowLayout)
    }

    private fun evaluateGamificationStatus(totalCount: Int, amountSpentSum: Double) {
        if (totalCount == 0) {
            tvBadgeIcon.text = "🏅"
            tvBadgeTitle.text = "No Badge Yet"
            tvBadgeTitle.setTextColor(Color.parseColor("#888888"))
            tvBadgeDesc.text = "Add your first transaction to earn your Bronze badge!"
            return
        }

        if (maxBudgetGoal > 0.0 && amountSpentSum > maxBudgetGoal) {
            tvBadgeIcon.text = "⚠️"
            tvBadgeTitle.text = "Budget Limit Exceeded"
            tvBadgeTitle.setTextColor(Color.parseColor("#EB2718"))
            tvBadgeDesc.text = "You have exceeded your assigned target budget goals!"
            return
        }

        val spendPercentage = if (maxBudgetGoal > 0.0) (amountSpentSum / maxBudgetGoal) * 100.0 else 0.0

        when {
            spendPercentage < 40.0 -> {
                tvBadgeIcon.text = "🏆"
                tvBadgeTitle.text = "Gold Wealth Master"
                tvBadgeTitle.setTextColor(Color.parseColor("#FFD700"))
                tvBadgeDesc.text = "Phenomenal! Your active spend run rate is safely optimized below limits."
            }
            spendPercentage in 40.0..85.0 -> {
                tvBadgeIcon.text = "🥈"
                tvBadgeTitle.text = "Silver Saver"
                tvBadgeTitle.setTextColor(Color.parseColor("#C0C0C0"))
                tvBadgeDesc.text = "Great job! Spending is running comfortably within threshold bands."
            }
            else -> {
                tvBadgeIcon.text = "🥉"
                tvBadgeTitle.text = "Bronze Warning"
                tvBadgeTitle.setTextColor(Color.parseColor("#CD7F32"))
                tvBadgeDesc.text = "Caution: Asset outlays are reaching top threshold parameters. Monitor balances."
            }
        }
    }

    private fun setupBottomNavigation() {
        btnNavHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        btnNavCategory.setOnClickListener {
            startActivity(Intent(this, AddCategoryActivity::class.java))
            finish()
        }
        btnNavAdd.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
        btnNavBudget.setOnClickListener {
            startActivity(Intent(this, BudgetActivity::class.java))
            finish()
        }
        btnNavHistory.setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}