package com.example.budgettracker.model

data class Transactions(
    val transaction_id: String = "",
    val transaction_name: String = "",
    val transaction_date: String = "",
    val transaction_type: String = "",
    val transaction_amount: Double = 0.0,
    val user_id: String = "",
    val category: Category = Category(),
    val receipt: String = ""
)
