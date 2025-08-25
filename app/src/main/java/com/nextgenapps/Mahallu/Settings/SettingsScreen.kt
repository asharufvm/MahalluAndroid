package com.nextgenapps.Mahallu.Settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.filled.Delete

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Today
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import com.nextgenapps.Mahallu.MainActivity
import com.nextgenapps.Mahallu.Profile.SessionManager

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val role = SessionManager.role // e.g. "admin" or "member"

    // Persist expand/collapse states across recomposition & navigation
    var dataEntryExpanded by rememberSaveable { mutableStateOf(false) }
    var reportsExpanded by rememberSaveable { mutableStateOf(false) }
    var memberMgmtExpanded by rememberSaveable { mutableStateOf(false) }
    var accountMgmtExpanded by rememberSaveable { mutableStateOf(false) }
    var analyticsExpanded by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        item {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // ================== Member Options ==================
        item {
            Text(
                "Member Options",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        item {
            ListItem(
                headlineContent = { Text("My Receipts") },
                leadingContent = {
                    Icon(Icons.Default.Description, contentDescription = "My Receipts")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("my_receipts") }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Sign Out") },
                leadingContent = {
                    Icon(Icons.Default.Logout, contentDescription = "Sign Out")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { logoutAndRestart(context) }
            )
        }

        // ================== Admin Options ==================
        if (role == "Admin" || role == "Owner") {
            // Admin Data Entry
            item {
                SectionHeader(
                    title = "Admin Data Entry",
                    expanded = dataEntryExpanded,
                    onToggle = { dataEntryExpanded = !dataEntryExpanded }
                )
            }
            if (dataEntryExpanded) {
                item {
                    ListItem(
                        headlineContent = { Text("Add Receipt") },
                        leadingContent = { Icon(Icons.Default.Receipt, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("add_receipt") }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("Add Voucher") },
                        leadingContent = { Icon(Icons.Default.AttachMoney, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("add_voucher") }
                    )
                }
            }

            // Admin Reports
            item {
                SectionHeader(
                    title = "Admin Reports",
                    expanded = reportsExpanded,
                    onToggle = { reportsExpanded = !reportsExpanded }
                )
            }
            if (reportsExpanded) {
                item {
                    ListItem(
                        headlineContent = { Text("Day Book") },
                        leadingContent = { Icon(Icons.Default.Book, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("day_book") }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("Account Statement") },
                        leadingContent = { Icon(Icons.Default.Article, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("account_statement") }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("Dues") },
                        leadingContent = { Icon(Icons.Default.Payment, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("dues") }
                    )
                }
            }

            // Admin Member Management
            item {
                SectionHeader(
                    title = "Admin Member Management",
                    expanded = memberMgmtExpanded,
                    onToggle = { memberMgmtExpanded = !memberMgmtExpanded }
                )
            }
            if (memberMgmtExpanded) {
                item {
                    ListItem(
                        headlineContent = { Text("Members List") },
                        leadingContent = { Icon(Icons.Default.People, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("members_list") }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("Add New Member") },
                        leadingContent = { Icon(Icons.Default.PersonAdd, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("add_member") }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("Add Dues") },
                        leadingContent = { Icon(Icons.Default.MoneyOff, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("add_due") }
                    )
                }
            }

            // Admin Account Management
            item {
                SectionHeader(
                    title = "Admin Account Management",
                    expanded = accountMgmtExpanded,
                    onToggle = { accountMgmtExpanded = !accountMgmtExpanded }
                )
            }
            if (accountMgmtExpanded) {
                item {
                    ListItem(
                        headlineContent = { Text("Accounts") },
                        leadingContent = { Icon(Icons.Default.AccountBalance, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("accounts") }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("Categories") },
                        leadingContent = { Icon(Icons.Default.Category, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("categories") }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("Class") },
                        leadingContent = { Icon(Icons.Default.School, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("class") }
                    )
                }
            }

            // Admin Data Analytics
            item {
                SectionHeader(
                    title = "Admin Data Analytics",
                    expanded = analyticsExpanded,
                    onToggle = { analyticsExpanded = !analyticsExpanded }
                )
            }
            if (analyticsExpanded) {
                item {
                    ListItem(
                        headlineContent = { Text("Dashboard") },
                        leadingContent = { Icon(Icons.Default.Dashboard, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("dashboard") }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("Daily Status") },
                        leadingContent = { Icon(Icons.Default.Today, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("daily_status") }
                    )
                }
            }
        }
    }
}


@Composable
private fun SectionHeader(title: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Collapse" else "Expand"
        )
    }
}


fun logoutAndRestart(context: Context) {
    FirebaseAuth.getInstance().signOut()
    clearUserSession(context)

    val intent = Intent(context, MainActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    context.startActivity(intent)
}

fun clearUserSession(context: Context) {
    SessionManager.organizationId = ""
    SessionManager.role = ""
}


