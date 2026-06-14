package com.example.budgettracker

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budgettracker.model.Category
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AddCategoryActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var etCategoryName: EditText

    private lateinit var rvCategoriesDisplay: RecyclerView
    private val categoriesList = ArrayList<Category>()
    private lateinit var adapter: CategoryAdapter
    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_category)

        auth = FirebaseAuth.getInstance()
        etCategoryName = findViewById(R.id.etCategoryName)

        rvCategoriesDisplay = findViewById(R.id.rvCategoriesDisplay)
        rvCategoriesDisplay.layoutManager = LinearLayoutManager(this)

        adapter = CategoryAdapter(categoriesList) { category ->
            deleteCategory(category)
        }
        rvCategoriesDisplay.adapter = adapter

        findViewById<Button>(R.id.btnSaveCategory).setOnClickListener { saveCategory() }
        setupBottomNav()
        listenForCategories()
    }

    private fun listenForCategories() {
        val uid = auth.currentUser?.uid ?: return
        dbRef = FirebaseDatabase.getInstance().reference.child("categories").child(uid)

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categoriesList.clear()
                for (catSnapshot in snapshot.children) {
                    val category = catSnapshot.getValue(Category::class.java)
                    if (category != null) {
                        categoriesList.add(category)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AddCategoryActivity, "Error loading categories", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveCategory() {
        val nameInput = etCategoryName.text.toString().trim()

        if (nameInput.isEmpty()) {
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

        val category = Category(catId, nameInput, "", uid)

        db.child("categories").child(uid).child(catId).setValue(category)
            .addOnSuccessListener {
                Toast.makeText(this, "'$nameInput' saved!", Toast.LENGTH_SHORT).show()
                etCategoryName.text.clear()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun deleteCategory(category: Category) {
        val uid = auth.currentUser?.uid ?: return

        // FIXED: Extracting properties by position destructing to bypass property naming mismatch
        val (categoryId, categoryName) = category

        if (categoryId != null) {
            FirebaseDatabase.getInstance().reference
                .child("categories")
                .child(uid)
                .child(categoryId)
                .removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "'$categoryName' deleted!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to delete: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun setupBottomNav() {
        findViewById<LinearLayout>(R.id.btnNavHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.btnNavAdd).setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
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
}

class CategoryAdapter(
    private val list: List<Category>,
    private val onDeleteClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    class ViewHolder(view: View, val txtName: TextView, val btnDel: ImageButton) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            setPadding(16, 16, 16, 16)
            setBackgroundColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER_VERTICAL
        }

        val textView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textSize = 16f
            setTextColor(0xFF333333.toInt())
        }

        val imageButton = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_delete)
            background = null
            imageTintList = android.content.res.ColorStateList.valueOf(0xFFD32F2F.toInt())
        }

        container.addView(textView)
        container.addView(imageButton)

        return ViewHolder(container, textView, imageButton)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        // FIXED: Extracting name safely via component restructuring positions
        val (_, categoryName) = item
        holder.txtName.text = categoryName

        holder.btnDel.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount(): Int = list.size
}