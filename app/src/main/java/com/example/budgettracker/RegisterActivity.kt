package com.example.budgettracker

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.budgettracker.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        nameEditText = findViewById(R.id.registerName)
        emailEditText = findViewById(R.id.registerEmail)
        passwordEditText = findViewById(R.id.registerPassword)
        confirmPasswordEditText = findViewById(R.id.ConfirmPassword)
    }

    fun goToSignIn(view: View) {
        startActivity(Intent(this, LoginActivity::class.java))
    }

    fun registerUser(view: View) {
        val name = nameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()

        if (name.isEmpty()) { nameEditText.error = "Name is required"; return }
        if (email.isEmpty()) { emailEditText.error = "Email is required"; return }
        if (password.isEmpty()) { passwordEditText.error = "Password is required"; return }
        if (password != confirmPassword) {
            confirmPasswordEditText.error = "Passwords do not match"; return
        }
        if (password.length < 6) {
            passwordEditText.error = "Password must be at least 6 characters"; return
        }
        addUser(name, email, password)
    }

    private fun addUser(name: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser!!.uid
                    val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                    val user = User(uid, name, email, 0.0, dateStr, "")
                    FirebaseDatabase.getInstance().getReference("users").child(uid)
                        .setValue(user)
                        .addOnCompleteListener {
                            Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                } else {
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}
