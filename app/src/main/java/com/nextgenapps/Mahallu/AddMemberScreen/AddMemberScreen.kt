package com.nextgenapps.Mahallu.AddMemberScreen


import com.google.firebase.Timestamp
import java.util.UUID

import android.app.Application
import androidx.compose.foundation.background
import androidx.lifecycle.AndroidViewModel
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FieldValue
import com.nextgenapps.Mahallu.Profile.SessionManager

data class UserProfile(
    val id: String = "",
    val phoneNumber: String = "",
    val name: String = "",
    val fatherName: String = "",
    val houseName: String = "",
    val address: String = "",
    val role: String = "",
    val classId: String = "",
    val className: String = "",
    val createdAt: Timestamp? = null,
    val organizationId: String = "",
    val organizationName: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "phoneNumber" to phoneNumber,
        "name" to name,
        "fatherName" to fatherName,
        "houseName" to houseName,
        "address" to address,
        "role" to role,
        "classId" to classId,
        "className" to className,
        "createdAt" to createdAt,
        "organizationId" to organizationId,
        "organizationName" to organizationName
    )
}






class AddMemberViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()

    val phoneNumber = MutableStateFlow("")
    val name = MutableStateFlow("")
    val fatherName = MutableStateFlow("")
    val houseName = MutableStateFlow("")
    val address = MutableStateFlow("")
    val role = MutableStateFlow("")
    val classId = MutableStateFlow("")
    val className = MutableStateFlow("")

    private val _classes = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val classes: StateFlow<List<Pair<String, String>>> = _classes

    private val _showSuccessDialog = MutableStateFlow(false)
    val showSuccessDialog: StateFlow<Boolean> = _showSuccessDialog

    private val _showConfirmDialog = MutableStateFlow(false)
    val showConfirmDialog: StateFlow<Boolean> = _showConfirmDialog

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadClasses()
    }

    private fun loadClasses() {
        db.collection("organizations")
            .document(SessionManager.organizationId ?: "")
            .collection("Classes")
            .get()
            .addOnSuccessListener { result ->
                val classList = result.documents.mapNotNull {
                    val id = it.id
                    val name = it.getString("name") ?: ""
                    id to name
                }
                _classes.value = classList
            }
    }

    fun canAddMember(): Boolean {
        if (phoneNumber.value.length != 10) return false
        if (name.value.isBlank() || role.value.isBlank()) return false
        if (role.value != "Guest" && classId.value.isBlank()) return false
        return true
    }

    fun requestAddMember() {
        _showConfirmDialog.value = true
    }

    fun confirmAddMember() {
        _showConfirmDialog.value = false
        addMember()
    }

    private fun addMember() {
        _isLoading.value = true

        val user = UserProfile(
            id = UUID.randomUUID().toString(),
            phoneNumber = "+91${phoneNumber.value}",
            name = name.value,
            fatherName = fatherName.value,
            houseName = houseName.value,
            address = address.value,
            role = role.value,
            classId = if (role.value == "Guest") "" else classId.value,
            className = if (role.value == "Guest") "" else className.value,
            createdAt = null, // we’ll override
            organizationId = SessionManager.organizationId ?: "",
            organizationName = SessionManager.organizationName ?: ""
        )

        val userMap = user.toMap().toMutableMap().apply {
            this["createdAt"] = FieldValue.serverTimestamp()
        }

        db.collection("users")
            .document(user.id)
            .set(userMap)
            .addOnSuccessListener {
                _isLoading.value = false
                _showSuccessDialog.value = true
                clearFields()
            }
            .addOnFailureListener {
                _isLoading.value = false
            }
    }

    fun dismissSuccessDialog() {
        _showSuccessDialog.value = false
    }

    fun dismissConfirmDialog() {
        _showConfirmDialog.value = false
    }

    private fun clearFields() {
        phoneNumber.value = ""
        name.value = ""
        fatherName.value = ""
        houseName.value = ""
        address.value = ""
        role.value = ""
        classId.value = ""
        className.value = ""
    }
}







@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberScreen(
    navController: NavHostController,
    viewModel: AddMemberViewModel = viewModel()
) {
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val name by viewModel.name.collectAsState()
    val fatherName by viewModel.fatherName.collectAsState()
    val houseName by viewModel.houseName.collectAsState()
    val address by viewModel.address.collectAsState()
    val role by viewModel.role.collectAsState()
    val classId by viewModel.classId.collectAsState()
    val classes by viewModel.classes.collectAsState()
    val showSuccessDialog by viewModel.showSuccessDialog.collectAsState()
    val showConfirmDialog by viewModel.showConfirmDialog.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Member") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack()}) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { if (it.length <= 10 && it.all { ch -> ch.isDigit() }) viewModel.phoneNumber.value = it },
                    label = { Text("Mobile Number*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { viewModel.name.value = it },
                    label = { Text("Name*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = fatherName,
                    onValueChange = { viewModel.fatherName.value = it },
                    label = { Text("Father Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = houseName,
                    onValueChange = { viewModel.houseName.value = it },
                    label = { Text("House Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { viewModel.address.value = it },
                    label = { Text("Address") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )

                // Role dropdown
                var roleExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = roleExpanded, onExpandedChange = { roleExpanded = it }) {
                    OutlinedTextField(
                        value = role,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role*") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                        listOf("Admin", "Member", "Guest").forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    viewModel.role.value = item
                                    roleExpanded = false
                                }
                            )
                        }
                    }
                }

                // Show class dropdown only if role != Guest
                if (role != "Guest" && role.isNotBlank()) {
                    var classExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = classExpanded, onExpandedChange = { classExpanded = it }) {
                        OutlinedTextField(
                            value = classes.firstOrNull { it.first == classId }?.second ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Class*") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = classExpanded, onDismissRequest = { classExpanded = false }) {
                            classes.forEach { (id, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        viewModel.classId.value = id
                                        viewModel.className.value = name
                                        classExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { viewModel.requestAddMember() },
                    enabled = viewModel.canAddMember(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Member")
                }
            }

            // Confirmation Dialog
            if (showConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissConfirmDialog() },
                    confirmButton = {
                        TextButton(onClick = { viewModel.confirmAddMember() }) {
                            Text("Yes")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissConfirmDialog() }) {
                            Text("No")
                        }
                    },
                    title = { Text("Confirm") },
                    text = { Text("Do you want to add this member?") }
                )
            }

            // Success Dialog
            if (showSuccessDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissSuccessDialog() },
                    confirmButton = {
                        TextButton(onClick = { viewModel.dismissSuccessDialog() }) {
                            Text("OK")
                        }
                    },
                    title = { Text("Success") },
                    text = { Text("Member added successfully.") }
                )
            }

            // Loading Spinner
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}



