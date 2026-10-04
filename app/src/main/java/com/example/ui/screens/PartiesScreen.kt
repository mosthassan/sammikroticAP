package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ledger.AccountConstants
import com.example.core.model.CurrencyCode
import com.example.core.model.Money
import com.example.core.model.UuidUtils
import com.example.data.local.AppDatabase
import com.example.data.local.entity.PartyEntity
import com.example.domain.usecase.StatementOfAccountReport
import com.example.ui.components.AmountSemanticType
import com.example.ui.components.AmountText
import com.example.ui.components.SectionHeader
import com.example.ui.theme.SemanticExpenseRed
import com.example.ui.theme.SemanticIncomeGreen
import com.example.ui.theme.SemanticWarningAmber
import com.example.ui.viewmodel.AppViewModel

@Composable
fun PartiesScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val parties by viewModel.allParties.collectAsState()
    val currentStatement by viewModel.currentPartyStatement.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Groceries/Agents, 2: Vendors, 3: Partners
    var searchQuery by remember { mutableStateOf("") }
    var showAddPartySheet by remember { mutableStateOf(false) }
    var selectedPartyForStatement by remember { mutableStateOf<PartyEntity?>(null) }

    val filteredParties = remember(parties, selectedTab, searchQuery) {
        parties.filter { p ->
            val matchesTab = when (selectedTab) {
                1 -> p.isCustomer
                2 -> p.isVendor
                3 -> p.isPartner
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() || p.name.contains(searchQuery, ignoreCase = true) || p.phone.contains(searchQuery)
            matchesTab && matchesQuery
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddPartySheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("fab_add_party")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة طرف جديد")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("بحث عن بقالة أو وكيل أو مورد") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Role Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("الكل") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("بقالات ووكلاء") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("موردون") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("شركاء") })
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Parties List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredParties) { party ->
                    var partyBalance by remember { mutableStateOf<Long?>(null) }

                    LaunchedEffect(party.id) {
                        val controlCode = when {
                            party.isPartner -> AccountConstants.PARTNER_CURRENT
                            party.isVendor -> AccountConstants.ACCOUNTS_PAYABLE
                            else -> AccountConstants.ACCOUNTS_RECEIVABLE
                        }
                        partyBalance = viewModel.repository.getPartyBalance(party.id, controlCode)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                            .clickable {
                                selectedPartyForStatement = party
                                val controlCode = when {
                                    party.isPartner -> AccountConstants.PARTNER_CURRENT
                                    party.isVendor -> AccountConstants.ACCOUNTS_PAYABLE
                                    else -> AccountConstants.ACCOUNTS_RECEIVABLE
                                }
                                viewModel.loadStatementOfAccount(party.id, controlCode, null, null)
                            }
                            .testTag("party_item_${party.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (party.isCustomer) Icons.Default.Store else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = party.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (party.isCustomer) RoleChip("وكيل/بقالة", SemanticIncomeGreen)
                                        if (party.isVendor) RoleChip("مورد", Color(0xFF3B82F6))
                                        if (party.isPartner) RoleChip("شريك", SemanticWarningAmber)
                                        if (!party.isActive) RoleChip("مؤرشف", Color.Gray)
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                partyBalance?.let { bal ->
                                    AmountText(
                                        money = Money(bal, CurrencyCode.FUNCTIONAL),
                                        semanticType = AmountSemanticType.AUTO,
                                        fontSize = 14
                                    )
                                }
                                Text("انقر للكشف", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    // Statement of Account Dialog
    selectedPartyForStatement?.let { party ->
        StatementOfAccountDialog(
            party = party,
            report = currentStatement,
            onDismiss = { selectedPartyForStatement = null }
        )
    }

    // Add Party Bottom Sheet
    if (showAddPartySheet) {
        AddPartyBottomSheet(
            onDismiss = { showAddPartySheet = false },
            onSubmit = { name, phone, isCust, isVend, isPart, limitMinor ->
                val newParty = PartyEntity(
                    id = UuidUtils.newTimeOrderedId(),
                    name = name,
                    phone = phone,
                    isCustomer = isCust,
                    isVendor = isVend,
                    isPartner = isPart,
                    creditLimitMinor = limitMinor
                )
                viewModel.insertParty(newParty) {
                    showAddPartySheet = false
                }
            }
        )
    }
}

@Composable
fun RoleChip(title: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = title,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun StatementOfAccountDialog(
    party: PartyEntity,
    report: StatementOfAccountReport?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("كشف حساب: ${party.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = { /* Share statement text */ }) {
                    Icon(Icons.Default.Share, contentDescription = "مشاركة")
                }
            }
        },
        text = {
            if (report == null) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("جاري استخراج أسطر الأستاذ...")
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الرصيد الافتتاحي: ${Money(report.openingBalanceMinor, CurrencyCode.FUNCTIONAL).format()}", fontSize = 12.sp)
                        Text("الرصيد الجاري: ${Money(report.closingBalanceMinor, CurrencyCode.FUNCTIONAL).format()}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.height(280.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (report.items.isEmpty()) {
                            item {
                                Text("لا توجد حركات مسجلة لهذا الطرف في دفتر الأستاذ", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        items(report.items) { itm ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${itm.docType} #${itm.docNumber}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(itm.memo, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        if (itm.baseDebitMinor > 0L) {
                                            Text("مدين: ${Money(itm.baseDebitMinor, CurrencyCode.FUNCTIONAL).format()}", color = SemanticIncomeGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        }
                                        if (itm.baseCreditMinor > 0L) {
                                            Text("دائن: ${Money(itm.baseCreditMinor, CurrencyCode.FUNCTIONAL).format()}", color = SemanticExpenseRed, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        }
                                        Text("الرصيد: ${Money(itm.runningBalanceMinor, CurrencyCode.FUNCTIONAL).format()}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPartyBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: (name: String, phone: String, isCustomer: Boolean, isVendor: Boolean, isPartner: Boolean, creditLimitMinor: Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isCustomer by remember { mutableStateOf(true) }
    var isVendor by remember { mutableStateOf(false) }
    var isPartner by remember { mutableStateOf(false) }
    var creditLimitText by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("إضافة طرف جديد (بقالة / مورد / شريك)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("الاسم الكامل (مطلوب)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("رقم الهاتف / الواتساب") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("أدوار الطرف في النظام:", fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isCustomer, onCheckedChange = { isCustomer = it })
                Text("عميل / وكيل / بقالة توزيع كروت")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isVendor, onCheckedChange = { isVendor = it })
                Text("مورد أجهزة أو خدمات شبكة")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isPartner, onCheckedChange = { isPartner = it })
                Text("شريك في رأس المال")
            }

            OutlinedTextField(
                value = creditLimitText,
                onValueChange = { creditLimitText = it },
                label = { Text("الحد الائتماني المسموح به (اختياري بالريال)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val limit = (creditLimitText.toLongOrNull() ?: 0L) * 100L
                        onSubmit(name, phone, isCustomer, isVendor, isPartner, limit)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("حفظ الطرف في النظام", fontWeight = FontWeight.Bold)
            }
        }
    }
}
