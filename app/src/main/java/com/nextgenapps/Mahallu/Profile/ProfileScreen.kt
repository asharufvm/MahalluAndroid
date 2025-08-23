package com.nextgenapps.Mahallu.Profile

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController

import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.shape.RoundedCornerShape


//@Composable
//fun ProfileScreen(navController: NavHostController) {
//    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//        Text("Profile Screen")
//    }
//}




//import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

import com.nextgenapps.Mahallu.R


data class UserProfile(
    val name: String? = null,
    val fatherName: String? = null,
    val houseName: String? = null,
    val address: String? = null,
    val phoneNumber: String? = null,
    val className: String? = null,
    val role: String? = null,
    val profileImageURL: String? = null
)



data class Organization(
    val name: String? = null,
    val image: String? = null
)


object SessionManager {
    var phoneNumber: String? = null
    var organizationId: String? = null
    var role: String? = null

    fun clear() {
        phoneNumber = null
        organizationId = null
        role = null
    }
}



class EditProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile

    private val _organization = MutableStateFlow<Organization?>(null)
    val organization: StateFlow<Organization?> = _organization

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    init {
        getUserMobileNumber()?.let { fetchUserProfile(it) }
    }

    fun getUserMobileNumber(): String? {
        val number = auth.currentUser?.phoneNumber
        SessionManager.phoneNumber = number
        return number
    }

    fun fetchUserProfile(phoneNumber: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val querySnapshot = firestore.collection("users")
                    .whereEqualTo("phoneNumber", phoneNumber)
                    .get()
                    .await()

                if (!querySnapshot.isEmpty) {
                    val userDoc = querySnapshot.documents[0]
                    val profile = userDoc.toObject(UserProfile::class.java)
                    _userProfile.value = profile

                    SessionManager.organizationId = userDoc.getString("organizationId")
                    SessionManager.role = userDoc.getString("role")

                    SessionManager.organizationId?.let(::fetchOrganization)
                } else {
                    _userProfile.value = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _userProfile.value = null
            } finally {
                _loading.value = false
            }
        }
    }

    fun fetchOrganization(organizationId: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("organizations")
                    .document(organizationId)
                    .get()
                    .await()

                _organization.value = snapshot.toObject(Organization::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                _organization.value = null
            }
        }
    }

    fun updateUserProfile(
        name: String,
        fatherName: String,
        houseName: String,
        address: String,
        newImageUri: Uri?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val uid = auth.currentUser?.uid
                val phoneNumber = getUserMobileNumber()

                if (uid == null || phoneNumber == null) {
                    onError("User not logged in")
                    return@launch
                }

                var imageUrl: String? = userProfile.value?.profileImageURL

                // ✅ Upload new image to Storage folder matching your rules
                newImageUri?.let { uri ->
                    val storageRef = storage.reference.child("profileImages/$uid/profile.jpg") // matches rule
                    storageRef.putFile(uri).await()
                    imageUrl = storageRef.downloadUrl.await().toString()
                }

                val updates = mapOf(
                    "name" to name,
                    "fatherName" to fatherName,
                    "houseName" to houseName,
                    "address" to address,
                    "profileImageURL" to imageUrl
                )

                // Update Firestore
                val querySnapshot = firestore.collection("users")
                    .whereEqualTo("phoneNumber", phoneNumber)
                    .get()
                    .await()

                if (!querySnapshot.isEmpty) {
                    val docId = querySnapshot.documents[0].id
                    firestore.collection("users")
                        .document(docId)
                        .update(updates)
                        .await()
                } else {
                    onError("User not found")
                    return@launch
                }

                fetchUserProfile(phoneNumber)
                onSuccess()

            } catch (e: Exception) {
                onError(e.message ?: "Unknown error")
            } finally {
                _loading.value = false
            }
        }
    }
}





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    navController: NavController,
    userViewModel: EditProfileViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val userProfile by userViewModel.userProfile.collectAsState()
    val isLoading by userViewModel.loading.collectAsState()
    val organization by userViewModel.organization.collectAsState()

    val isOverallLoading = isLoading || organization == null
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isOverallLoading)

    // Load profile if not yet loaded
    LaunchedEffect(Unit) {
        if (userProfile == null) {
            userViewModel.getUserMobileNumber()?.let { phone ->
                userViewModel.fetchUserProfile(phone)
            }
        }
    }

    // Everything inside composable scope → safe to use MaterialTheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "My Profile",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            actions = {
                IconButton(onClick = { navController.navigate("edit_profile") }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = {
                userViewModel.getUserMobileNumber()?.let { phone ->
                    userViewModel.fetchUserProfile(phone)
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            if (isOverallLoading) {
                // ✅ Single center-aligned loader
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    // Organization Card
                    organization?.let { org ->
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                elevation = CardDefaults.cardElevation(4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column {
                                    Text(
                                        buildAnnotatedString {
                                            withStyle(style = ParagraphStyle(lineHeight = 40.sp)) {
                                                withStyle(
                                                    style = SpanStyle(
                                                        //color = MaterialTheme.colorScheme.primary,
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Normal
                                                    )
                                                ) { append("Welcome to\n") }
                                                withStyle(
                                                    style = SpanStyle(
                                                        //color = MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 32.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                ) { append(org.name ?: "") }
                                            }
                                        },
                                        modifier = Modifier.padding(16.dp)
                                    )

                                    AsyncImage(
                                        model = org.image,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }

                    // User Profile Card
                    userProfile?.let { profile ->
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    AsyncImage(
                                        model = profile.profileImageURL.takeIf { !it.isNullOrEmpty() }
                                            ?: R.drawable.placeholder_profile,
                                        contentDescription = "User Profile Image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    val textColor = MaterialTheme.colorScheme.onSurface
                                    Text("Name: ${profile.name ?: ""}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 26.sp,
                                        color = textColor)
                                    Text("Father's Name: ${profile.fatherName ?: ""}",
                                        fontWeight = FontWeight.Bold,
                                        color = textColor)
                                    Text("House Name: ${profile.houseName ?: ""}",
                                        fontWeight = FontWeight.Bold,
                                        color = textColor)
                                    Text("Address: ${profile.address ?: ""}",
                                        fontWeight = FontWeight.Bold,
                                        color = textColor)
                                    Text("Phone: ${profile.phoneNumber ?: ""}",
                                        fontWeight = FontWeight.Bold,
                                        color = textColor)
                                    Text("Class: ${profile.className ?: ""}",
                                        fontWeight = FontWeight.Bold,
                                        color = textColor)
                                    Text("Role: ${profile.role ?: ""}",
                                        fontWeight = FontWeight.Bold,
                                        color = textColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}










@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel: EditProfileViewModel = viewModel(),
    onProfileUpdated: () -> Unit
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val isLoading by viewModel.loading.collectAsState()

    var name by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var houseName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    // Fill existing profile values
    LaunchedEffect(userProfile) {
        userProfile?.let {
            name = it.name ?: ""
            fatherName = it.fatherName ?: ""
            houseName = it.houseName ?: ""
            address = it.address ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Profile image area
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null || userProfile?.profileImageURL != null) {
                            AsyncImage(
                                model = selectedImageUri ?: userProfile?.profileImageURL,
                                contentDescription = "Profile Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Default Profile Icon",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = fatherName,
                        onValueChange = { fatherName = it },
                        label = { Text("Father's Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = houseName,
                        onValueChange = { houseName = it },
                        label = { Text("House Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.updateUserProfile(
                                name = name,
                                fatherName = fatherName,
                                houseName = houseName,
                                address = address,
                                newImageUri = selectedImageUri,
                                onSuccess = { onProfileUpdated() },
                                onError = {
                                    Toast
                                        .makeText(context, "Failed: $it", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            "Save",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}




