package com.nextgenapps.Mahallu.AddReceiptAndVoucher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel


import android.content.Context
import android.content.Intent
import android.net.Uri

import androidx.compose.ui.platform.LocalContext
import com.nextgenapps.Mahallu.Profile.SessionManager
import java.io.File
import java.io.FileOutputStream
import java.util.*

data class DropdownItem(val id: String, val name: String)

class AddReceiptViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _accounts = MutableStateFlow<List<DropdownItem>>(emptyList())
    val accounts: StateFlow<List<DropdownItem>> = _accounts

    private val _categories = MutableStateFlow<List<DropdownItem>>(emptyList())
    val categories: StateFlow<List<DropdownItem>> = _categories

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        fetchAccounts()
        fetchCategories()
    }

    private fun fetchAccounts() {
        _isLoading.value = true
        firestore.collection("organizations")
            .document(SessionManager.organizationId ?: "")
            .collection("Accounts")
            .get()
            .addOnSuccessListener { result ->
                _accounts.value = result.documents.map {
                    DropdownItem(it.id, it.getString("name") ?: "")
                }
                _isLoading.value = false
            }
    }

    private fun fetchCategories() {
        firestore.collection("organizations")
            .document(SessionManager.organizationId ?: "")
            .collection("Categories")
            .get()
            .addOnSuccessListener { result ->
                _categories.value = result.documents.map {
                    DropdownItem(it.id, it.getString("name") ?: "")
                }
            }
    }
}








@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReceiptScreen(
    navController: NavController,
    isReceipt: Boolean // true = Receipt (credit), false = Voucher (debit)
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    // Input states
    var selectedAccount by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedCategory by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedPaymentMode by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Dropdown data
    var accounts by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    // UI states
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showShareButton by remember { mutableStateOf(false) }

    // Fetch Accounts
    LaunchedEffect(Unit) {
        db.collection("organizations")
            .document(SessionManager.organizationId ?: "")
            .collection("Accounts")
            .get()
            .addOnSuccessListener { result ->
                accounts = result.documents.map {
                    it.id to (it.getString("name") ?: "")
                }
            }
    }

    // Fetch Categories
    LaunchedEffect(Unit) {
        db.collection("organizations")
            .document(SessionManager.organizationId ?: "")
            .collection("Categories")
            .get()
            .addOnSuccessListener { result ->
                categories = result.documents.map {
                    it.id to (it.getString("name") ?: "")
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isReceipt) "Add Receipt" else "Add Voucher") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {

            // Account Dropdown
            DropdownMenuBox(
                label = "Select Account",
                items = accounts.map { it.second },
                selected = selectedAccount?.second ?: "",
                onItemSelected = { index -> selectedAccount = accounts[index] }
            )

            Spacer(Modifier.height(12.dp))

            // Category Dropdown
            DropdownMenuBox(
                label = "Select Category",
                items = categories.map { it.second },
                selected = selectedCategory?.second ?: "",
                onItemSelected = { index -> selectedCategory = categories[index] }
            )

            Spacer(Modifier.height(12.dp))

            // Payment Mode
            DropdownMenuBox(
                label = "Select Payment Mode",
                items = listOf("Cash", "Online", "Payment Gateway"),
                selected = selectedPaymentMode,
                onItemSelected = { index -> selectedPaymentMode = listOf("Cash", "Online", "Payment Gateway")[index] }
            )

            Spacer(Modifier.height(12.dp))

            // Amount
            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { c -> c.isDigit() }) amount = it },
                label = { Text("Amount (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Mobile Number
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = {
                    if (it.length <= 10 && it.all { c -> c.isDigit() }) phoneNumber = it
                },
                label = { Text("Mobile Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            // Add Button
            Button(
                onClick = { showConfirmDialog = true },
                enabled = selectedAccount != null &&
                        selectedCategory != null &&
                        selectedPaymentMode.isNotEmpty() &&
                        amount.isNotEmpty() &&
                        phoneNumber.length == 10 &&
                        description.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isReceipt) "Add Receipt" else "Add Voucher")
            }

            if (showShareButton) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        generateAndSharePdf(
                            context,
                            isReceipt,
                            selectedAccount,
                            selectedCategory,
                            selectedPaymentMode,
                            amount,
                            phoneNumber,
                            description
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Share Receipt")
                }
            }
        }

        // Confirmation Dialog
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("Confirm") },
                text = { Text("Are you sure you want to add this ${if (isReceipt) "receipt" else "voucher"}?") },
                confirmButton = {
                    TextButton(onClick = {
                        showConfirmDialog = false

                        val data = hashMapOf(
                            "accountId" to (selectedAccount?.first ?: ""),
                            "accountName" to (selectedAccount?.second ?: ""),
                            "amount" to amount.toInt(),
                            "categoryId" to (selectedCategory?.first ?: ""),
                            "categoryName" to (selectedCategory?.second ?: ""),
                            "descriptionDetails" to description,
                            "paymentCollectedBy" to phoneNumber,
                            "paymentMode" to selectedPaymentMode.lowercase(Locale.ROOT),
                            "phoneNumber" to "+91$phoneNumber",
                            "timestamp" to Date(),
                            "type" to if (isReceipt) "credit" else "debit"
                        )

                        db.collection("organizations")
                            .document(SessionManager.organizationId ?: "")
                            .collection("Donations")
                            .add(data)
                            .addOnSuccessListener {
                                showSuccessDialog = true
                            }
                    }) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Success Dialog
        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false },
                title = { Text("Success") },
                text = { Text("Successfully added ${if (isReceipt) "receipt" else "voucher"}") },
                confirmButton = {
                    TextButton(onClick = {
                        showSuccessDialog = false
                        showShareButton = true

                        // Clear fields
                        selectedAccount = null
                        selectedCategory = null
                        selectedPaymentMode = ""
                        amount = ""
                        phoneNumber = ""
                        description = ""
                    }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuBox(
    label: String,
    items: List<String>,
    selected: String,
    onItemSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEachIndexed { index, text ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onItemSelected(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

fun generateAndSharePdf(
    context: Context,
    isReceipt: Boolean,
    account: Pair<String, String>?,
    category: Pair<String, String>?,
    paymentMode: String,
    amount: String,
    phoneNumber: String,
    description: String
) {
    val file = File(context.cacheDir, "receipt.pdf")
    val fos = FileOutputStream(file)
    fos.write("Receipt/Voucher\n".toByteArray())
    fos.write("Type: ${if (isReceipt) "Credit" else "Debit"}\n".toByteArray())
    fos.write("Account: ${account?.second}\n".toByteArray())
    fos.write("Category: ${category?.second}\n".toByteArray())
    fos.write("Payment Mode: $paymentMode\n".toByteArray())
    fos.write("Amount: ₹$amount\n".toByteArray())
    fos.write("Phone: $phoneNumber\n".toByteArray())
    fos.write("Description: $description\n".toByteArray())
    fos.close()

    val uri = Uri.fromFile(file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Receipt"))
}



