package com.example.budgettracker.model

data class User(
    val user_id: String = "",
    val name: String = "",
    val email: String = "",
    val balance: Double = 0.0,
    val created_at: String = "",
    val main_goal: String = ""
)
