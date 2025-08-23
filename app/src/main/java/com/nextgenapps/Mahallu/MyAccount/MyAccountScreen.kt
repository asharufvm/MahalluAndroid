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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.firebase.functions.FirebaseFunctions
import com.nextgenapps.Mahallu.Profile.SessionManager

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme



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
    var editableDue by rememberSaveable { mutableStateOf("") }

    // Load dues only if not loaded yet
    LaunchedEffect(Unit) {
        if (transactions.isEmpty()) viewModel.loadDues()
    }

    // Keep editableDue synced with totalDue
    LaunchedEffect(totalDue) {
        editableDue = String.format("%.2f", totalDue)
    }

    // Show error snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    // Open payment URL when available
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

    Column(
        modifier = Modifier.background(MaterialTheme.colorScheme.background) // ✅ theme aware
    ) {
        TopAppBar(title = { Text("My Account", style = MaterialTheme.typography.titleLarge) })

        Box(modifier = Modifier.weight(1f)) {
            // ❌ Removed isRefreshing binding to avoid double loader
            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing = false),
                onRefresh = { viewModel.loadDues() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp, end = 12.dp, top = 12.dp, bottom = 80.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Due Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
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
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally)
                                ) {
                                    Text(
                                        "₹",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    BasicTextField(
                                        value = editableDue,
                                        onValueChange = { input ->
                                            if (totalDue > 0) { // ✅ only allow edit if totalDue > 0
                                                val clean = input.replace("[^\\d.]".toRegex(), "")
                                                if (clean.toDoubleOrNull() != null || clean.isEmpty()) {
                                                    editableDue = clean
                                                }
                                            }
                                        },
                                        textStyle = TextStyle(
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            color = if (totalDue > 0) MaterialTheme.colorScheme.onSurface
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) // visually disabled
                                        ),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .widthIn(min = 100.dp)
                                            .wrapContentWidth(Alignment.CenterHorizontally)
                                            .background(Color.Transparent)
                                    )
                                }

                                Divider(
                                    modifier = Modifier.padding(top = 4.dp),
                                    color = MaterialTheme.colorScheme.outline
                                )
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
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column {
                                    Text(
                                        text = year,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(8.dp)
                                    )
                                    dues.forEachIndexed { index, transaction ->
                                        TransactionItem(transaction) {
                                            navController.navigate("transaction_detail/${transaction.id}")
                                        }
                                        if (index != dues.lastIndex) Divider(
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ✅ Single overlay loader only
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)),
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
            Text(
                due.category,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                due.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            // ✅ Pick dark green for credit, dark red for debit
            val amountColor = when (due.type?.lowercase(Locale.ROOT)) {
                "credit" -> Color(0xFF006400) // Dark Green
                "debit" -> Color(0xFF8B0000)  // Dark Red
                else -> MaterialTheme.colorScheme.onSurface
            }

            Text(
                "₹${String.format("%.2f", due.amount)}",
                color = amountColor,
                fontWeight = FontWeight.Bold
            )

            Text(
                due.date?.toDate()?.let {
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it)
                } ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    val organizationId: String = viewModel.getOrganizationId() ?: ""
    val transaction = remember { mutableStateOf<Map<String, Any>?>(null) }
    val isLoading = remember { mutableStateOf(true) }

    val darkGreen = Color(0xFF006400)
    val darkRed = Color(0xFF8B0000)

    LaunchedEffect(transactionId) {
        val db = FirebaseFirestore.getInstance()
        db.collection("organizations")
            .document(organizationId)
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
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            transaction.value?.let { txn ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(top = paddingValues.calculateTopPadding()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    txn.forEach { (key, value) ->
                        val displayValue = when {
                            key.equals("amount", ignoreCase = true) -> {
                                val amount = value.toString().toDoubleOrNull() ?: 0.0
                                "₹${String.format("%.2f", amount)}"
                            }
                            value is Timestamp -> {
                                val date = value.toDate()
                                val formatter = SimpleDateFormat(
                                    "dd MMM yyyy, hh:mm a",
                                    Locale.getDefault()
                                )
                                formatter.format(date)
                            }
                            else -> value.toString()
                        }

                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                elevation = CardDefaults.cardElevation(2.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Label
                                    Text(
                                        text = key.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Value with conditional color
                                    Text(
                                        text = displayValue,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (key.equals("amount", true)) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = when {
                                            key.equals("amount", ignoreCase = true) -> {
                                                when (txn["type"]?.toString()?.lowercase()) {
                                                    "credit" -> darkGreen
                                                    "debit" -> darkRed
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            }
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Spacer at bottom
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            } ?: run {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Transaction not found.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
















