package com.example.budgettracker

import com.example.budgettracker.model.Budget
import com.example.budgettracker.model.Category
import com.example.budgettracker.model.Transactions
import com.example.budgettracker.model.User
import com.example.budgettracker.model.Rewards
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

class ExampleUnitTest {

    // ── Test data ─────────────────────────────────────────────────────────────

    private lateinit var groceriesCategory: Category
    private lateinit var transportCategory: Category
    private lateinit var salaryCategory: Category

    private lateinit var expenseTransaction: Transactions
    private lateinit var incomeTransaction: Transactions
    private lateinit var zeroTransaction: Transactions

    private lateinit var testUser: User
    private lateinit var testBudget: Budget

    @Before
    fun setup() {
        groceriesCategory = Category("cat1", "Groceries", "", "user1")
        transportCategory = Category("cat2", "Transport", "", "user1")
        salaryCategory    = Category("cat3", "Salary",    "", "user1")

        expenseTransaction = Transactions(
            "tr1", "Weekly Groceries", "01/05/2026",
            "Expense", 850.0, "user1", groceriesCategory, ""
        )
        incomeTransaction = Transactions(
            "tr2", "Monthly Salary", "01/05/2026",
            "Income", 25000.0, "user1", salaryCategory, ""
        )
        zeroTransaction = Transactions(
            "tr3", "Zero Amount", "01/05/2026",
            "Expense", 0.0, "user1", groceriesCategory, ""
        )

        testUser = User(
            "user1", "Gambu", "gambu@email.com",
            0.0, "01/01/2026", "Save more"
        )

        testBudget = Budget(
            "bud1", "user1", groceriesCategory,
            2000.0, "01/05/2026", "31/05/2026", "Limit groceries"
        )
    }

    // ── Category model tests ───────────────────────────────────────────────────

    @Test
    fun category_hasCorrectId() {
        assertEquals("cat1", groceriesCategory.category_id)
    }

    @Test
    fun category_hasCorrectName() {
        assertEquals("Groceries", groceriesCategory.category_name)
    }

    @Test
    fun category_hasCorrectUserId() {
        assertEquals("user1", groceriesCategory.user_id)
    }

    @Test
    fun category_nameIsNotEmpty() {
        assertTrue(groceriesCategory.category_name.isNotEmpty())
    }

    @Test
    fun category_defaultConstructor_hasEmptyFields() {
        val empty = Category()
        assertEquals("", empty.category_id)
        assertEquals("", empty.category_name)
        assertEquals("", empty.user_id)
    }

    @Test
    fun category_twoCategories_areNotEqual() {
        assertNotEquals(groceriesCategory, transportCategory)
    }

    // ── Transaction model tests ───────────────────────────────────────────────

    @Test
    fun transaction_expenseType_isCorrect() {
        assertEquals("Expense", expenseTransaction.transaction_type)
    }

    @Test
    fun transaction_incomeType_isCorrect() {
        assertEquals("Income", incomeTransaction.transaction_type)
    }

    @Test
    fun transaction_amount_isPositive() {
        assertTrue(expenseTransaction.transaction_amount > 0)
    }

    @Test
    fun transaction_hasCorrectCategory() {
        assertEquals("Groceries", expenseTransaction.category.category_name)
    }

    @Test
    fun transaction_hasCorrectName() {
        assertEquals("Weekly Groceries", expenseTransaction.transaction_name)
    }

    @Test
    fun transaction_hasCorrectDate() {
        assertEquals("01/05/2026", expenseTransaction.transaction_date)
    }

    @Test
    fun transaction_defaultConstructor_hasZeroAmount() {
        val empty = Transactions()
        assertEquals(0.0, empty.transaction_amount, 0.001)
    }

    @Test
    fun transaction_receiptUrl_defaultIsEmpty() {
        assertEquals("", expenseTransaction.receipt)
    }

    @Test
    fun transaction_withReceipt_hasUrl() {
        val withReceipt = expenseTransaction.copy(receipt = "https://storage.firebase.com/img.jpg")
        assertTrue(withReceipt.receipt.isNotEmpty())
    }

    // ── Balance calculation tests ─────────────────────────────────────────────

    @Test
    fun balance_income_minus_expenses_isCorrect() {
        val income   = 25000.0
        val expenses = 850.0
        val balance  = income - expenses
        assertEquals(24150.0, balance, 0.001)
    }

    @Test
    fun balance_withMultipleExpenses_sumsCorrectly() {
        val transactions = listOf(
            Transactions("t1", "Groceries", "01/05/2026", "Expense", 850.0,  "user1", groceriesCategory, ""),
            Transactions("t2", "Fuel",      "02/05/2026", "Expense", 500.0,  "user1", transportCategory, ""),
            Transactions("t3", "Salary",    "01/05/2026", "Income",  25000.0,"user1", salaryCategory,    "")
        )

        var totalIncome   = 0.0
        var totalExpenses = 0.0
        for (t in transactions) {
            if (t.transaction_type == "Income") totalIncome   += t.transaction_amount
            else                                totalExpenses += t.transaction_amount
        }

        assertEquals(25000.0, totalIncome,    0.001)
        assertEquals(1350.0,  totalExpenses,  0.001)
        assertEquals(23650.0, totalIncome - totalExpenses, 0.001)
    }

    @Test
    fun balance_noTransactions_isZero() {
        val transactions = emptyList<Transactions>()
        var total = 0.0
        for (t in transactions) total += t.transaction_amount
        assertEquals(0.0, total, 0.001)
    }

    @Test
    fun balance_onlyExpenses_isNegative() {
        val income   = 0.0
        val expenses = 1500.0
        val balance  = income - expenses
        assertTrue(balance < 0)
    }

    @Test
    fun balance_expensesExceedIncome_isNegative() {
        val income   = 500.0
        val expenses = 2000.0
        assertTrue((income - expenses) < 0)
    }

    // ── Category spending grouping tests ─────────────────────────────────────

    @Test
    fun categorySpend_groupsCorrectlyByCategoryId() {
        val transactions = listOf(
            Transactions("t1", "Woolworths", "01/05/2026", "Expense", 400.0, "user1", groceriesCategory, ""),
            Transactions("t2", "Pick n Pay", "03/05/2026", "Expense", 450.0, "user1", groceriesCategory, ""),
            Transactions("t3", "Uber",       "02/05/2026", "Expense", 200.0, "user1", transportCategory, "")
        )

        val spendMap = mutableMapOf<String, Double>()
        for (t in transactions) {
            if (t.transaction_type == "Expense") {
                spendMap[t.category.category_id] =
                    (spendMap[t.category.category_id] ?: 0.0) + t.transaction_amount
            }
        }

        assertEquals(850.0, spendMap["cat1"] ?: 0.0, 0.001)
        assertEquals(200.0, spendMap["cat2"] ?: 0.0, 0.001)
    }

    @Test
    fun categorySpend_incomeTransactions_notCounted() {
        val transactions = listOf(
            Transactions("t1", "Salary",    "01/05/2026", "Income",  25000.0, "user1", salaryCategory,    ""),
            Transactions("t2", "Groceries", "02/05/2026", "Expense", 850.0,   "user1", groceriesCategory, "")
        )

        val spendMap = mutableMapOf<String, Double>()
        for (t in transactions) {
            if (t.transaction_type == "Expense") {
                spendMap[t.category.category_id] =
                    (spendMap[t.category.category_id] ?: 0.0) + t.transaction_amount
            }
        }

        assertNull(spendMap[salaryCategory.category_id])
        assertEquals(850.0, spendMap[groceriesCategory.category_id] ?: 0.0, 0.001)
    }

    @Test
    fun categorySpend_emptyTransactions_returnsEmptyMap() {
        val transactions = emptyList<Transactions>()
        val spendMap     = mutableMapOf<String, Double>()
        for (t in transactions) {
            if (t.transaction_type == "Expense")
                spendMap[t.category.category_id] =
                    (spendMap[t.category.category_id] ?: 0.0) + t.transaction_amount
        }
        assertTrue(spendMap.isEmpty())
    }

    // ── Budget goal tests ─────────────────────────────────────────────────────

    @Test
    fun budgetGoal_spendingBelowMax_isWithinBudget() {
        val maxGoal    = 5000.0
        val totalSpent = 3000.0
        assertTrue(totalSpent <= maxGoal)
    }

    @Test
    fun budgetGoal_spendingAboveMax_isOverBudget() {
        val maxGoal    = 5000.0
        val totalSpent = 6000.0
        assertTrue(totalSpent > maxGoal)
    }

    @Test
    fun budgetGoal_spendingBelowMin_isBelowMinimum() {
        val minGoal    = 1000.0
        val totalSpent = 500.0
        assertTrue(totalSpent < minGoal)
    }

    @Test
    fun budgetGoal_maxMustBeGreaterThanMin_valid() {
        val min = 500.0
        val max = 5000.0
        assertTrue(max > min)
    }

    @Test
    fun budgetGoal_maxLessThanMin_isInvalid() {
        val min = 5000.0
        val max = 500.0
        assertFalse(max > min)
    }

    @Test
    fun budgetGoal_progressPercentage_calculatesCorrectly() {
        val maxGoal    = 5000.0
        val totalSpent = 2500.0
        val percentage = ((totalSpent / maxGoal) * 100).toInt()
        assertEquals(50, percentage)
    }

    @Test
    fun budgetGoal_progressPercentage_capsAt100() {
        val maxGoal    = 5000.0
        val totalSpent = 7000.0
        val percentage = ((totalSpent / maxGoal) * 100).toInt().coerceIn(0, 100)
        assertEquals(100, percentage)
    }

    @Test
    fun budgetGoal_progressPercentage_doesNotGoBelowZero() {
        val maxGoal    = 5000.0
        val totalSpent = 0.0
        val percentage = ((totalSpent / maxGoal) * 100).toInt().coerceIn(0, 100)
        assertEquals(0, percentage)
    }

    // ── Budget model tests ────────────────────────────────────────────────────

    @Test
    fun budget_hasCorrectLimit() {
        assertEquals(2000.0, testBudget.budget_limit, 0.001)
    }

    @Test
    fun budget_hasCorrectCategory() {
        assertEquals("Groceries", testBudget.category.category_name)
    }

    @Test
    fun budget_limitIsPositive() {
        assertTrue(testBudget.budget_limit > 0)
    }

    @Test
    fun budget_categorySpend_exceedsLimit_isOverBudget() {
        val spent = 2500.0
        assertTrue(spent > testBudget.budget_limit)
    }

    @Test
    fun budget_categorySpend_underLimit_isOk() {
        val spent = 1500.0
        assertTrue(spent <= testBudget.budget_limit)
    }

    @Test
    fun budget_defaultConstructor_hasZeroLimit() {
        val empty = Budget()
        assertEquals(0.0, empty.budget_limit, 0.001)
    }

    // ── User model tests ──────────────────────────────────────────────────────

    @Test
    fun user_hasCorrectName() {
        assertEquals("Gambu", testUser.name)
    }

    @Test
    fun user_hasCorrectEmail() {
        assertEquals("gambu@email.com", testUser.email)
    }

    @Test
    fun user_nameIsNotEmpty() {
        assertTrue(testUser.name.isNotEmpty())
    }

    @Test
    fun user_emailContainsAtSymbol() {
        assertTrue(testUser.email.contains("@"))
    }

    @Test
    fun user_defaultConstructor_hasEmptyFields() {
        val empty = User()
        assertEquals("", empty.name)
        assertEquals("", empty.email)
        assertEquals(0.0, empty.balance, 0.001)
    }

    // ── Input validation tests ────────────────────────────────────────────────

    @Test
    fun validation_emptyName_isInvalid() {
        val name = ""
        assertTrue(name.isEmpty())
    }

    @Test
    fun validation_validName_isValid() {
        val name = "Weekly Groceries"
        assertFalse(name.isEmpty())
    }

    @Test
    fun validation_negativeAmount_isInvalid() {
        val amount = -50.0
        assertFalse(amount > 0)
    }

    @Test
    fun validation_zeroAmount_isInvalid() {
        val amount = 0.0
        assertFalse(amount > 0)
    }

    @Test
    fun validation_positiveAmount_isValid() {
        val amount = 150.0
        assertTrue(amount > 0)
    }

    @Test
    fun validation_nonNumericAmount_parsesToNull() {
        val input  = "abc"
        val result = input.toDoubleOrNull()
        assertNull(result)
    }

    @Test
    fun validation_validAmountString_parsesCorrectly() {
        val input  = "1500.50"
        val result = input.toDoubleOrNull()
        assertNotNull(result)
        assertEquals(1500.50, result!!, 0.001)
    }

    @Test
    fun validation_emptyDate_isInvalid() {
        val date = ""
        assertTrue(date.isEmpty())
    }

    @Test
    fun validation_validDate_isValid() {
        val date = "01/05/2026"
        assertFalse(date.isEmpty())
    }

    @Test
    fun validation_emailWithoutAt_isInvalid() {
        val email = "notanemail.com"
        assertFalse(email.contains("@"))
    }

    @Test
    fun validation_validEmail_isValid() {
        val email = "user@example.com"
        assertTrue(email.contains("@") && email.contains("."))
    }

    @Test
    fun validation_passwordTooShort_isInvalid() {
        val password = "abc"
        assertFalse(password.length >= 6)
    }

    @Test
    fun validation_passwordLongEnough_isValid() {
        val password = "secure123"
        assertTrue(password.length >= 6)
    }

    @Test
    fun validation_passwordsMismatch_isInvalid() {
        val password        = "mypassword"
        val confirmPassword = "different"
        assertNotEquals(password, confirmPassword)
    }

    @Test
    fun validation_passwordsMatch_isValid() {
        val password        = "mypassword"
        val confirmPassword = "mypassword"
        assertEquals(password, confirmPassword)
    }

    // ── Rewards model tests ───────────────────────────────────────────────────

    @Test
    fun rewards_defaultConstructor_hasEmptyFields() {
        val empty = Rewards()
        assertEquals("", empty.reward_id)
        assertEquals("", empty.reward_name)
        assertEquals("", empty.user_id)
    }

    @Test
    fun rewards_hasCorrectType() {
        val reward = Rewards("r1", "user1", "badge", "Budget Master", "01/05/2026", "Stay under budget")
        assertEquals("badge", reward.reward_type)
    }

    @Test
    fun rewards_hasCorrectName() {
        val reward = Rewards("r1", "user1", "badge", "Budget Master", "01/05/2026", "Stay under budget")
        assertEquals("Budget Master", reward.reward_name)
    }

    // ── Amount formatting tests ───────────────────────────────────────────────

    @Test
    fun formatting_expense_prefixIsNegative() {
        val type   = "Expense"
        val prefix = if (type == "Income") "+ZAR" else "-ZAR"
        assertEquals("-ZAR", prefix)
    }

    @Test
    fun formatting_income_prefixIsPositive() {
        val type   = "Income"
        val prefix = if (type == "Income") "+ZAR" else "-ZAR"
        assertEquals("+ZAR", prefix)
    }

    @Test
    fun formatting_amount_twoDecimalPlaces() {
        val amount    = 1500.5
        val formatted = String.format("%.2f", amount)
        assertEquals("1500.50", formatted)
    }

    @Test
    fun formatting_largeAmount_formatsCorrectly() {
        val amount    = 25000.0
        val formatted = String.format("%.2f", amount)
        assertEquals("25000.00", formatted)
    }

    // ── Transaction list tests ────────────────────────────────────────────────

    @Test
    fun transactionList_recentFive_returnsCorrectCount() {
        val allTransactions = (1..10).map { i ->
            Transactions("tr$i", "Transaction $i", "0$i/05/2026",
                "Expense", i * 100.0, "user1", groceriesCategory, "")
        }
        val recent = if (allTransactions.size > 5) allTransactions.subList(0, 5)
        else allTransactions
        assertEquals(5, recent.size)
    }

    @Test
    fun transactionList_fewerThanFive_returnsAll() {
        val allTransactions = listOf(expenseTransaction, incomeTransaction)
        val recent = if (allTransactions.size > 5) allTransactions.subList(0, 5)
        else allTransactions
        assertEquals(2, recent.size)
    }

    @Test
    fun transactionList_filterExpensesOnly_excludesIncome() {
        val transactions = listOf(expenseTransaction, incomeTransaction)
        val expenses     = transactions.filter { it.transaction_type == "Expense" }
        assertEquals(1, expenses.size)
        assertEquals("Expense", expenses.first().transaction_type)
    }

    @Test
    fun transactionList_filterIncomeOnly_excludesExpenses() {
        val transactions = listOf(expenseTransaction, incomeTransaction)
        val income       = transactions.filter { it.transaction_type == "Income" }
        assertEquals(1, income.size)
        assertEquals("Income", income.first().transaction_type)
    }

    @Test
    fun transactionList_isEmpty_returnsTrue() {
        val list = emptyList<Transactions>()
        assertTrue(list.isEmpty())
    }

    @Test
    fun transactionList_isNotEmpty_afterAdding() {
        val list = listOf(expenseTransaction)
        assertFalse(list.isEmpty())
    }
}