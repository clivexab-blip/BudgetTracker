package com.example.budgettracker

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.budgettracker.model.Category
import com.example.budgettracker.model.Transactions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class AddTransactionActivity : AppCompatActivity() {
    private lateinit var db: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var etTransactionName: EditText
    private lateinit var etAmount: EditText
    private lateinit var etDate: EditText
    private lateinit var spinnerType: Spinner
    private lateinit var spinnerCategory: Spinner
    private lateinit var ivReceiptPreview: ImageView
    private lateinit var tvImageStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSave: Button

    private val categoryList  = mutableListOf<Category>()
    private val categoryNames = mutableListOf<String>()
    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            ivReceiptPreview.setImageURI(uri)
            ivReceiptPreview.visibility = View.VISIBLE
            tvImageStatus.text = "✓ Image selected"
            tvImageStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_transaction)

        auth = FirebaseAuth.getInstance()
        db   = FirebaseDatabase.getInstance().reference

        etTransactionName = findViewById(R.id.etTransactionName)
        etAmount          = findViewById(R.id.etAmount)
        etDate            = findViewById(R.id.etDate)
        spinnerType       = findViewById(R.id.spinnerType)
        spinnerCategory   = findViewById(R.id.spinnerCategory)
        ivReceiptPreview  = findViewById(R.id.ivReceiptPreview)
        tvImageStatus     = findViewById(R.id.tvImageStatus)
        progressBar       = findViewById(R.id.progressBar)
        btnSave           = findViewById(R.id.btnSaveTransaction)

        spinnerType.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item, listOf("Expense", "Income"))

        etDate.setOnClickListener { showDatePicker() }
        findViewById<Button>(R.id.btnPickImage).setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
        btnSave.setOnClickListener { saveTransaction() }


        loadCategories()
        setupBottomNav()
    }
    private fun setupBottomNav() {
        findViewById<LinearLayout>(R.id.btnNavHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
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
        findViewById<LinearLayout>(R.id.btnNavAnalytics).setOnClickListener {
            startActivity(Intent(this, AnalyticsDashboardActivity::class.java))
        }

    }
        private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            etDate.setText(String.format("%02d/%02d/%04d", d, m + 1, y))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadCategories() {
        val uid = auth.currentUser?.uid ?: return
        db.child("categories").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    categoryList.clear(); categoryNames.clear()
                    for (child in snapshot.children) {
                        val cat = child.getValue(Category::class.java) ?: continue
                        categoryList.add(cat); categoryNames.add(cat.category_name)
                    }
                    if (categoryNames.isEmpty()) categoryNames.add("Uncategorized")
                    spinnerCategory.adapter = ArrayAdapter(
                        this@AddTransactionActivity,
                        android.R.layout.simple_spinner_dropdown_item, categoryNames)
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AddTransactionActivity,
                        "Could not load categories: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun saveTransaction() {
        val name      = etTransactionName.text.toString().trim()
        val amountStr = etAmount.text.toString().trim()
        val date      = etDate.text.toString().trim()
        val type      = spinnerType.selectedItem.toString()

        if (name.isEmpty())      { etTransactionName.error = "Required"; return }
        if (amountStr.isEmpty()) { etAmount.error = "Required"; return }
        if (date.isEmpty())      { etDate.error = "Select a date"; return }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) { etAmount.error = "Enter a valid amount"; return }

        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Error: not logged in", Toast.LENGTH_LONG).show(); return
        }

        val selectedCat = if (categoryList.isNotEmpty() &&
            spinnerCategory.selectedItemPosition < categoryList.size)
            categoryList[spinnerCategory.selectedItemPosition]
        else Category("", "Uncategorized", "", uid)

        setLoading(true)

        if (selectedImageUri != null) {
            uploadImageThenSave(uid, name, date, type, amount, selectedCat)
        } else {
            saveToDb(uid, name, date, type, amount, selectedCat, "")
        }
    }

    private fun uploadImageThenSave(
        uid: String, name: String, date: String,
        type: String, amount: Double, category: Category
    ) {
        val transId = db.child("transactions").child(uid).push().key
            ?: run { showError("Could not generate transaction ID"); return }

        // Get storage instance — make sure Storage is enabled in Firebase Console
        val storage    = FirebaseStorage.getInstance()
        val storageRef = storage.reference.child("receipts/$uid/$transId.jpg")

        tvImageStatus.text = "Uploading image…"

        storageRef.putFile(selectedImageUri!!)
            .addOnProgressListener { task ->
                val pct = (100.0 * task.bytesTransferred / task.totalByteCount).toInt()
                tvImageStatus.text = "Uploading… $pct%"
            }
            .addOnSuccessListener {
                storageRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        saveToDb(uid, name, date, type, amount, category,
                            downloadUri.toString(), transId)
                    }
                    .addOnFailureListener { e ->
                        showError("Could not get image URL: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                // Show the real error so you know what went wrong
                showError("Upload failed: ${e.message}\n\nMake sure Firebase Storage is enabled in the Firebase Console.")
            }
    }

    private fun saveToDb(
        uid: String, name: String, date: String, type: String,
        amount: Double, category: Category, receiptUrl: String,
        existingId: String? = null
    ) {
        val transId = existingId
            ?: db.child("transactions").child(uid).push().key
            ?: run { showError("Could not generate ID"); return }

        val transaction = Transactions(transId, name, date, type, amount, uid, category, receiptUrl)

        db.child("transactions").child(uid).child(transId).setValue(transaction)
            .addOnSuccessListener {
                Toast.makeText(this, "Transaction saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                showError("Failed to save: ${e.message}")
            }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnSave.isEnabled      = !loading
        btnSave.text           = if (loading) "Saving…" else "Save Transaction"
    }

    private fun showError(msg: String) {
        setLoading(false)
        tvImageStatus.text = "Upload failed"
        tvImageStatus.setTextColor(getColor(android.R.color.holo_red_dark))
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}