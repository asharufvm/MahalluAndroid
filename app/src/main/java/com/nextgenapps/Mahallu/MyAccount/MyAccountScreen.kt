package com.nextgenapps.Mahallu.MyAccount
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.navigation.NavHostController


import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.tasks.await

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.TextStyle

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.navigation.NavController
import com.google.firebase.functions.FirebaseFunctions
import com.nextgenapps.Mahallu.Profile.SessionManager

// MyAccountViewModel.kt

data class Due(
    val id: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val createdBy: String = "",
    val date: Timestamp? = null,
    val description: String = "",
    val paidAmount: Double = 0.0,
    val phoneNumber: String = "",
    val status: String = "",
    val type: String = "" // debit or credit
)

// MyAccountViewModel.kt
class MyAccountViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val functions = FirebaseFunctions.getInstance("asia-south1")

    private val _transactions = MutableStateFlow<List<Due>>(emptyList())
    val transactions: StateFlow<List<Due>> = _transactions

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _paymentUrl = MutableStateFlow<String?>(null)
    val paymentUrl: StateFlow<String?> = _paymentUrl

    val totalDue: StateFlow<Double> = _transactions.map { dues ->
        val totalDebit = dues.filter { it.type == "debit" }.sumOf { it.amount }
        val totalCredit = dues.filter { it.type == "credit" }.sumOf { it.amount }
        totalDebit - totalCredit
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    fun loadDues() {
        val organizationId = getOrganizationId() ?: return
        val path = "/organizations/$organizationId/Dues"

        val phone = auth.currentUser?.phoneNumber ?: return
        _isLoading.value = true
        firestore.collection(path)
            .whereEqualTo("phoneNumber", phone)
            .addSnapshotListener { snapshot, e ->
                _isLoading.value = false
                if (e != null) {
                    Log.e("MyAccount", "Error loading dues", e)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Due::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                _transactions.value = list.sortedByDescending { it.date }
            }
    }

    fun donateNow(amount: Double) {
        val organizationId = getOrganizationId() ?: return
        val phone = auth.currentUser?.phoneNumber ?: return

        val params = hashMapOf(
            "organizationId" to organizationId,
            "accountName" to "Mosque",
            "categoryName" to "Due",
            "phoneNumber" to phone,
            "amount" to amount,
            "flowType" to 2 // 1 = Donations, 2 = Dues
        )

        _isLoading.value = true
        functions
            .getHttpsCallable("donateNow")
            .call(params)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (!task.isSuccessful) {
                    val e = task.exception
                    Log.e("MyAccount", "❌ donateNow error", e)
                    _errorMessage.value = e?.localizedMessage ?: "Something went wrong"
                    return@addOnCompleteListener
                }

                val data = task.result?.data as? Map<*, *>
                val urlString = data?.get("payment_link_url") as? String
                if (urlString != null) {
                    _paymentUrl.value = urlString
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

    fun getOrganizationId(): String? {
        return SessionManager.organizationId
    }
}




// MyAccountScreen.kt
// MyAccountScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAccountScreen(
    navController: NavHostController,
    viewModel: MyAccountViewModel = viewModel()
) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsState()
    val totalDue by viewModel.totalDue.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val paymentUrl by viewModel.paymentUrl.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var editableDue by remember { mutableStateOf(totalDue.toString()) }

    // load dues on first render
    LaunchedEffect(Unit) { viewModel.loadDues() }

    // keep editableDue synced with totalDue
    LaunchedEffect(totalDue) { editableDue = totalDue.toString() }

    // show error snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    // open payment URL when available
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Account", style = MaterialTheme.typography.titleLarge) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Transactions list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Total Due Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Total Due",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("₹", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                BasicTextField(
                                    value = editableDue,
                                    onValueChange = {
                                        if (it.toDoubleOrNull() != null || it.isEmpty()) {
                                            editableDue = it
                                        }
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier
                                        .widthIn(min = 80.dp)
                                        .background(Color.Transparent)
                                )
                            }
                            Divider(modifier = Modifier.padding(top = 4.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    editableDue.toDoubleOrNull()?.let { amount ->
                                        if (amount > 0) viewModel.donateNow(amount)
                                    }
                                },
                                enabled = editableDue.toDoubleOrNull()?.let { it > 0 } == true && !isLoading
                            ) {
                                Text("Pay Now")
                            }
                        }
                    }
                }

                // Transactions grouped by year
                val grouped = transactions.groupBy { due ->
                    due.date?.toDate()?.let {
                        SimpleDateFormat("yyyy", Locale.getDefault()).format(it)
                    } ?: "Unknown"
                }

                grouped.forEach { (year, dues) ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column {
                                Text(
                                    text = year,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFE0E0E0))
                                        .padding(8.dp)
                                )
                                dues.forEachIndexed { index, transaction ->
                                    TransactionItem(transaction) {
                                        navController.navigate("transaction_detail/${transaction.id}")
                                    }
                                    if (index != dues.lastIndex) Divider()
                                }
                            }
                        }
                    }
                }
            }

            // 🔄 Overlay loader when isLoading = true
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}



@Composable
fun TransactionItem(due: Due, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(due.category, fontWeight = FontWeight.Bold)
            Text(due.description, style = MaterialTheme.typography.bodySmall)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "₹${String.format("%.2f", due.amount)}",
                color = if (due.type == "debit") Color.Red else Color(0xFF2E7D32),
                fontWeight = FontWeight.Bold
            )
            Text(
                due.date?.toDate()?.let {
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it)
                } ?: "",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}




class TransactionDetailsViewModel(application: Application) : AndroidViewModel(application) {
    /*fun getOrganizationId(): String? {
        val prefs = getApplication<Application>()
            .getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return prefs.getString("organizationId", null)
    }*/
    fun getOrganizationId(): String? {
        return SessionManager.organizationId
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsScreen(
    transactionId: String,
    navController: NavController,
    viewModel: TransactionDetailsViewModel = viewModel()
) {
    val organizationId = viewModel.getOrganizationId()
    val transaction = remember { mutableStateOf<Map<String, Any>?>(null) }
    val isLoading = remember { mutableStateOf(true) }

    LaunchedEffect(transactionId) {
        val db = FirebaseFirestore.getInstance()
        db.collection("organizations")
            .document("$organizationId")
            .collection("Dues")
            .document(transactionId)
            .get()
            .addOnSuccessListener { document ->
                transaction.value = document.data
                isLoading.value = false
            }
            .addOnFailureListener {
                isLoading.value = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            transaction.value?.let { txn ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    txn.forEach { (key, value) ->
                        val displayValue = when (value) {
                            is Timestamp -> {
                                val date = value.toDate()
                                val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                formatter.format(date)
                            }
                            else -> value.toString()
                        }

                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = displayValue,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
            } ?: run {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Transaction not found.")
                }
            }
        }
    }
}










