package com.nextgenapps.Mahallu.CategoriesScreen

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.nextgenapps.Mahallu.Profile.SessionManager
import kotlinx.coroutines.launch

data class Category(
    val id: String = "",
    val name: String = "",
    val isAvailableToPublic: Boolean = false
)

class CategoriesViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val orgId = SessionManager.organizationId ?: ""

    val categories = mutableStateListOf<Category>()
    val isLoading = mutableStateOf(true)

    init {
        fetchCategories()
    }

    fun fetchCategories() {
        isLoading.value = true
        db.collection("organizations")
            .document(orgId)
            .collection("Categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoading.value = false
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    categories.clear()
                    for (doc in snapshot.documents) {
                        val category = Category(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            isAvailableToPublic = doc.getBoolean("isAvailableToPublic") ?: false
                        )
                        categories.add(category)
                    }
                    isLoading.value = false
                }
            }
    }

    fun addCategory(name: String, isAvailable: Boolean) {
        val newCategory = hashMapOf(
            "name" to name,
            "isAvailableToPublic" to isAvailable
        )
        db.collection("organizations")
            .document(orgId)
            .collection("Categories")
            .add(newCategory)
    }

    fun updateCategory(id: String, name: String, isAvailable: Boolean) {
        db.collection("organizations")
            .document(orgId)
            .collection("Categories")
            .document(id)
            .update(
                mapOf(
                    "name" to name,
                    "isAvailableToPublic" to isAvailable
                )
            )
    }

    fun deleteCategory(id: String) {
        db.collection("organizations")
            .document(orgId)
            .collection("Categories")
            .document(id)
            .delete()
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    navController: NavController,
    viewModel: CategoriesViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val categories = viewModel.categories
    val isLoading by viewModel.isLoading

    var showAddDialog by remember { mutableStateOf(false) }
    var editCategory by remember { mutableStateOf<Category?>(null) }
    var deleteCategoryId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Category")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.align(androidx.compose.ui.Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    items(categories) { category ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = category.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = if (category.isAvailableToPublic) "Public" else "Private",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Row {
                                    IconButton(onClick = { editCategory = category }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(onClick = { deleteCategoryId = category.id }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        CategoryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, available ->
                viewModel.addCategory(name, available)
                showAddDialog = false
            }
        )
    }

    // Edit dialog
    editCategory?.let { category ->
        CategoryDialog(
            initialName = category.name,
            initialAvailable = category.isAvailableToPublic,
            onDismiss = { editCategory = null },
            onConfirm = { name, available ->
                viewModel.updateCategory(category.id, name, available)
                editCategory = null
            }
        )
    }

    // Delete confirmation dialog
    deleteCategoryId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteCategoryId = null },
            title = { Text("Delete Category") },
            text = { Text("Are you sure you want to delete this category?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCategory(id)
                    deleteCategoryId = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteCategoryId = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun CategoryDialog(
    initialName: String = "",
    initialAvailable: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var available by remember { mutableStateOf(initialAvailable) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialName.isEmpty()) "Add Category" else "Edit Category") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Available to Public")
                    Switch(
                        checked = available,
                        onCheckedChange = { available = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onConfirm(name, available)
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}


