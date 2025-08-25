package com.nextgenapps.Mahallu.MembersListScreen


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Search

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import com.google.gson.Gson
import com.nextgenapps.Mahallu.Profile.SessionManager
import com.nextgenapps.Mahallu.Splash.UserSharedViewModel
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.util.*
import com.nextgenapps.Mahallu.UserDetailScreen.UserDetailScreen
import com.nextgenapps.Mahallu.UserDetailScreen.UserDetailScreenViewModel

import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// --- Model ---
data class UserProfile(
    val id: String = "",
    val name: String = "",
    val fatherName: String = "",
    val houseName: String = "",
    val phoneNumber: String = "",
    val classId: String = "",
    val className: String = "",
    val role: String = "",
    val organizationId: String = ""
)

data class ClassModel(
    val id: String = "",
    val name: String = ""
)

// --- Repository ---


class UserRepository(private val firestore: FirebaseFirestore) {

    fun listenToUsers(orgId: String, onDataChanged: (List<UserProfile>) -> Unit): ListenerRegistration {
        return firestore.collection("users")
            .whereEqualTo("organizationId", orgId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onDataChanged(emptyList())
                    return@addSnapshotListener
                }
                val users = snapshot.documents.mapNotNull {
                    it.toObject(UserProfile::class.java)?.copy(id = it.id)
                }
                onDataChanged(users)
            }
    }

    fun listenToClasses(orgId: String, onDataChanged: (List<ClassModel>) -> Unit): ListenerRegistration {
        return firestore.collection("organizations")
            .document(orgId)
            .collection("Classes")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onDataChanged(emptyList())
                    return@addSnapshotListener
                }
                val classes = snapshot.documents.mapNotNull {
                    it.toObject(ClassModel::class.java)?.copy(id = it.id)
                }
                onDataChanged(classes)
            }
    }
}



// --- ViewModel ---
class MembersListViewModel(
    private val repository: UserRepository
) : ViewModel() {

    var users by mutableStateOf<List<UserProfile>>(emptyList())
    var filteredUsers by mutableStateOf<List<UserProfile>>(emptyList())
    var classes by mutableStateOf<List<ClassModel>>(emptyList())
    var isLoading by mutableStateOf(true)

    var searchQuery by mutableStateOf("")
    var selectedSort by mutableStateOf("Name")
    var selectedClass by mutableStateOf<String?>(null) // store classId
    var selectedRole by mutableStateOf<String?>(null)

    private var usersListener: ListenerRegistration? = null
    private var classesListener: ListenerRegistration? = null

    init {
        subscribeToData()
    }

    private fun subscribeToData() {
        val orgId = SessionManager.organizationId ?: ""

        // Users Listener
        usersListener = repository.listenToUsers(orgId) { userList ->
            users = userList
            applyFilters()
            isLoading = false
        }

        // Classes Listener
        classesListener = repository.listenToClasses(orgId) { classList ->
            classes = classList
            applyFilters()
        }
    }

    fun getSelectedClassName(): String {
        return if (selectedClass == null) {
            "All"
        } else {
            classes.find { it.id == selectedClass }?.name ?: "Unknown"
        }
    }

    fun applyFilters() {
        var list = users

        // Search
        if (searchQuery.isNotEmpty()) {
            list = list.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.phoneNumber.contains(searchQuery, ignoreCase = true) ||
                        it.houseName.contains(searchQuery, ignoreCase = true)
            }
        }

        // Filter by Class
        selectedClass?.let { classId ->
            val selectedClassName = classes.find { it.id == classId }?.name
            if (selectedClassName != null) {
                list = list.filter { it.className == selectedClassName }
            }
        }

        // Filter by Role
        selectedRole?.let { role ->
            list = list.filter { it.role.equals(role, ignoreCase = true) }
        }

        // Sort
        list = when (selectedSort) {
            "Name" -> list.sortedBy { it.name }
            "FatherName" -> list.sortedBy { it.fatherName }
            "HouseName" -> list.sortedBy { it.houseName }
            "Class" -> list.sortedBy { user ->
                classes.find { it.id == user.classId }?.name ?: ""
            }
            else -> list
        }

        filteredUsers = list
    }

    override fun onCleared() {
        super.onCleared()
        usersListener?.remove()
        classesListener?.remove()
    }
}




class MembersListViewModelFactory(
    private val repository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MembersListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MembersListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}



// --- UI ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersListScreen(
    navController: NavHostController,
    userViewModel: UserSharedViewModel, // Shared ViewModel for selected user
    viewModel: MembersListViewModel = viewModel(
        factory = MembersListViewModelFactory(UserRepository(FirebaseFirestore.getInstance()))
    )
) {
    val users = viewModel.filteredUsers
    val isLoading = viewModel.isLoading
    val classes = viewModel.classes

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Members List") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // ---------------- Search ----------------
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = {
                    viewModel.searchQuery = it
                    viewModel.applyFilters()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                label = { Text("Search (Name, Phone, House)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )

            // ---------------- Sort + Filter ----------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // --- Sort Dropdown ---
                var sortExpanded by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { sortExpanded = true }) {
                        Text("Sort: ${viewModel.selectedSort}")
                    }
                    DropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }
                    ) {
                        listOf("Name", "FatherName", "HouseName", "Class").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    viewModel.selectedSort = option
                                    viewModel.applyFilters()
                                    sortExpanded = false
                                }
                            )
                        }
                    }
                }

                // --- Class Filter ---
                var classExpanded by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { classExpanded = true }) {
                        Text("Class: ${viewModel.getSelectedClassName()}")
                    }
                    DropdownMenu(
                        expanded = classExpanded,
                        onDismissRequest = { classExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All") },
                            onClick = {
                                viewModel.selectedClass = null
                                viewModel.applyFilters()
                                classExpanded = false
                            }
                        )
                        classes.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.name) },
                                onClick = {
                                    viewModel.selectedClass = c.id
                                    viewModel.applyFilters()
                                    classExpanded = false
                                }
                            )
                        }
                    }
                }

                // --- Role Filter ---
                var roleExpanded by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { roleExpanded = true }) {
                        Text("Role: ${viewModel.selectedRole ?: "All"}")
                    }
                    DropdownMenu(
                        expanded = roleExpanded,
                        onDismissRequest = { roleExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All") },
                            onClick = {
                                viewModel.selectedRole = null
                                viewModel.applyFilters()
                                roleExpanded = false
                            }
                        )
                        listOf("Admin", "Guest", "Member").forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role) },
                                onClick = {
                                    viewModel.selectedRole = role
                                    viewModel.applyFilters()
                                    roleExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // ---------------- Loading Indicator ----------------
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // ---------------- Users List ----------------
                LazyColumn {
                    itemsIndexed(users, key = { _, user -> user.id }) { index, user ->
                        UserRow(user = user, onClick = {
                            userViewModel.selectedUser = user  // store selected user
                            navController.navigate("user_detail")
                        })
                        if (index < users.lastIndex) {
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserRow(user: UserProfile, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Text(
            user.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(user.phoneNumber, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(2.dp))
        Text(user.houseName, style = MaterialTheme.typography.bodySmall)
    }
}


