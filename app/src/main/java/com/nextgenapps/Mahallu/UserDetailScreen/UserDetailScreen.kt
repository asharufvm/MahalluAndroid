package com.nextgenapps.Mahallu.UserDetailScreen


import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController


import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User
import com.nextgenapps.Mahallu.MembersListScreen.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    navController: NavHostController,
    userId: String,
    viewModel: UserDetailScreenViewModel = viewModel()
) {
    val context = LocalContext.current
    val user by viewModel.user.collectAsState()
    val classes by viewModel.classes.collectAsState()

    var isEditing by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // ✅ Validation State
    val isFormValid by remember {
        derivedStateOf {
            viewModel.name.isNotBlank() &&
                    viewModel.fatherName.isNotBlank() &&
                    viewModel.houseName.isNotBlank() &&
                    viewModel.phoneNumber.length == 10 &&
                    (viewModel.role == "Guest" || viewModel.className.isNotBlank())
        }
    }

    LaunchedEffect(userId) {
        viewModel.fetchUser(userId)
        viewModel.fetchClasses()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(user?.name ?: "User Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isEditing) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                }
            )
        },
        content = { padding ->
            user?.let {
                // ✅ Scrollable Column
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll( rememberScrollState()), // <-- scroll enabled
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // --- Editable Fields ---
                    OutlinedTextField(
                        value = viewModel.name,
                        onValueChange = { if (isEditing) viewModel.name = it },
                        label = { Text("Name") },
                        readOnly = !isEditing,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = viewModel.fatherName,
                        onValueChange = { if (isEditing) viewModel.fatherName = it },
                        label = { Text("Father Name") },
                        readOnly = !isEditing,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = viewModel.houseName,
                        onValueChange = { if (isEditing) viewModel.houseName = it },
                        label = { Text("House Name") },
                        readOnly = !isEditing,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = viewModel.phoneNumber,
                        onValueChange = {
                            if (isEditing && it.length <= 10 && it.all { ch -> ch.isDigit() }) {
                                viewModel.phoneNumber = it
                            }
                        },
                        label = { Text("Phone Number") },
                        readOnly = !isEditing,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // --- Role Dropdown ---
                    val roles = listOf("Admin", "Member", "Guest")
                    var roleExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = roleExpanded,
                        onExpandedChange = { if (isEditing) roleExpanded = !roleExpanded }
                    ) {
                        OutlinedTextField(
                            value = viewModel.role,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Role") },
                            trailingIcon = {
                                if (isEditing) {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded)
                                }
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .clickable(enabled = isEditing) { roleExpanded = true }
                        )
                        ExposedDropdownMenu(
                            expanded = roleExpanded,
                            onDismissRequest = { roleExpanded = false }
                        ) {
                            roles.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r) },
                                    onClick = {
                                        viewModel.role = r
                                        if (r == "Guest") viewModel.className = ""
                                        roleExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // --- Class Dropdown ---
                    if (viewModel.role != "Guest") {
                        var classExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = classExpanded,
                            onExpandedChange = { if (isEditing) classExpanded = !classExpanded }
                        ) {
                            OutlinedTextField(
                                value = viewModel.className,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Class") },
                                trailingIcon = {
                                    if (isEditing) {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = classExpanded)
                                    }
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .clickable(enabled = isEditing) { classExpanded = true }
                            )
                            ExposedDropdownMenu(
                                expanded = classExpanded,
                                onDismissRequest = { classExpanded = false }
                            ) {
                                classes.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c) },
                                        onClick = {
                                            viewModel.className = c
                                            classExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Navigation Buttons ---
                    Text("Actions", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                navController.navigate("member_account/+91${viewModel.phoneNumber}")
                                      },
                            modifier = Modifier.weight(1f)
                        ) { Text("Due Details") }

                        Button(
                            onClick = {
                                navController.navigate("AddMemberDuesScreen/+91${viewModel.phoneNumber}")
                                      },
                            modifier = Modifier.weight(1f)
                        ) { Text("Add Due") }

                        Button(
                            onClick = {
                                navController.navigate("member_receipts/+91${viewModel.phoneNumber}")
                                      },
                            modifier = Modifier.weight(1f)
                        ) { Text("Receipts") }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Buttons in Edit Mode ---
                    if (isEditing) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showUpdateDialog = true },
                                enabled = isFormValid, // ✅ Enable only if valid
                                modifier = Modifier.weight(1f)
                            ) { Text("Update") }

                            OutlinedButton(
                                onClick = {
                                    user?.let { u -> viewModel.resetFields(u) }
                                    isEditing = false
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel") }
                        }

                        // ✅ Update Confirmation Dialog
                        if (showUpdateDialog) {
                            AlertDialog(
                                onDismissRequest = { showUpdateDialog = false },
                                title = { Text("Confirm Update") },
                                text = { Text("Are you sure you want to update this user?") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            viewModel.updateUser(
                                                onSuccess = {
                                                    Toast.makeText(context, "Updated successfully", Toast.LENGTH_SHORT).show()
                                                    isEditing = false
                                                },
                                                onFailure = { msg ->
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                            showUpdateDialog = false
                                        }
                                    ) {
                                        Text("Update")
                                    }
                                },
                                dismissButton = {
                                    OutlinedButton(onClick = { showUpdateDialog = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Delete User", color = Color.White)
                        }

                        // ✅ Delete Confirmation Dialog
                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                title = { Text("Confirm Delete") },
                                text = { Text("Are you sure you want to delete this user? This action cannot be undone.") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            viewModel.deleteUser(
                                                onSuccess = {
                                                    Toast.makeText(context, "User deleted", Toast.LENGTH_SHORT).show()
                                                    navController.popBackStack()
                                                },
                                                onFailure = { msg ->
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                            showDeleteDialog = false
                                        }
                                    ) {
                                        Text("Delete")
                                    }
                                },
                                dismissButton = {
                                    OutlinedButton(onClick = { showDeleteDialog = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}




// ---------------- ViewModel ------------------

class UserDetailScreenViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _user = MutableStateFlow<UserProfile?>(null)
    val user: StateFlow<UserProfile?> = _user

    var name by mutableStateOf("")
    var fatherName by mutableStateOf("")
    var houseName by mutableStateOf("")
    var phoneNumber by mutableStateOf("") // <-- store only 10 digits
    var role by mutableStateOf("Member")
    var className by mutableStateOf("")
    private val _classes = MutableStateFlow<List<String>>(emptyList())
    val classes: StateFlow<List<String>> = _classes

    fun fetchUser(userId: String) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val u = doc.toObject(UserProfile::class.java)?.copy(id = doc.id)
                    _user.value = u
                    u?.let {
                        name = it.name ?: ""
                        fatherName = it.fatherName ?: ""
                        houseName = it.houseName ?: ""
                        phoneNumber = it.phoneNumber?.removePrefix("+91") ?: "" // strip +91
                        role = it.role ?: "Member"
                        className = it.className ?: ""
                    }
                }
            }
    }

    fun fetchClasses() {
        firestore.collection("organizations")
            .document("0W41mQmirmky9H4KBr53")
            .collection("Classes")
            .get()
            .addOnSuccessListener { snapshot ->
                _classes.value = snapshot.documents.mapNotNull { it.getString("name") }
            }
            .addOnFailureListener { _classes.value = emptyList() }
    }

    fun updateUser(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val u = _user.value ?: return
        val data = mapOf(
            "name" to name,
            "fatherName" to fatherName,
            "houseName" to houseName,
            "phoneNumber" to if (phoneNumber.isNotBlank()) "+91$phoneNumber" else "",
            "role" to role,
            "className" to if (role == "Guest") "" else className
        )
        firestore.collection("users").document(u.id)
            .update(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.message ?: "Update failed") }
    }

    fun deleteUser(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val u = _user.value ?: return
        firestore.collection("users").document(u.id)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.message ?: "Delete failed") }
    }

    fun resetFields(u: UserProfile) {
        name = u.name ?: ""
        fatherName = u.fatherName ?: ""
        houseName = u.houseName ?: ""
        phoneNumber = u.phoneNumber?.removePrefix("+91") ?: ""
        role = u.role ?: "Member"
        className = u.className ?: ""
    }
}









