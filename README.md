# BudgetTracker - Android App

A personal finance tracker built with Kotlin + Firebase.

## Features
- User registration & login (Firebase Auth)
- Create custom expense categories
- Log income & expense transactions
- Dashboard with live balance, income, expenses
- Full transaction history

## Setup

### 1. Firebase Setup
> See the included `Firebase_Setup_Guide.pdf` for full step-by-step instructions with screenshots.

1. Go to https://console.firebase.google.com
2. Create a new project named **BudgetTracker**
3. Enable **Authentication** → Email/Password
4. Create **Realtime Database** → test mode
5. Add Android App → package: `com.example.budgettracker`
6. Download `google-services.json` → replace `app/google-services.json`

### 2. Open in Android Studio
1. Open Android Studio → Open → select the `BudgetTracker` folder
2. Wait for Gradle sync to complete
3. Run on emulator or physical device (API 25+)

## Project Structure
```
app/src/main/java/com/example/budgettracker/
├── model/
│   ├── Category.kt
│   ├── Transactions.kt
│   ├── Budget.kt
│   ├── User.kt
│   └── Rewards.kt
├── adapter/
│   └── TransactionAdapter.kt
├── SplashActivity.kt
├── LoginActivity.kt
├── RegisterActivity.kt
├── MainActivity.kt
├── AddTransactionActivity.kt
├── AddCategoryActivity.kt
└── TransactionHistoryActivity.kt
```

## Firebase Database Structure
```
budgettracker-db/
├── users/
│   └── {uid}/
│       ├── user_id, name, email, balance, created_at, main_goal
├── categories/
│   └── {uid}/
│       └── {catId}/ category_id, category_name, category_url, user_id
└── transactions/
    └── {uid}/
        └── {transId}/ transaction_id, transaction_name, transaction_date,
                       transaction_type, transaction_amount, user_id,
                       category{...}, receipt
```
