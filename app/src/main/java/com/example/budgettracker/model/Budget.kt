package com.example.budgettracker.model

data class Budget(
    val budget_id: String = "",
    val user_id: String = "",
    val category: Category = Category(),
    val budget_limit: Double = 0.0,
    val start_date: String = "",
    val end_date: String = "",
    val objective: String = ""
)
