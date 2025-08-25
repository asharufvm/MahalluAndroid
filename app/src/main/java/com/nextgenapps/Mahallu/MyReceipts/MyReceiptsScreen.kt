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
import android.graphics.Typeface
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.firestore.Query
import com.nextgenapps.Mahallu.Profile.SessionManager

// --- Data Model for Organization ---
data class Organization(
    val name: String? = null,
    val address: String? = null,
    val registrationNumber: String? = null,
    val image: String? = null,
    val createdDate: com.google.firebase.Timestamp? = null
)


// --- ViewModel ---
class MyReceiptsViewModel(application: Application) : AndroidViewModel(application) {

    var phoneNumber: String? = null

    private val _receipts = MutableStateFlow<List<Donation>>(emptyList())
    val receipts: StateFlow<List<Donation>> = _receipts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _organization = MutableStateFlow<Organization?>(null)
    val organization: StateFlow<Organization?> = _organization

    fun loadReceipts() {

        val currentUserPhone = phoneNumber
            ?: FirebaseAuth.getInstance().currentUser?.phoneNumber
                    ?: return
        //val currentUserPhone = FirebaseAuth.getInstance().currentUser?.phoneNumber ?: return

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

    fun loadOrganization() {
        val organizationId = getOrganizationId() ?: return
        FirebaseFirestore.getInstance()
            .collection("organizations")
            .document(organizationId)
            .get()
            .addOnSuccessListener { doc ->
                _organization.value = doc.toObject(Organization::class.java)
            }
    }

    fun getOrganizationId(): String? {
        return SessionManager.organizationId
    }
}



// --- Export All Receipts with Org Header ---
fun exportAllReceiptsToPdf(context: Context, receipts: List<Donation>, organization: Organization?) {
    if (receipts.isEmpty()) return

    val pdfDocument = PdfDocument()
    var pageNumber = 1
    var y = 40

    val paint = Paint().apply {
        textSize = 10f
    }
    val boldPaint = Paint().apply {
        textSize = 12f
        isFakeBoldText = true
    }

    fun newPage(): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(300, 600, pageNumber).create()
        return pdfDocument.startPage(pageInfo)
    }

    var page = newPage()
    var canvas = page.canvas

    fun drawLine(text: String, bold: Boolean = false) {
        if (y > 580) {
            pdfDocument.finishPage(page)
            pageNumber++
            y = 40
            page = newPage()
            canvas = page.canvas
        }
        canvas.drawText(text, 10f, y.toFloat(), if (bold) boldPaint else paint)
        y += 15
    }

    // --- Header ---
    organization?.let {
        drawLine(it.name ?: "Organization Name", bold = true)
        drawLine("Reg No: ${it.registrationNumber ?: "N/A"}", bold = true)
        drawLine(it.address ?: "Address not available", bold = true)
        drawLine("================================", bold = true)
        y += 10
    }

    // --- Receipts ---
    drawLine("My Receipts", bold = true)
    drawLine("===============")

    receipts.forEachIndexed { index, donation ->
        val formattedDate = donation.timestamp?.toDate()?.let {
            SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(it)
        } ?: "N/A"

        drawLine("${index + 1}. ${donation.accountName ?: "N/A"} - ₹${donation.amount ?: 0}", bold = true)
        drawLine("   Category: ${donation.categoryName ?: "N/A"}")
        drawLine("   Date: $formattedDate")
        drawLine("   Payment Mode: ${donation.paymentMode ?: "N/A"}")
        drawLine("   Collected By: ${donation.paymentCollectedBy ?: "N/A"}")
        drawLine("   Description: ${donation.descriptionDetails ?: "N/A"}")
        drawLine("------------------------------")
    }

    pdfDocument.finishPage(page)

    val fileName = "all_receipts_${System.currentTimeMillis()}.pdf"
    val file = File(context.cacheDir, fileName)
    pdfDocument.writeTo(FileOutputStream(file))
    pdfDocument.close()

    val uri: Uri = FileProvider.getUriForFile(
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



// --- Composable Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReceiptsScreen(
    navController: NavController,
    phoneNumber: String? = null,
    viewModel: MyReceiptsViewModel = viewModel()
) {
    val receipts by viewModel.receipts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val organization by viewModel.organization.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.phoneNumber = phoneNumber
        viewModel.loadReceipts()
        viewModel.loadOrganization()
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
                                exportAllReceiptsToPdf(context, receipts, organization)
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
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            receipts.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "You don't have any receipts",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            else -> {
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
                                Text("Account: ${receipt.accountName ?: "N/A"}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("Category: ${receipt.categoryName ?: "N/A"}",
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text("Amount: ₹${receipt.amount ?: 0}",
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text("Date: $formattedDate",
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text("Payment Mode: ${receipt.paymentMode ?: "N/A"}",
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text("Collected By: ${receipt.paymentCollectedBy ?: "N/A"}",
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text("Description: ${receipt.descriptionDetails ?: "N/A"}",
                                    color = MaterialTheme.colorScheme.onSurface)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(onClick = {
                                        try {
                                            exportReceiptToPdf(context, receipt, organization)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Icon(
                                            Icons.Default.FileDownload,
                                            contentDescription = "Export to PDF",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



// --- Single Receipt Export (with Header) ---
// --- Receipt Export with Header ---
fun exportReceiptToPdf(context: Context, receipt: Donation, org: Organization?) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
    val page = pdfDocument.startPage(pageInfo)

    val canvas = page.canvas
    val paint = Paint().apply {
        textSize = 12f
        isFakeBoldText = true
    }

    var yPos = 20f

    // --- Header Section ---
    paint.textAlign = Paint.Align.CENTER
    paint.textSize = 14f
    canvas.drawText(org?.name ?: "Organization Name", pageInfo.pageWidth / 2f, yPos, paint)

    paint.textSize = 12f
    yPos += 20f
    canvas.drawText("Reg No: ${org?.registrationNumber ?: "N/A"}", pageInfo.pageWidth / 2f, yPos, paint)

    yPos += 20f
    canvas.drawText(org?.address ?: "Address not available", pageInfo.pageWidth / 2f, yPos, paint)

    // reset for body
    paint.textAlign = Paint.Align.LEFT
    paint.isFakeBoldText = false
    yPos += 30f

    // --- Receipt Details ---
    canvas.drawText("Account: ${receipt.accountName ?: "N/A"}", 10f, yPos, paint)
    yPos += 20f
    canvas.drawText("Amount: ₹${receipt.amount ?: 0.0}", 10f, yPos, paint)
    yPos += 20f
    canvas.drawText("Category: ${receipt.categoryName ?: "N/A"}", 10f, yPos, paint)
    yPos += 20f

    val formattedDate = receipt.timestamp?.toDate()?.let {
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(it)
    } ?: "N/A"
    canvas.drawText("Date: $formattedDate", 10f, yPos, paint)
    yPos += 20f

    canvas.drawText("Collected By: ${receipt.paymentCollectedBy ?: "N/A"}", 10f, yPos, paint)
    yPos += 20f
    canvas.drawText("Payment Mode: ${receipt.paymentMode ?: "N/A"}", 10f, yPos, paint)

    pdfDocument.finishPage(page)

    val file = File(context.getExternalFilesDir(null), "receipt.pdf")
    pdfDocument.writeTo(FileOutputStream(file))
    pdfDocument.close()

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











