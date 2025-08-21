package com.nextgenapps.Mahallu.MyReceipts

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun MyReceiptsScreen(navController: NavController) {
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("My Receipts") },
//                navigationIcon = {
//                    IconButton(onClick = { navController.popBackStack() }) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                }
//            )
//        }
//    ) { innerPadding ->
//        Column(
//            modifier = Modifier
//                .padding(innerPadding)
//                .padding(16.dp)
//        ) {
//            Text("List of receipts will go here...")
//        }
//    }
//}


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import android.graphics.Paint
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.firestore.Query
import com.nextgenapps.Mahallu.Profile.SessionManager

class MyReceiptsViewModel (application: Application) : AndroidViewModel(application) {

    private val _receipts = MutableStateFlow<List<Donation>>(emptyList())
    val receipts: StateFlow<List<Donation>> = _receipts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadReceipts() {
        val currentUserPhone = FirebaseAuth.getInstance().currentUser?.phoneNumber ?: return

        _isLoading.value = true

        val organizationId = getOrganizationId()
        val path = "/organizations/$organizationId/Donations"

        FirebaseFirestore.getInstance()
            .collection(path)
            .whereEqualTo("phoneNumber", currentUserPhone)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                _receipts.value = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Donation::class.java)
                    } catch (e: Exception) {
                        null // Skip invalid documents
                    }
                }
                _isLoading.value = false
            }
            .addOnFailureListener {
                _receipts.value = emptyList()
                _isLoading.value = false
            }
    }

    /*fun getOrganizationId(): String? {
        val prefs = getApplication<Application>()
            .getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return prefs.getString("organizationId", null)
    }*/
    fun getOrganizationId(): String? {
        return SessionManager.organizationId
    }

}




//class MyReceiptsViewModel : ViewModel() {
//    private val _receipts = MutableStateFlow<List<Donation>>(emptyList())
//    val receipts: StateFlow<List<Donation>> = _receipts
//
//    private val db = FirebaseFirestore.getInstance()
//    private val auth = FirebaseAuth.getInstance()
//
//    fun loadReceipts() {
//        val phone = auth.currentUser?.phoneNumber ?: return
//        viewModelScope.launch {
//            db.collection("/organizations/0W41mQmirmky9H4KBr53/Donations")
//                .whereEqualTo("phoneNumber", phone)
//                .get()
//                .addOnSuccessListener { snapshot ->
//                    _receipts.value = snapshot.documents.mapNotNull { it.toObject(Donation::class.java) }
//                }
//        }
//    }
//}



fun exportAllReceiptsToPdf(context: Context, receipts: List<Donation>) {
    if (receipts.isEmpty()) return

    val pdfDocument = PdfDocument()
    var pageNumber = 1
    var y = 20
    val paint = android.graphics.Paint().apply {
        textSize = 10f
    }

    fun newPage(): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(300, 600, pageNumber).create()
        return pdfDocument.startPage(pageInfo)
    }

    var page = newPage()
    var canvas = page.canvas

    fun drawLine(text: String) {
        if (y > 580) { // End current page and start a new one
            pdfDocument.finishPage(page)
            pageNumber++
            y = 20
            page = newPage()
            canvas = page.canvas
        }
        canvas.drawText(text, 10f, y.toFloat(), paint)
        y += 15
    }

    drawLine("My Receipts")
    drawLine("===============")

    receipts.forEachIndexed { index, donation ->
        val formattedDate = donation.timestamp?.toDate()?.let {
            SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(it)
        } ?: "N/A"

        drawLine("${index + 1}. ${donation.accountName ?: "N/A"} - ₹${donation.amount ?: 0}")
        drawLine("   Category: ${donation.categoryName ?: "N/A"}")
        drawLine("   Date: $formattedDate")
        drawLine("   Payment Mode: ${donation.paymentMode ?: "N/A"}")
        drawLine("   Collected By: ${donation.paymentCollectedBy ?: "N/A"}")
        drawLine("   Description: ${donation.descriptionDetails ?: "N/A"}")
        drawLine("------------------------------")
    }

    // Finish last page
    pdfDocument.finishPage(page)

    val fileName = "all_receipts_${System.currentTimeMillis()}.pdf"
    val file = File(context.cacheDir, fileName)
    pdfDocument.writeTo(FileOutputStream(file))
    pdfDocument.close()

    val uri: Uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        context.packageName + ".provider",
        file
    )
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share or Print All Receipts"))
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReceiptsScreen(
    navController: NavController,
    viewModel: MyReceiptsViewModel = viewModel()
) {
    val receipts by viewModel.receipts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadReceipts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Receipts") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (receipts.isNotEmpty()) {
                        IconButton(onClick = {
                            try {
                                exportAllReceiptsToPdf(context, receipts)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export All")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier.fillMaxSize()
            ) {
                items(receipts) { receipt ->
                    val formattedDate = receipt.timestamp?.toDate()?.let {
                        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(it)
                    } ?: "N/A"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Account: ${receipt.accountName ?: "N/A"}", style = MaterialTheme.typography.bodyLarge)
                            Text("Category: ${receipt.categoryName ?: "N/A"}")
                            Text("Amount: ₹${receipt.amount ?: 0}")
                            Text("Date: $formattedDate")
                            Text("Payment Mode: ${receipt.paymentMode ?: "N/A"}")
                            Text("Collected By: ${receipt.paymentCollectedBy ?: "N/A"}")
                            Text("Description: ${receipt.descriptionDetails ?: "N/A"}")

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = {
                                    try {
                                        exportReceiptToPdf(context, receipt)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(Icons.Default.FileDownload, contentDescription = "Export to PDF")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}






fun exportReceiptToPdf(context: Context, receipt: Donation) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
    val page = pdfDocument.startPage(pageInfo)

    val canvas = page.canvas
    val paint = Paint()

    paint.textSize = 12f
    canvas.drawText("Account: ${receipt.accountName ?: "N/A"}", 10f, 25f, paint)
    canvas.drawText("Amount: ₹${receipt.amount ?: 0.0}", 10f, 50f, paint)
    canvas.drawText("Category: ${receipt.categoryName ?: "N/A"}", 10f, 75f, paint)

    // ✅ Format timestamp before drawing
    val formattedDate = receipt.timestamp?.toDate()?.let {
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(it)
    } ?: "N/A"
    canvas.drawText("Date: $formattedDate", 10f, 100f, paint)

    canvas.drawText("Collected By: ${receipt.paymentCollectedBy ?: "N/A"}", 10f, 125f, paint)
    canvas.drawText("Payment Mode: ${receipt.paymentMode ?: "N/A"}", 10f, 150f, paint)

    pdfDocument.finishPage(page)

    val file = File(context.getExternalFilesDir(null), "receipt.pdf")
    pdfDocument.writeTo(FileOutputStream(file))
    pdfDocument.close()

    // Native share / print
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
}



data class Donation(
    val accountId: String? = null,
    val accountName: String? = null,
    val amount: Double? = null, // nullable & uses Number.toDouble() conversion later
    val categoryId: String? = null,
    val categoryName: String? = null,
    val descriptionDetails: String? = null,
    val paymentCollectedBy: String? = null,
    val paymentMode: String? = null,
    val phoneNumber: String? = null,
    val timestamp: Timestamp? = null, // store as Firestore Timestamp
    val type: String? = null,
    val userId: String? = null
)











