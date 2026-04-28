package com.example.budgettracker

import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.budgettracker.model.Category
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AddCategoryActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var etCategoryName: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_category)

        auth = FirebaseAuth.getInstance()
        etCategoryName = findViewById(R.id.etCategoryName)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSaveCategory).setOnClickListener { saveCategory() }
    }

    private fun saveCategory() {
        val name = etCategoryName.text.toString().trim()

        if (name.isEmpty()) {
            etCategoryName.error = "Category name is required"
            return
        }

        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Error: not logged in", Toast.LENGTH_LONG).show()
            return
        }

        val db = FirebaseDatabase.getInstance().reference
        val catId = db.child("categories").child(uid).push().key
        if (catId == null) {
            Toast.makeText(this, "Error: could not connect to database", Toast.LENGTH_LONG).show()
            return
        }

        val category = Category(catId, name, "", uid)

        db.child("categories").child(uid).child(catId).setValue(category)
            .addOnSuccessListener {
                Toast.makeText(this, "'$name' saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}