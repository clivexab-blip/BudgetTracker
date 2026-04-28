package com.example.budgettracker

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budgettracker.adapter.TransactionAdapter
import com.example.budgettracker.model.Transactions
import com.example.budgettracker.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: DatabaseReference
    private lateinit var rvTransaction: RecyclerView
    private lateinit var tvUserName: TextView
    private lateinit var tvGreeting: TextView
    private lateinit var tvTotalBalance: TextView
    private lateinit var tvIncome: TextView
    private lateinit var tvExpenses: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db   = FirebaseDatabase.getInstance().reference

        tvUserName     = findViewById(R.id.tvUserName)
        tvGreeting     = findViewById(R.id.tvGreeting)
        tvTotalBalance = findViewById(R.id.tvTotalBalance)
        tvIncome       = findViewById(R.id.tvIncome)
        tvExpenses     = findViewById(R.id.tvExpenses)
        rvTransaction  = findViewById(R.id.rvTransaction)

        rvTransaction.layoutManager = LinearLayoutManager(this)

        setGreeting()
        loadUserData()
        loadTransactions()

        // View All link
        findViewById<TextView>(R.id.tvViewAll).setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }

        // Logout circle button
        findViewById<TextView>(R.id.tvLogout).setOnClickListener {
            showLogoutDialog()
        }

        // ── Bottom navigation ──────────────────────────────
        findViewById<LinearLayout>(R.id.btnNavHome).setOnClickListener {
            // already on home
        }
        findViewById<LinearLayout>(R.id.btnNavAdd).setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnNavCategory).setOnClickListener {
            startActivity(Intent(this, AddCategoryActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnNavBudget).setOnClickListener {
            startActivity(Intent(this, BudgetActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnNavHistory).setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
        loadTransactions()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log Out") { _, _ ->
                auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        tvGreeting.text = when {
            hour < 12 -> "Good morning,"
            hour < 17 -> "Good afternoon,"
            else      -> "Good evening,"
        }
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return
        db.child("users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    tvUserName.text = snapshot.getValue(User::class.java)?.name ?: "User"
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadTransactions() {
        val uid = auth.currentUser?.uid ?: return
        db.child("transactions").child(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list          = mutableListOf<Transactions>()
                    var totalIncome   = 0.0
                    var totalExpenses = 0.0

                    for (child in snapshot.children) {
                        val t = child.getValue(Transactions::class.java) ?: continue
                        list.add(t)
                        if (t.transaction_type == "Income") totalIncome   += t.transaction_amount
                        else                                totalExpenses += t.transaction_amount
                    }

                    list.sortByDescending { it.transaction_date }
                    rvTransaction.adapter =
                        TransactionAdapter(if (list.size > 5) list.subList(0, 5) else list)

                    tvIncome.text       = "ZAR ${String.format("%.2f", totalIncome)}"
                    tvExpenses.text     = "ZAR ${String.format("%.2f", totalExpenses)}"
                    tvTotalBalance.text = "ZAR ${String.format("%.2f", totalIncome - totalExpenses)}"
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}