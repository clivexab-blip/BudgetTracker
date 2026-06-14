BudgetTracker 

LINK TO VIDEO 
https://youtu.be/F6c41kGFxFc?feature=shared

A comprehensive, gamified personal finance tracker designed to help users take control of their spending through visual analytics and achievement-based milestones. Built with Kotlin and powered by Firebase.

Changes Made from Part 2
Global Navigation Implementation: Added the shared bottom navigation menu across all application screens instead of restricting it to the main dashboard.

Streamlined Layout Actions: Removed standard back icon headers across secondary screens to embrace seamless global menu transitions.

Quick Account Session Management: Integrated a dedicated log out button layout asset directly at the top header area of the main user interface.

Advanced Graphic UI Components: Deployed visual data modules including a custom PieChartView and BarChartView that were absent in prior iterations.

Upgraded Category Analytics: Enhanced the category spending rows to present real-time numerical figures alongside the existing progress bars.

Dynamic Data Mutation: Implemented secure database nodes and UI triggers allowing users to directly delete transaction categories.

Infinite Viewport Enhancements: Wrapped layout groups inside dedicated scrollable views (NestedScrollView) across remaining application files to avoid container clipping.

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

Place it inside your project's /app directory, replacing the placeholder file.

2. Android Studio Implementation
Clone/Open: Open Android Studio and select the root BudgetTracker folder.

Sync: Wait for Gradle to build and download all dependencies completely.

Run: Launch the application on an Android Emulator or physical test device (API Level 25+).

Project Structure
The files within the main application package (app/src/main/java/com/example/budgettracker/) are organized into the following direct modules:

adapter

TransactionAdapter.kt: Handles list binding and formatting for user transaction records.

model

Budget.kt: Data template holding active budget limits.

Category.kt: Data structure governing transaction tracking categories.

Rewards.kt: Architectural model managing user achievement states.

Transactions.kt: Standard schema structure containing operational record details.

User.kt: Identity structure mapping user attributes.

ui

AddCategoryActivity.kt: Form viewport built to create personalized spending labels.

AddTransactionActivity.kt: Main screen supporting ledger entries with support for file attachments.

AnalyticsDashboardActivity.kt: Core processing activity for visual metrics and user milestone assignments.

BudgetActivity.kt: Operational limits management dashboard.

LoginActivity.kt: Secure authentication entry screen.

MainActivity.kt: Central interaction landing dashboard.

RegisterActivity.kt: Account setup execution flow.

SplashActivity.kt: Initialization launch layout view.

TransactionHistoryActivity.kt: Chronological timeline ledger displaying logged items.

views

BarChartView.kt: Custom continuous engine handling vertical column drawing calculations.

PieChartView.kt: Specialized canvas controller managing multi-category angular distribution displays.

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
