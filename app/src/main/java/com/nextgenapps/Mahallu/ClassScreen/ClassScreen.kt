package com.nextgenapps.Mahallu.ClassScreen


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.KeyboardType
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

// ----------------- Data Model -----------------
data class ClassItem(
    val id: String = "",
    val name: String = "",
    val amount: Int = 0,
    val frequency: String = "Monthly"
)

// ----------------- ViewModel -----------------
class ClassesViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val classesRef = db.collection("organizations")
        .document(SessionManager.organizationId ?: "")
        .collection("Classes")

    private val _classes = MutableStateFlow<List<ClassItem>>(emptyList())
    val classes: StateFlow<List<ClassItem>> = _classes

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    init {
        fetchClasses()
    }

    private fun fetchClasses() {
        _loading.value = true
        classesRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                _loading.value = false
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.map { doc ->
                ClassItem(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    amount = (doc.getLong("amount") ?: 0).toInt(),
                    frequency = doc.getString("frequency") ?: "Monthly"
                )
            } ?: emptyList()
            _classes.value = list
            _loading.value = false
        }
    }

    fun addClass(name: String, amount: Int, frequency: String) {
        val newClass = hashMapOf(
            "name" to name,
            "amount" to amount,
            "frequency" to frequency
        )
        classesRef.add(newClass)
    }

    fun updateClass(id: String, name: String, amount: Int, frequency: String) {
        val updates = mapOf(
            "name" to name,
            "amount" to amount,
            "frequency" to frequency
        )
        classesRef.document(id).update(updates)
    }

    fun deleteClass(id: String) {
        classesRef.document(id).delete()
    }
}

// ----------------- UI -----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassesScreen(navController: NavHostController, viewModel: ClassesViewModel = viewModel()) {
    val classes by viewModel.classes.collectAsState()
    val loading by viewModel.loading.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<ClassItem?>(null) }
    var deleteItem by remember { mutableStateOf<ClassItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Classes") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDialog = true; editItem = null }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Class")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (loading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(classes) { classItem ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(classItem.name, style = MaterialTheme.typography.titleMedium)
                                    Text("₹${classItem.amount} / ${classItem.frequency}")
                                }
                                Row {
                                    IconButton(onClick = {
                                        editItem = classItem
                                        showDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(onClick = { deleteItem = classItem }) {
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

    // Add/Edit Dialog
    if (showDialog) {
        var name by remember { mutableStateOf(editItem?.name ?: "") }
        var amount by remember { mutableStateOf(editItem?.amount?.toString() ?: "") }
        var frequency by remember { mutableStateOf(editItem?.frequency ?: "Monthly") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editItem == null) "Add Class" else "Edit Class") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = frequency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Frequency") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("Monthly", "Yearly").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        frequency = option
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editItem == null) {
                        viewModel.addClass(name, amount.toIntOrNull() ?: 0, frequency)
                    } else {
                        viewModel.updateClass(editItem!!.id, name, amount.toIntOrNull() ?: 0, frequency)
                    }
                    showDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete confirmation
    if (deleteItem != null) {
        AlertDialog(
            onDismissRequest = { deleteItem = null },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete '${deleteItem!!.name}'?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteClass(deleteItem!!.id)
                    deleteItem = null
                }) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteItem = null }) { Text("Cancel") }
            }
        )
    }
}
