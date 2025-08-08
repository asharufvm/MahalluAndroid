package com.nextgenapps.Mahallu.DonateNow

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*

//import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nextgenapps.Mahallu.utils.CommonFunctions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateNowScreen(viewModel: DonateNowViewModel = viewModel()) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var amount by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Donate Now") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Account Dropdown
            ExposedDropdownMenuBox(
                expanded = viewModel.accountDropdownExpanded,
                onExpandedChange = { viewModel.accountDropdownExpanded = !viewModel.accountDropdownExpanded }
            ) {
                TextField(
                    value = selectedAccount?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Account") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = viewModel.accountDropdownExpanded) },
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

            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = viewModel.categoryDropdownExpanded,
                onExpandedChange = { viewModel.categoryDropdownExpanded = !viewModel.categoryDropdownExpanded }
            ) {
                TextField(
                    value = selectedCategory?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = viewModel.categoryDropdownExpanded) },
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

            // Amount Text Field
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Enter Amount") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Pay Now Button
            Button(
                onClick = {
                    // TODO: handle pay now
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = selectedAccount != null && selectedCategory != null && amount.isNotBlank()
            ) {
                Text("Pay Now")
            }
        }
    }
}



//package com.example.donationapp.ui.donate




@HiltViewModel
class DonateNowViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    var accountDropdownExpanded by mutableStateOf(false)
    var categoryDropdownExpanded by mutableStateOf(false)

    init {
        fetchAccounts()
        fetchCategories()
    }

    private fun fetchAccounts() {
        _isLoading.value = true
        val path = CommonFunctions.getOrganizationPath(context, "Accounts")
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
        val path = CommonFunctions.getOrganizationPath(context, "Categories")
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
}







//package com.example.donationapp.model

data class Account(
    val name: String = "",
    val isAvailableToPublic: Boolean = false
)

data class Category(
    val name: String = "",
    val isAvailableToPublic: Boolean = false
)
