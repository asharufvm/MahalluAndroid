package com.nextgenapps.Mahallu.AccountsScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.firestore.ListenerRegistration
import com.nextgenapps.Mahallu.Profile.SessionManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest

data class Account(
    val id: String = "",
    val name: String = "",
    val isAvailableToPublic: Boolean = false
)






class AccountRepository(
    orgId: String = SessionManager.organizationId ?: ""
) {
    private val db = FirebaseFirestore.getInstance()
    private val accountsRef = db.collection("organizations").document(orgId).collection("Accounts")

    // Live updates
    fun observeAccounts(): Flow<List<Account>> = callbackFlow {
        val reg: ListenerRegistration = accountsRef.addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.map { d ->
                Account(
                    id = d.id,
                    name = d.getString("name") ?: "",
                    isAvailableToPublic = d.getBoolean("isAvailableToPublic") ?: false
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    suspend fun addAccount(name: String, isAvailableToPublic: Boolean) {
        accountsRef.add(
            mapOf(
                "name" to name,
                "isAvailableToPublic" to isAvailableToPublic
            )
        ).await()
    }

    suspend fun updateAccount(account: Account) {
        require(account.id.isNotEmpty())
        accountsRef.document(account.id)
            .update(
                mapOf(
                    "name" to account.name,
                    "isAvailableToPublic" to account.isAvailableToPublic
                )
            ).await()
    }

    suspend fun updatePublic(accountId: String, isPublic: Boolean) {
        accountsRef.document(accountId).update("isAvailableToPublic", isPublic).await()
    }

    suspend fun deleteAccount(accountId: String) {
        accountsRef.document(accountId).delete().await()
    }
}






class AccountsViewModel(
    private val repo: AccountRepository = AccountRepository()
) : ViewModel() {

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeAccounts().collectLatest { list ->
                _accounts.value = list
            }
        }
    }

    fun addAccount(name: String, isPublic: Boolean) = viewModelScope.launch {
        _loading.value = true
        try { repo.addAccount(name, isPublic) } catch (e: Exception) { _error.value = e.message }
        _loading.value = false
    }

    fun saveEdit(accountId: String, name: String, isPublic: Boolean) = viewModelScope.launch {
        _loading.value = true
        try { repo.updateAccount(Account(id = accountId, name = name, isAvailableToPublic = isPublic)) }
        catch (e: Exception) { _error.value = e.message }
        _loading.value = false
    }

    fun togglePublic(account: Account, newValue: Boolean) = viewModelScope.launch {
        try { repo.updatePublic(account.id, newValue) } catch (e: Exception) { _error.value = e.message }
    }

    fun deleteAccount(accountId: String) = viewModelScope.launch {
        _loading.value = true
        try { repo.deleteAccount(accountId) } catch (e: Exception) { _error.value = e.message }
        _loading.value = false
    }

    fun clearError() { _error.value = null }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    navController: NavHostController,
    vm: AccountsViewModel = viewModel()
) {
    val accounts by vm.accounts.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    // Add dialog state
    var showAdd by remember { mutableStateOf(false) }
    var addName by remember { mutableStateOf("") }
    var addPublic by remember { mutableStateOf(false) }

    // Edit dialog state
    var showEdit by remember { mutableStateOf(false) }
    var editId by remember { mutableStateOf("") }
    var editName by remember { mutableStateOf("") }
    var editPublic by remember { mutableStateOf(false) }

    // Delete confirmation state
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accounts") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Account")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                loading && accounts.isEmpty() -> {
                    // ✅ Initial loading
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                accounts.isEmpty() -> {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("No accounts yet")
                        Spacer(Modifier.height(8.dp))
                        FilledTonalButton(onClick = { showAdd = true }) { Text("Add Account") }
                    }
                }
                else -> {
                    // ✅ Show loading overlay during background refresh
                    if (loading) {
                        LinearProgressIndicator(
                            Modifier.fillMaxWidth().align(Alignment.TopCenter)
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(accounts, key = { it.id }) { account ->
                            ElevatedCard(Modifier.fillMaxWidth()) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(account.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            if (account.isAvailableToPublic) "Public" else "Private",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    // Only edit + delete actions now
                                    IconButton(onClick = {
                                        // Open edit dialog prefilled
                                        editId = account.id
                                        editName = account.name
                                        editPublic = account.isAvailableToPublic
                                        showEdit = true
                                    }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(onClick = {
                                        // ✅ Ask confirmation before delete
                                        confirmDeleteId = account.id
                                    }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                    }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(64.dp)) }
                    }
                }
            }

            if (error != null) {
                SnackbarHost(hostState = remember { SnackbarHostState() })
                LaunchedEffect(error) {
                    vm.clearError()
                }
            }
        }
    }

    // ------- Add Dialog -------
    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add New Account") },
            text = {
                Column {
                    OutlinedTextField(
                        value = addName,
                        onValueChange = { addName = it },
                        label = { Text("Account Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Switch(checked = addPublic, onCheckedChange = { addPublic = it })
                        Spacer(Modifier.width(8.dp))
                        Text("Available to Public")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = addName.isNotBlank(),
                    onClick = {
                        vm.addAccount(addName.trim(), addPublic)
                        addName = ""
                        addPublic = false
                        showAdd = false
                    }
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }

    // ------- Edit Dialog -------
    if (showEdit) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("Edit Account") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Account Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Switch(checked = editPublic, onCheckedChange = { editPublic = it })
                        Spacer(Modifier.width(8.dp))
                        Text("Available to Public")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = editId.isNotBlank() && editName.isNotBlank(),
                    onClick = {
                        vm.saveEdit(editId, editName.trim(), editPublic)
                        showEdit = false
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEdit = false }) { Text("Cancel") } }
        )
    }

    // ------- Delete Confirmation Dialog -------
    if (confirmDeleteId != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to delete this account? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteId?.let { vm.deleteAccount(it) }
                    confirmDeleteId = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteId = null }) { Text("Cancel") }
            }
        )
    }
}


