package com.nextgenapps.Mahallu.DuesScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.nextgenapps.Mahallu.Profile.SessionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberDuesScreen(
    navController: NavController,
    phoneNumber: String? = null,
    viewModel: AddMemberDuesViewModel = viewModel()
) {
    // Local states for dropdown expand/collapse
    var accountExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Due") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // 🔹 Account Dropdown
            ExposedDropdownMenuBox(
                expanded = accountExpanded,
                onExpandedChange = { accountExpanded = !accountExpanded }
            ) {
                OutlinedTextField(
                    value = viewModel.selectedAccount?.name ?: "",
                    onValueChange = {},
                    label = { Text("Select Account") },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                DropdownMenu(
                    expanded = accountExpanded,
                    onDismissRequest = { accountExpanded = false }
                ) {
                    viewModel.accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                viewModel.selectedAccount = account
                                accountExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 🔹 Category Dropdown
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = viewModel.selectedCategory?.name ?: "",
                    onValueChange = {},
                    label = { Text("Select Category") },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                DropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    viewModel.categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                viewModel.selectedCategory = category
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 🔹 Amount Input
            OutlinedTextField(
                value = viewModel.amount,
                onValueChange = { viewModel.amount = it },
                label = { Text("Amount") },
                leadingIcon = { Text("₹") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // 🔹 Description Input
            OutlinedTextField(
                value = viewModel.description,
                onValueChange = { viewModel.description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            // 🔹 Add Due Button
            Button(
                onClick = { viewModel.showConfirmDialog = true },
                enabled = viewModel.canSubmit(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Due")
            }
        }

        // Confirmation Dialog
        if (viewModel.showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.showConfirmDialog = false },
                title = { Text("Confirm Add Due") },
                text = { Text("Are you sure you want to add this due?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.showConfirmDialog = false
                        val createdBy = SessionManager.phoneNumber ?: FirebaseAuth.getInstance().currentUser?.phoneNumber ?: ""
                        viewModel.saveDue(
                            phoneNumber = phoneNumber,
                            createdBy = createdBy,
                            onSuccess = { navController.popBackStack() },
                            onError = { /* show error snackbar */ }
                        )
                    }) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.showConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}







data class Account(val id: String, val name: String)
data class Category(val id: String, val name: String)

class AddMemberDuesViewModel(
    private val orgId: String = "0W41mQmirmky9H4KBr53"
) : ViewModel() {

    var accounts by mutableStateOf<List<Account>>(emptyList())
    var categories by mutableStateOf<List<Category>>(emptyList())

    var selectedAccount by mutableStateOf<Account?>(null)
    var selectedCategory by mutableStateOf<Category?>(null)
    var amount by mutableStateOf("")
    var description by mutableStateOf("")

    var showConfirmDialog by mutableStateOf(false)
    var isLoading by mutableStateOf(false)

    private val db = FirebaseFirestore.getInstance()

    init {
        loadAccounts()
        loadCategories()
    }

    private fun loadAccounts() {
        db.collection("organizations").document(orgId).collection("Accounts")
            .get()
            .addOnSuccessListener { result ->
                accounts = result.documents.mapNotNull { doc ->
                    doc.getString("name")?.let { Account(doc.id, it) }
                }
            }
    }

    private fun loadCategories() {
        db.collection("organizations").document(orgId).collection("Categories")
            .get()
            .addOnSuccessListener { result ->
                categories = result.documents.mapNotNull { doc ->
                    doc.getString("name")?.let { Category(doc.id, it) }
                }
            }
    }

    fun canSubmit(): Boolean {
        return selectedAccount != null &&
                selectedCategory != null &&
                amount.isNotBlank() &&
                description.isNotBlank()
    }

    fun saveDue(phoneNumber: String?, createdBy: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (phoneNumber == null) {
            onError("Phone number missing")
            return
        }

        val dueData = hashMapOf(
            "accountName" to (selectedAccount?.name ?: ""),
            "amount" to amount.toDoubleOrNull(),
            "category" to (selectedCategory?.name ?: ""),
            "createdBy" to createdBy,
            "date" to Timestamp.now(),
            "description" to description,
            "paidAmount" to 0,
            "phoneNumber" to phoneNumber,
            "status" to "unpaid",
            "type" to "debit"
        )

        isLoading = true
        db.collection("organizations").document(orgId).collection("Dues")
            .add(dueData)
            .addOnSuccessListener {
                isLoading = false
                onSuccess()
            }
            .addOnFailureListener {
                isLoading = false
                onError(it.message ?: "Error saving due")
            }
    }
}

