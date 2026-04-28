package com.example.budgettracker

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

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSaveGoals).setOnClickListener { saveGoals() }
        findViewById<Button>(R.id.btnSaveBudget).setOnClickListener { saveCategoryBudget() }

        loadGoals()
        loadCategories()
        loadCategorySpending()
    }

    private fun loadGoals() {
        val uid = auth.currentUser?.uid ?: return
        db.child("goals").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val min = snapshot.child("min_goal").getValue(Double::class.java) ?: 0.0
                    val max = snapshot.child("max_goal").getValue(Double::class.java) ?: 0.0
                    if (min > 0) etMinGoal.setText(String.format("%.2f", min))
                    if (max > 0) etMaxGoal.setText(String.format("%.2f", max))
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun saveGoals() {
        val uid = auth.currentUser?.uid ?: return
        val min = etMinGoal.text.toString().trim().toDoubleOrNull()
        val max = etMaxGoal.text.toString().trim().toDoubleOrNull()

        if (min == null || min < 0) { etMinGoal.error = "Enter valid amount"; return }
        if (max == null || max < 0) { etMaxGoal.error = "Enter valid amount"; return }
        if (max < min)              { etMaxGoal.error = "Max must be greater than min"; return }

        db.child("goals").child(uid)
            .setValue(mapOf("min_goal" to min, "max_goal" to max))
            .addOnSuccessListener {
                Toast.makeText(this, "Goals saved!", Toast.LENGTH_SHORT).show()
                loadCategorySpending()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadCategories() {
        val uid = auth.currentUser?.uid ?: return
        db.child("categories").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    categoryList.clear(); categoryNames.clear()
                    for (child in snapshot.children) {
                        val cat = child.getValue(Category::class.java) ?: continue
                        categoryList.add(cat); categoryNames.add(cat.category_name)
                    }
                    if (categoryNames.isEmpty()) categoryNames.add("No categories yet")
                    spinnerBudgetCategory.adapter = ArrayAdapter(
                        this@BudgetActivity,
                        android.R.layout.simple_spinner_dropdown_item, categoryNames)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
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
                loadCategorySpending()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadCategorySpending() {
        val uid = auth.currentUser?.uid ?: return
        db.child("goals").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(goalsSnap: DataSnapshot) {
                    val minGoal = goalsSnap.child("min_goal").getValue(Double::class.java) ?: 0.0
                    val maxGoal = goalsSnap.child("max_goal").getValue(Double::class.java) ?: 0.0

                    db.child("transactions").child(uid)
                        .addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(txSnap: DataSnapshot) {
                                val spendMap   = mutableMapOf<String, Double>()
                                val catNames   = mutableMapOf<String, String>()
                                var totalSpent = 0.0

                                for (child in txSnap.children) {
                                    val t = child.getValue(Transactions::class.java) ?: continue
                                    if (t.transaction_type == "Expense") {
                                        val id       = t.category.category_id
                                        spendMap[id] = (spendMap[id] ?: 0.0) + t.transaction_amount
                                        catNames[id] = t.category.category_name
                                        totalSpent  += t.transaction_amount
                                    }
                                }

                                tvTotalSpent.text = "ZAR ${String.format("%.2f", totalSpent)}"

                                when {
                                    maxGoal > 0 && totalSpent > maxGoal -> {
                                        tvGoalStatus.text = "⚠️ Over maximum goal!"
                                        tvGoalStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                                        progressOverall.setIndicatorColor(getColor(android.R.color.holo_red_dark))
                                    }
                                    minGoal > 0 && totalSpent < minGoal -> {
                                        tvGoalStatus.text = "✅ Under minimum — keep saving!"
                                        tvGoalStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                                        progressOverall.setIndicatorColor(getColor(android.R.color.holo_green_light))
                                    }
                                    maxGoal > 0 -> {
                                        tvGoalStatus.text = "👍 Within budget"
                                        tvGoalStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                                        progressOverall.setIndicatorColor(getColor(android.R.color.holo_green_light))
                                    }
                                    else -> {
                                        tvGoalStatus.text = "Set goals above to track progress"
                                        tvGoalStatus.setTextColor(getColor(android.R.color.darker_gray))
                                    }
                                }

                                progressOverall.progress = if (maxGoal > 0)
                                    ((totalSpent / maxGoal) * 100).toInt().coerceIn(0, 100)
                                else 0

                                db.child("budgets").child(uid)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(budgetSnap: DataSnapshot) {
                                            val budgetMap = mutableMapOf<String, Double>()
                                            for (b in budgetSnap.children) {
                                                val bud = b.getValue(Budget::class.java) ?: continue
                                                budgetMap[bud.category.category_id] = bud.budget_limit
                                            }
                                            renderCategoryBreakdown(spendMap, catNames, budgetMap)
                                        }
                                        override fun onCancelled(error: DatabaseError) {
                                            renderCategoryBreakdown(spendMap, catNames, emptyMap())
                                        }
                                    })
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
                override fun onCancelled(error: DatabaseError) {}
            })
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
            val pct     = if (limit > 0) ((spent / limit) * 100).toInt().coerceIn(0, 100) else -1

            val row = LayoutInflater.from(this)
                .inflate(R.layout.item_budget_category, llCategoryBreakdown, false)

            row.findViewById<TextView>(R.id.tvCatName).text  = catName
            row.findViewById<TextView>(R.id.tvCatSpent).text =
                "ZAR ${String.format("%.2f", spent)}" +
                        if (limit > 0) " / ZAR ${String.format("%.2f", limit)}" else " (no limit set)"

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
}
