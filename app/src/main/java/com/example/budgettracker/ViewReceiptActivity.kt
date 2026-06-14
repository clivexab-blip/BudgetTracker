package com.example.budgettracker

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class ViewReceiptActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_view_receipt)

        val receiptUrl      = intent.getStringExtra("receipt_url") ?: ""
        val transactionName = intent.getStringExtra("transaction_name") ?: ""
        val transactionDate = intent.getStringExtra("transaction_date") ?: ""
        val transactionType = intent.getStringExtra("transaction_type") ?: ""
        val categoryName    = intent.getStringExtra("category_name") ?: ""
        val amount          = intent.getDoubleExtra("transaction_amount", 0.0)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvReceiptTitle).text = transactionName
        findViewById<TextView>(R.id.tvReceiptDate).text  = transactionDate
        findViewById<TextView>(R.id.tvReceiptCat).text   = categoryName

        val isIncome  = transactionType == "Income"
        val amountTv  = findViewById<TextView>(R.id.tvReceiptAmount)
        amountTv.text = (if (isIncome) "+ZAR" else "-ZAR") + String.format("%.2f", amount)
        amountTv.setTextColor(
            getColor(if (isIncome) android.R.color.holo_green_dark
            else          android.R.color.holo_red_dark)
        )

        if (receiptUrl.isNotEmpty()) {
            Glide.with(this)
                .load(receiptUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(findViewById(R.id.ivFullReceipt))
        }

    }

}