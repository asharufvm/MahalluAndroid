package com.nextgenapps.Mahallu.AddDueScreen


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.QuerySnapshot
import com.nextgenapps.Mahallu.Profile.SessionManager
import kotlinx.coroutines.tasks.await

data class DropdownItem(val id: String, val name: String)

class AddDueViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    val accounts = mutableStateOf<List<DropdownItem>>(emptyList())
    val categories = mutableStateOf<List<DropdownItem>>(emptyList())
    val classes = mutableStateOf<List<DropdownItem>>(emptyList())

    val selectedAccount = mutableStateOf<DropdownItem?>(null)
    val selectedCategory = mutableStateOf<DropdownItem?>(null)
    val selectedClass = mutableStateOf<DropdownItem?>(null)

    val amount = mutableStateOf("")
    val description = mutableStateOf("")

    val isLoading = mutableStateOf(false)
    val showSuccessDialog = mutableStateOf(false)
    val successMessage = mutableStateOf("")
    val showConfirmDialog = mutableStateOf(false)

    suspend fun loadDropdownData(orgId: String) {
        accounts.value = fetchCollection("/organizations/$orgId/Accounts")
        categories.value = fetchCollection("/organizations/$orgId/Categories")
        classes.value = fetchCollection("/organizations/$orgId/Classes")
    }

    private suspend fun fetchCollection(path: String): List<DropdownItem> {
        return try {
            val snap: QuerySnapshot = db.collection(path).get().await()
            snap.documents.mapNotNull {
                val name = it.getString("name") ?: return@mapNotNull null
                DropdownItem(it.id, name)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addDue(orgId: String, createdBy: String) {
        viewModelScope.launch {
            try {
                isLoading.value = true

                // fetch users of selected class
                val snap = db.collection("users")
                    .whereEqualTo("organizationId", orgId)
                    .whereEqualTo("className", selectedClass.value?.name)
                    .get().await()

                val batch = db.batch()
                var count = 0

                snap.documents.forEach { doc ->
                    val phone = doc.getString("phoneNumber") ?: return@forEach
                    val due = hashMapOf(
                        "accountName" to selectedAccount.value?.name,
                        "category" to selectedCategory.value?.name,
                        "className" to selectedClass.value?.name,
                        "amount" to amount.value.toDouble(),
                        "description" to description.value,
                        "date" to Timestamp.now(),
                        "createdBy" to createdBy,
                        "phoneNumber" to phone,
                        "status" to "unpaid",
                        "type" to "debit",
                        "paidAmount" to 0
                    )
                    val ref = db.collection("/organizations/$orgId/Dues").document()
                    batch.set(ref, due)
                    count++
                }

                batch.commit().await()

                successMessage.value = "Dues added for $count users"
                showSuccessDialog.value = true

                // reset
                selectedAccount.value = null
                selectedCategory.value = null
                selectedClass.value = null
                amount.value = ""
                description.value = ""

            } catch (e: Exception) {
                successMessage.value = "Error: ${e.message}"
                showSuccessDialog.value = true
            } finally {
                isLoading.value = false
            }
        }
    }

    fun isFormValid(): Boolean {
        return selectedAccount.value != null &&
                selectedCategory.value != null &&
                selectedClass.value != null &&
                amount.value.isNotBlank() &&
                description.value.isNotBlank()
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDueScreen(
    navController: NavHostController,
    viewModel: AddDueViewModel = viewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val orgId = SessionManager.organizationId ?: "ofqbT25pUf1iHvSaj9hg"
    val createdBy =  FirebaseAuth.getInstance().currentUser?.phoneNumber ?: ""

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            viewModel.loadDropdownData(orgId)
        }
    }

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
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {

            if (viewModel.isLoading.value) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DropdownField(
                        label = "Select Account",
                        items = viewModel.accounts.value,
                        selected = viewModel.selectedAccount.value,
                        onSelected = { viewModel.selectedAccount.value = it }
                    )
                    DropdownField(
                        label = "Select Category",
                        items = viewModel.categories.value,
                        selected = viewModel.selectedCategory.value,
                        onSelected = { viewModel.selectedCategory.value = it }
                    )
                    DropdownField(
                        label = "Select Class",
                        items = viewModel.classes.value,
                        selected = viewModel.selectedClass.value,
                        onSelected = { viewModel.selectedClass.value = it }
                    )

                    OutlinedTextField(
                        value = viewModel.amount.value,
                        onValueChange = { viewModel.amount.value = it },
                        label = { Text("Amount") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = viewModel.description.value,
                        onValueChange = { viewModel.description.value = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { viewModel.showConfirmDialog.value = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = viewModel.isFormValid()
                    ) {
                        Text("Add Dues to Users")
                    }
                }
            }

            // Confirmation Dialog
            if (viewModel.showConfirmDialog.value) {
                AlertDialog(
                    onDismissRequest = { viewModel.showConfirmDialog.value = false },
                    title = { Text("Confirm") },
                    text = { Text("Are you sure you want to add dues to all users in ${viewModel.selectedClass.value?.name}?") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.showConfirmDialog.value = false
                            viewModel.addDue(orgId, createdBy)
                        }) {
                            Text("Yes")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.showConfirmDialog.value = false }) {
                            Text("No")
                        }
                    }
                )
            }

            // Success Dialog
            if (viewModel.showSuccessDialog.value) {
                AlertDialog(
                    onDismissRequest = { viewModel.showSuccessDialog.value = false },
                    title = { Text("Result") },
                    text = { Text(viewModel.successMessage.value) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.showSuccessDialog.value = false }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DropdownField(
    label: String,
    items: List<DropdownItem>,
    selected: DropdownItem?,
    onSelected: (DropdownItem) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select")
                }
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.name) },
                    onClick = {
                        onSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}




