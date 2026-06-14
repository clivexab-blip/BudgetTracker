package com.example.budgettracker

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budgettracker.adapter.TransactionAdapter
import com.example.budgettracker.model.Transactions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class TransactionHistoryActivity : AppCompatActivity() {
    private lateinit var rvTransaction: RecyclerView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: DatabaseReference

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_transaction_history)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().reference

        rvTransaction = findViewById(R.id.rvTransaction)
        rvTransaction.layoutManager = LinearLayoutManager(this)



        loadAllTransactions()
        setupBottomNav()
    }
    private fun setupBottomNav() {
        findViewById<LinearLayout>(R.id.btnNavHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
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
        findViewById<LinearLayout>(R.id.btnNavAnalytics).setOnClickListener {
            startActivity(Intent(this, AnalyticsDashboardActivity::class.java))
        }

    }
    private fun loadAllTransactions() {
        val uid = auth.currentUser?.uid ?: return
        db.child("transactions").child(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<Transactions>()
                    for (child in snapshot.children) {
                        val t = child.getValue(Transactions::class.java) ?: continue
                        list.add(t)
                    }
                    list.sortByDescending { it.transaction_date }
                    rvTransaction.adapter = TransactionAdapter(list)
                    findViewById<TextView>(R.id.tvTransactionCount).text =
                        "${list.size} transactions"
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
