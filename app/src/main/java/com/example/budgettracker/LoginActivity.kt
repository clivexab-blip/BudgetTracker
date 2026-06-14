package com.example.budgettracker

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        emailEditText = findViewById(R.id.loginEmail)
        passwordEditText = findViewById(R.id.passwordLogin)
    }

    fun goToSignUp(view: View) {
        startActivity(Intent(this, RegisterActivity::class.java))
    }

    fun login(view: View) {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            return
        }
        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            return
        }
        loginUser(email, password)
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    val message = when {
                        task.exception?.message?.contains("password") == true -> "Incorrect password. Please try again."
                        task.exception?.message?.contains("no user") == true -> "No account found with this email."
                        task.exception?.message?.contains("blocked") == true -> "Too many attempts. Try again later."
                        else -> "Login failed. Please check your email or password."
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
    }
}

