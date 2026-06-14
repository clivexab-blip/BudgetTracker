package com.example.budgettracker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.budgettracker.model.Budget
import com.example.budgettracker.model.Category
import com.example.budgettracker.model.Transactions
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class BudgetActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: DatabaseReference

    private lateinit var etMinGoal: EditText
    private lateinit var etMaxGoal: EditText
    private lateinit var tvTotalSpent: TextView
    private lateinit var tvGoalStatus: TextView
    private lateinit var progressOverall: LinearProgressIndicator
    private lateinit var llCategoryBreakdown: LinearLayout
    private lateinit var spinnerBudgetCategory: Spinner
    private lateinit var etBudgetLimit: EditText

    private val categoryList  = mutableListOf<Category>()
    private val categoryNames = mutableListOf<String>()

    // Local state cache — decouples reading cycles to prevent data-bleeding
    private var cachedMinGoal = 0.0
    private var cachedMaxGoal = 0.0
    private val cachedSpendMap    = mutableMapOf<String, Double>()
    private val cachedCatNamesMap = mutableMapOf<String, String>()
    private val cachedBudgetMap   = mutableMapOf<String, Double>()

    // Track whether the user is currently editing goals so live updates
    // don't overwrite their in-progress typing
    private var userEditingGoals = false

    // Keep references so we can remove listeners in onDestroy
    private var goalsListener: ValueEventListener? = null
    private var categoriesListener: ValueEventListener? = null
    private var transactionsListener: ValueEventListener? = null
    private var budgetsListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_budget)

        auth = FirebaseAuth.getInstance()
        db   = FirebaseDatabase.getInstance().reference

        etMinGoal             = findViewById(R.id.etMinGoal)
        etMaxGoal             = findViewById(R.id.etMaxGoal)
        tvTotalSpent          = findViewById(R.id.tvTotalSpent)
        tvGoalStatus          = findViewById(R.id.tvGoalStatus)
        progressOverall       = findViewById(R.id.progressOverall)
        llCategoryBreakdown   = findViewById(R.id.llCategoryBreakdown)
        spinnerBudgetCategory = findViewById(R.id.spinnerBudgetCategory)
        etBudgetLimit         = findViewById(R.id.etBudgetLimit)

        // Mark when user starts typing so live listener won't overwrite their input
        etMinGoal.setOnFocusChangeListener { _, hasFocus -> userEditingGoals = hasFocus }
        etMaxGoal.setOnFocusChangeListener { _, hasFocus -> userEditingGoals = hasFocus }

        findViewById<Button>(R.id.btnSaveGoals).setOnClickListener { saveGoals() }
        findViewById<Button>(R.id.btnSaveBudget).setOnClickListener { saveCategoryBudget() }

        setupLiveGoalsListener()      // FIX 1: replaces one-shot loadGoals()
        setupLiveCategoriesListener() // FIX 2: replaces one-shot loadCategories()
        setupFlatFinancialListeners()
        setupBottomNav()
    }

    override fun onDestroy() {
        super.onDestroy()
        val uid = auth.currentUser?.uid ?: return
        // Clean up all persistent listeners to avoid memory leaks
        goalsListener?.let       { db.child("goals").child(uid).removeEventListener(it) }
        categoriesListener?.let  { db.child("categories").child(uid).removeEventListener(it) }
        transactionsListener?.let{ db.child("transactions").child(uid).removeEventListener(it) }
        budgetsListener?.let     { db.child("budgets").child(uid).removeEventListener(it) }
    }

    // ─── FIX 1: Live goals listener ──────────────────────────────────────────
    // Previously loadGoals() used addListenerForSingleValueEvent — it only fired
    // once at startup. If the user saved new goals from another device or session,
    // the fields would be stale. Now the fields always mirror Firebase in real time,
    // but only update when the user is NOT actively typing.
    private fun setupLiveGoalsListener() {
        val uid = auth.currentUser?.uid ?: return
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val min = snapshot.child("min_goal").getValue(Double::class.java) ?: 0.0
                val max = snapshot.child("max_goal").getValue(Double::class.java) ?: 0.0

                // Don't overwrite what the user is currently typing
                if (!userEditingGoals) {
                    etMinGoal.setText(if (min > 0) String.format("%.2f", min) else "")
                    etMaxGoal.setText(if (max > 0) String.format("%.2f", max) else "")
                }

                // Always update the cached values used by the summary card
                cachedMinGoal = min
                cachedMaxGoal = max
                combineAndRenderUI()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("goals").child(uid).addValueEventListener(listener)
        goalsListener = listener
    }

    private fun saveGoals() {
        val uid = auth.currentUser?.uid ?: return
        val min = etMinGoal.text.toString().trim().toDoubleOrNull()
        val max = etMaxGoal.text.toString().trim().toDoubleOrNull()

        if (min == null || min < 0) { etMinGoal.error = "Enter valid amount"; return }
        if (max == null || max < 0) { etMaxGoal.error = "Enter valid amount"; return }
        if (max < min)              { etMaxGoal.error = "Max must be greater than min"; return }

        // Clear focus so live listener can update the fields once confirmed
        etMinGoal.clearFocus()
        etMaxGoal.clearFocus()
        userEditingGoals = false

        db.child("goals").child(uid)
            .setValue(mapOf("min_goal" to min, "max_goal" to max))
            .addOnSuccessListener {
                Toast.makeText(this, "Goals saved!", Toast.LENGTH_SHORT).show()
                // Live listener (setupLiveGoalsListener) will automatically
                // re-populate the fields with exactly what Firebase stored
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ─── FIX 2: Live categories listener ────────────────────────────────────
    // Previously loadCategories() used addListenerForSingleValueEvent — categories
    // added in AddCategoryActivity wouldn't appear in the spinner until a full
    // restart. Now the spinner refreshes automatically whenever categories change.
    private fun setupLiveCategoriesListener() {
        val uid = auth.currentUser?.uid ?: return
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val selectedName = if (categoryList.isNotEmpty() &&
                    spinnerBudgetCategory.selectedItemPosition < categoryList.size)
                    categoryList[spinnerBudgetCategory.selectedItemPosition].category_name
                else null

                categoryList.clear()
                categoryNames.clear()

                for (child in snapshot.children) {
                    val cat = child.getValue(Category::class.java) ?: continue
                    categoryList.add(cat)
                    categoryNames.add(cat.category_name)
                }

                if (categoryNames.isEmpty()) categoryNames.add("No categories yet")

                val adapter = ArrayAdapter(
                    this@BudgetActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    categoryNames
                )
                spinnerBudgetCategory.adapter = adapter

                // Restore previously selected category if it still exists
                if (selectedName != null) {
                    val idx = categoryNames.indexOf(selectedName)
                    if (idx >= 0) spinnerBudgetCategory.setSelection(idx)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("categories").child(uid).addValueEventListener(listener)
        categoriesListener = listener
    }

    private fun saveCategoryBudget() {
        val uid   = auth.currentUser?.uid ?: return
        val limit = etBudgetLimit.text.toString().trim().toDoubleOrNull()

        if (limit == null || limit <= 0) { etBudgetLimit.error = "Enter valid limit"; return }
        if (categoryList.isEmpty()) {
            Toast.makeText(this, "Please create a category first", Toast.LENGTH_SHORT).show()
            return
        }

        val cat      = categoryList[spinnerBudgetCategory.selectedItemPosition]
        val budgetId = db.child("budgets").child(uid).push().key ?: return
        val budget   = Budget(budgetId, uid, cat, limit, "", "", "")

        db.child("budgets").child(uid).child(cat.category_id).setValue(budget)
            .addOnSuccessListener {
                Toast.makeText(this, "Budget set for ${cat.category_name}!", Toast.LENGTH_SHORT).show()
                etBudgetLimit.text.clear()
                // Live budgets listener (Pipeline 3) will immediately reflect
                // the new limit in the category breakdown without any extra call
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ─── Three decoupled flat pipelines ──────────────────────────────────────
    // Goals pipeline is now handled by setupLiveGoalsListener() above so that
    // the edit fields and the summary card both stay in sync from one source.
    private fun setupFlatFinancialListeners() {
        val uid = auth.currentUser?.uid ?: return

        // Pipeline 2: Ledger Expenses Record Aggregator
        val txListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cachedSpendMap.clear()
                cachedCatNamesMap.clear()

                for (child in snapshot.children) {
                    val t = child.getValue(Transactions::class.java) ?: continue
                    if (t.transaction_type == "Expense" && t.category != null) {
                        val id = t.category.category_id
                        cachedSpendMap[id]    = (cachedSpendMap[id] ?: 0.0) + t.transaction_amount
                        cachedCatNamesMap[id] = t.category.category_name
                    }
                }
                combineAndRenderUI()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("transactions").child(uid).addValueEventListener(txListener)
        transactionsListener = txListener

        // Pipeline 3: Custom Category Specific Budgets Node
        val budgetListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cachedBudgetMap.clear()
                for (child in snapshot.children) {
                    val bud = child.getValue(Budget::class.java) ?: continue
                    if (bud.category != null) {
                        cachedBudgetMap[bud.category.category_id] = bud.budget_limit
                    }
                }
                combineAndRenderUI()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("budgets").child(uid).addValueEventListener(budgetListener)
        budgetsListener = budgetListener
    }

    // ─── UI rendering ────────────────────────────────────────────────────────
    private fun combineAndRenderUI() {
        val totalSpent = cachedSpendMap.values.sum()

        val goalsSummary = "ZAR ${String.format("%.2f", totalSpent)}\n" +
                "(Min Target: ZAR ${String.format("%.2f", cachedMinGoal)} | " +
                "Max Target: ZAR ${String.format("%.2f", cachedMaxGoal)})"
        tvTotalSpent.text = goalsSummary

        when {
            cachedMaxGoal > 0 && totalSpent > cachedMaxGoal -> {
                tvGoalStatus.text = "⚠️ Over maximum goal!"
                tvGoalStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                progressOverall.setIndicatorColor(getColor(android.R.color.holo_red_dark))
            }
            cachedMinGoal > 0 && totalSpent < cachedMinGoal -> {
                tvGoalStatus.text = "✅ Under minimum — keep saving!"
                tvGoalStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                progressOverall.setIndicatorColor(getColor(android.R.color.holo_green_light))
            }
            cachedMaxGoal > 0 -> {
                tvGoalStatus.text = "👍 Within budget"
                tvGoalStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                progressOverall.setIndicatorColor(getColor(android.R.color.holo_green_light))
            }
            else -> {
                tvGoalStatus.text = "Set goals above to track progress"
                tvGoalStatus.setTextColor(getColor(android.R.color.darker_gray))
            }
        }

        progressOverall.progress = if (cachedMaxGoal > 0)
            ((totalSpent / cachedMaxGoal) * 100).toInt().coerceIn(0, 100)
        else 0

        renderCategoryBreakdown(cachedSpendMap, cachedCatNamesMap, cachedBudgetMap)
    }

    private fun renderCategoryBreakdown(
        spendMap:  Map<String, Double>,
        catNames:  Map<String, String>,
        budgetMap: Map<String, Double>
    ) {
        llCategoryBreakdown.removeAllViews()

        if (spendMap.isEmpty()) {
            val tv = TextView(this)
            tv.text     = "No expenses recorded yet"
            tv.textSize = 14f
            tv.setPadding(0, 16, 0, 16)
            llCategoryBreakdown.addView(tv)
            return
        }

        for ((catId, spent) in spendMap) {
            val catName = catNames[catId] ?: "Unknown"
            val limit   = budgetMap[catId] ?: 0.0
            val pct     = if (limit > 0.0) ((spent / limit) * 100).toInt().coerceIn(0, 100) else -1

            val row = LayoutInflater.from(this)
                .inflate(R.layout.item_budget_category, llCategoryBreakdown, false)

            row.findViewById<TextView>(R.id.tvCatName).text = catName
            row.findViewById<TextView>(R.id.tvCatSpent).text =
                "ZAR ${String.format("%.2f", spent)}" +
                        if (limit > 0.0) " / ZAR ${String.format("%.2f", limit)}" else " (no limit set)"

            val progress = row.findViewById<LinearProgressIndicator>(R.id.progressCat)
            val tvPct    = row.findViewById<TextView>(R.id.tvCatPct)

            if (pct >= 0) {
                progress.progress = pct
                tvPct.text = "$pct%"
                val colour = when {
                    pct >= 100 -> android.R.color.holo_red_dark
                    pct >= 80  -> android.R.color.holo_orange_dark
                    else       -> android.R.color.holo_green_dark
                }
                progress.setIndicatorColor(getColor(colour))
                tvPct.setTextColor(getColor(colour))
            } else {
                progress.progress = 0
                tvPct.text = "--"
                tvPct.setTextColor(getColor(android.R.color.darker_gray))
            }

            llCategoryBreakdown.addView(row)
        }
    }

    private fun setupBottomNav() {
        findViewById<LinearLayout>(R.id.btnNavHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java)); finish()
        }
        findViewById<LinearLayout>(R.id.btnNavAdd).setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnNavCategory).setOnClickListener {
            startActivity(Intent(this, AddCategoryActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnNavHistory).setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnNavAnalytics).setOnClickListener {
            startActivity(Intent(this, AnalyticsDashboardActivity::class.java)); finish()
        }
    }
}