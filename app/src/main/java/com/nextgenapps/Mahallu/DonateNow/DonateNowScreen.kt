package com.nextgenapps.Mahallu.DonateNow

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController


import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*

//import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
//import androidx.compose.ui.window.application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.nextgenapps.Mahallu.Profile.SessionManager
import com.nextgenapps.Mahallu.utils.CommonFunctions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateNowScreen(
    viewModel: DonateNowViewModel = viewModel()
) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var amount by remember { mutableStateOf("") }

    // Collect error messages
    val errorMessage by viewModel.errorMessage.collectAsState()
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    // Collect payment url and open in Chrome Custom Tab
    val paymentUrl by viewModel.paymentUrl.collectAsState()
    LaunchedEffect(paymentUrl) {
        paymentUrl?.let { url ->
            try {
                val customTabsIntent = CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .setUrlBarHidingEnabled(false)
                    .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                    .build()

                customTabsIntent.launchUrl(context, Uri.parse(url))
            } catch (e: Exception) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
            viewModel.clearPaymentUrl()
        }
    }

    // ✅ No inner Scaffold
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top Bar
        TopAppBar(title = { Text("Donate Now") })

        // Content
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 80.dp), // ✅ scroll under tab bar
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isLoading) {
                    item {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }

                // Account Dropdown
                item {
                    ExposedDropdownMenuBox(
                        expanded = viewModel.accountDropdownExpanded,
                        onExpandedChange = {
                            viewModel.accountDropdownExpanded = !viewModel.accountDropdownExpanded
                        }
                    ) {
                        TextField(
                            value = selectedAccount?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Account") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = viewModel.accountDropdownExpanded
                                )
                            },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = viewModel.accountDropdownExpanded,
                            onDismissRequest = { viewModel.accountDropdownExpanded = false }
                        ) {
                            accounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text(account.name) },
                                    onClick = {
                                        selectedAccount = account
                                        viewModel.accountDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Category Dropdown
                item {
                    ExposedDropdownMenuBox(
                        expanded = viewModel.categoryDropdownExpanded,
                        onExpandedChange = {
                            viewModel.categoryDropdownExpanded =
                                !viewModel.categoryDropdownExpanded
                        }
                    ) {
                        TextField(
                            value = selectedCategory?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Category") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = viewModel.categoryDropdownExpanded
                                )
                            },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = viewModel.categoryDropdownExpanded,
                            onDismissRequest = { viewModel.categoryDropdownExpanded = false }
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        selectedCategory = category
                                        viewModel.categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Amount Text Field
                item {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Enter Amount") },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Pay Now Button
                item {
                    Button(
                        onClick = {
                            viewModel.donateNow(
                                accountName = selectedAccount?.name ?: "",
                                categoryName = selectedCategory?.name ?: "",
                                amount = amount
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = selectedAccount != null &&
                                selectedCategory != null &&
                                amount.isNotBlank()
                    ) {
                        Text("Pay Now")
                    }
                }
            }
        }
    }
}





// -------------------- DonateNowViewModel.kt --------------------

class DonateNowViewModel(application: Application) : AndroidViewModel(application) {

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _paymentUrl = MutableStateFlow<String?>(null)
    val paymentUrl: StateFlow<String?> = _paymentUrl

    var accountDropdownExpanded by mutableStateOf(false)
    var categoryDropdownExpanded by mutableStateOf(false)

    init {
        fetchAccounts()
        fetchCategories()
    }

    private fun fetchAccounts() {
        _isLoading.value = true
        val organizationId = getOrganizationId()
        val path = "/organizations/$organizationId/Accounts"

        Firebase.firestore.collection(path)
            .whereEqualTo("isAvailableToPublic", true)
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapNotNull { it.toObject(Account::class.java) }
                _accounts.value = list
                _isLoading.value = false
            }
            .addOnFailureListener {
                _isLoading.value = false
            }
    }

    private fun fetchCategories() {
        _isLoading.value = true
        val organizationId = getOrganizationId()
        val path = "/organizations/$organizationId/Categories"

        Firebase.firestore.collection(path)
            .whereEqualTo("isAvailableToPublic", true)
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapNotNull { it.toObject(Category::class.java) }
                _categories.value = list
                _isLoading.value = false
            }
            .addOnFailureListener {
                _isLoading.value = false
            }
    }

    private fun getOrganizationId(): String? {
        return SessionManager.organizationId
    }

    private fun getPhoneNumber(): String? {
        return Firebase.auth.currentUser?.phoneNumber
    }

    fun donateNow(accountName: String, categoryName: String, amount: String) {
        val organizationId = getOrganizationId()
        val phoneNumber = getPhoneNumber()
        val doubleAmount = amount.toDoubleOrNull() ?: 0.0

        if (organizationId.isNullOrBlank() || phoneNumber.isNullOrBlank()) {
            _errorMessage.value = "Missing organizationId or phone number"
            return
        }

        _isLoading.value = true

        val params = hashMapOf(
            "organizationId" to organizationId,
            "accountName" to accountName,
            "categoryName" to categoryName,
            "phoneNumber" to phoneNumber,
            "amount" to doubleAmount,
            "flowType" to 1  // 1 = Donations, 2 = Dues
        )

        val functions = Firebase.functions("asia-south1")
        functions
            .getHttpsCallable("donateNow")
            .call(params)
            .addOnCompleteListener { task ->
                _isLoading.value = false

                if (!task.isSuccessful) {
                    _errorMessage.value = task.exception?.localizedMessage ?: "Something went wrong"
                    return@addOnCompleteListener
                }

                val data = task.result?.data as? Map<*, *>
                val url = data?.get("payment_link_url") as? String
                if (url != null) {
                    _paymentUrl.value = url
                } else {
                    _errorMessage.value = "Invalid response from server"
                }
            }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearPaymentUrl() {
        _paymentUrl.value = null
    }
}




data class Account(
    val name: String = "",
    val isAvailableToPublic: Boolean = false
)

data class Category(
    val name: String = "",
    val isAvailableToPublic: Boolean = false
)
