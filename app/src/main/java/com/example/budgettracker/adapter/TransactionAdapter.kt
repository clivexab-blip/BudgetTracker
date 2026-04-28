package com.example.budgettracker.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.budgettracker.R
import com.example.budgettracker.ViewReceiptActivity
import com.example.budgettracker.model.Transactions

class TransactionAdapter(private val transactions: List<Transactions>) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView         = itemView.findViewById(R.id.tvTitle)
        val tvCategory: TextView      = itemView.findViewById(R.id.tvCategory)
        val tvAmount: TextView        = itemView.findViewById(R.id.tvAmount)
        val tvTimestamp: TextView     = itemView.findViewById(R.id.tvTimestamp)
        val ivReceiptThumb: ImageView = itemView.findViewById(R.id.ivReceiptThumb)
        val tvCategoryIcon: TextView  = itemView.findViewById(R.id.tvCategoryIcon)
        val btnViewReceipt: Button    = itemView.findViewById(R.id.btnViewReceipt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.transaction_item, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val t        = transactions[position]
        val isIncome = t.transaction_type == "Income"

        holder.tvTitle.text     = t.transaction_name
        holder.tvCategory.text  = t.category.category_name
        holder.tvTimestamp.text = t.transaction_date
        holder.tvAmount.text    = (if (isIncome) "+ZAR" else "-ZAR") +
                String.format("%.2f", t.transaction_amount)
        holder.tvAmount.setTextColor(
            holder.itemView.context.getColor(
                if (isIncome) android.R.color.holo_green_dark
                else          android.R.color.holo_red_dark
            )
        )

        holder.tvCategoryIcon.text = categoryEmoji(t.category.category_name)

        if (t.receipt.isNotEmpty()) {
            // Has image — show thumbnail, hide emoji, show view button
            holder.ivReceiptThumb.visibility = View.VISIBLE
            holder.tvCategoryIcon.visibility = View.GONE
            holder.btnViewReceipt.visibility = View.VISIBLE

            Glide.with(holder.itemView.context)
                .load(t.receipt)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.ivReceiptThumb)

            val openViewer = View.OnClickListener {
                val intent = Intent(holder.itemView.context, ViewReceiptActivity::class.java).apply {
                    putExtra("receipt_url",         t.receipt)
                    putExtra("transaction_name",    t.transaction_name)
                    putExtra("transaction_amount",  t.transaction_amount)
                    putExtra("transaction_date",    t.transaction_date)
                    putExtra("transaction_type",    t.transaction_type)
                    putExtra("category_name",       t.category.category_name)
                }
                holder.itemView.context.startActivity(intent)
            }
            holder.ivReceiptThumb.setOnClickListener(openViewer)
            holder.btnViewReceipt.setOnClickListener(openViewer)

        } else {
            // No image — show emoji, hide thumbnail and view button
            holder.ivReceiptThumb.visibility = View.GONE
            holder.tvCategoryIcon.visibility = View.VISIBLE
            holder.btnViewReceipt.visibility = View.GONE
        }
    }

    override fun getItemCount() = transactions.size

    private fun categoryEmoji(name: String) = when {
        name.contains("grocer",     true) || name.contains("food",       true) -> "🛒"
        name.contains("transport",  true) || name.contains("fuel",       true) -> "🚗"
        name.contains("dining",     true) || name.contains("restaurant", true) -> "🍽️"
        name.contains("salary",     true) || name.contains("income",     true) -> "💰"
        name.contains("entertain",  true) || name.contains("movie",      true) -> "🎬"
        name.contains("health",     true) || name.contains("medical",    true) -> "🏥"
        name.contains("shop",       true) || name.contains("cloth",      true) -> "🛍️"
        name.contains("util",       true) || name.contains("electric",   true) -> "💡"
        else -> "💳"
    }
}