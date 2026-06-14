BudgetTracker 

A comprehensive, gamified personal finance tracker designed to help users take control of their spending through visual analytics and achievement-based milestones. Built with Kotlin and powered by Firebase.

changes made from Part 2
implement the menu to all pages rather then having it in the main page and the other pages having back icon so all the pages have bottom menn
removing back icon
implemetning log out at the top 
implementing charts and statics that went there at part 2 
having figures in the category spending then only having progress bar 
being able to delete category 
adding scrollable views to the rest of the app pages 


Features
User Authentication: Secure registration and login powered by Firebase Auth (Email/Password).

Smart Expense Entries: Log transactions seamlessly with the ability to attach receipts or images for physical proof of purchase.

Dynamic Category Management: Create and organize custom expense categories with unique image/URL attachments.

Visual Analytics: Real-time data visualization via:

Pie Charts: Spending distribution broken down by your custom categories.

Bar Charts: Comprehensive breakdowns comparing monthly spending trends over time.

Gamified Milestones: Earn system achievements (Bronze, Silver, Gold badges) dynamically assigned based on transaction frequency and logging history.

Live Dashboard: Keep an eye on your financial health with an active look at your current balance, total income, and running expense totals.

Tech Stack
Language: Kotlin

UI Framework: Material Components, XML Layouts, CoordinatorLayout, NestedScrollView

Backend Platform: Firebase (Realtime Database, Authentication)

Architecture: Model-View-Controller (MVC) / Model-View-ViewModel (MVVM) principles

Setup Instructions
1. Firebase Configuration
Detailed instructions are available in the included Firebase_Setup_Guide.pdf.

Project Creation: Create a new project at the Firebase Console (https://console.firebase.google.com).

Authentication: Enable Email/Password sign-in in the Authentication tab.

Database: Initialize Realtime Database in test mode.

Android Integration:

Add an Android app to your project with the package name: com.example.budgettracker.

Download the generated google-services.json file.

Place it inside your project's /app` directory, replacing the placeholder file.

2. Android Studio Implementation
Clone/Open: Open Android Studio and select the root BudgetTracker folder.

Sync: Wait for Gradle to build and download all dependencies completely.

Run: Launch the application on an Android Emulator or physical test device (API Level 25+).

Project Structure
Plaintext
app/src/main/java/com/example/budgettracker/
├── adapter/             # RecyclerView adapters for lists
│   └── TransactionAdapter.kt
├── model/               # Data transfer objects & data classes
│   ├── Budget.kt
│   ├── Category.kt
│   ├── Rewards.kt
│   ├── Transactions.kt
│   └── User.kt
├── ui/                  # Application Controller & Core Activities
│   ├── AddCategoryActivity.kt
│   ├── AddTransactionActivity.kt       # Includes image/receipt attachments
│   ├── AnalyticsDashboardActivity.kt  # Charts & badge scoring logic
│   ├── BudgetActivity.kt
│   ├── LoginActivity.kt
│   ├── MainActivity.kt
│   ├── RegisterActivity.kt
│   ├── SplashActivity.kt
│   └── TransactionHistoryActivity.kt
└── views/               # Custom Native UI Drawing Canvas Components
    ├── BarChartView.kt
    └── PieChartView.kt
Firebase Database Schema
JSON
{
  "users": {
    "{uid}": {
      "user_id": "auth_uid_string",
      "name": "John Doe",
      "email": "johndoe@example.com",
      "balance": 12500.50,
      "created_at": "14/06/2026",
      "main_goal": "Save for a holiday"
    }
  },
  "categories": {
    "{uid}": {
      "{catId}": {
        "category_id": "generated_cat_id",
        "category_name": "Groceries",
        "category_url": "https://link-to-icon-image.png",
        "user_id": "auth_uid_string"
      }
    }
  },
  "transactions": {
    "{uid}": {
      "{transId}": {
        "transaction_id": "generated_trans_id",
        "transaction_name": "Weekly Shop",
        "transaction_amount": 450.00,
        "transaction_date": "14/06/2026",
        "transaction_type": "Expense",
        "receipt": "https://firebase-storage-link-to-receipt-image.jpg",
        "user_id": "auth_uid_string",
        "category": {
          "category_id": "generated_cat_id",
          "category_name": "Groceries"
        }
      }
    }
  }
}
Badge Reward System
Badges are determined dynamically by tracking user transaction interaction volumes:

Bronze Badge (🥉): Earned immediately upon tracking your first transaction.

Silver Badge (🥈): Earned upon maintaining your logs and adding 10 or more transactions.

Gold Badge (🥇): Awarded to power users tracking 20 or more overall transactions.

Contributing
Feel free to fork this repository, open development branches, and submit pull requests. For major layout modifications or backend adaptations, please open an tracking issue first to review proposed architectural updates.

License
This project is open-source and available under standard educational guidelines.
