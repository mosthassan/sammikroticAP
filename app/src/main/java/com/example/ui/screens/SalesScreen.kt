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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.CurrencyCode
import com.example.core.model.ExchangeRate
import com.example.core.model.Money
import com.example.data.ledger.SalesItemSpec
import com.example.data.local.AppDatabase
import com.example.data.local.entity.DocumentEntity
import com.example.ui.components.AmountSemanticType
import com.example.ui.components.AmountText
import com.example.ui.components.MoneyField
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusChip
import com.example.ui.theme.SemanticExpenseRed
import com.example.ui.theme.SemanticIncomeGreen
import com.example.ui.theme.SemanticWarningAmber
import com.example.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val documents by viewModel.allDocuments.collectAsState()
    val allocations by viewModel.allAllocations.collectAsState()
    val parties by viewModel.allParties.collectAsState()
    val packages by viewModel.allPackages.collectAsState()
    val treasuries by viewModel.allTreasuries.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") }

    var showNewInvoiceSheet by remember { mutableStateOf(false) }
    var showQuickSaleSheet by remember { mutableStateOf(false) }
    var selectedInvoiceForDetail by remember { mutableStateOf<DocumentEntity?>(null) }
    var invoiceToVoid by remember { mutableStateOf<DocumentEntity?>(null) }
    var voidReason by remember { mutableStateOf("") }

    val partyMap = remember(parties) { parties.associateBy { it.id } }
    val allocationsByInvoice = remember(allocations) { allocations.groupBy { it.invoiceDocId } }

    val salesInvoices = remember(documents, searchQuery, statusFilter, allocationsByInvoice) {
        documents.filter { it.type == "SALES_INVOICE" }.filter { doc ->
            val partyName = partyMap[doc.partyId]?.name ?: ""
            val matchesQuery = searchQuery.isBlank() ||
                    doc.docNumber.toString().contains(searchQuery) ||
                    partyName.contains(searchQuery, ignoreCase = true)

            val paidMinor = allocationsByInvoice[doc.id]?.sumOf { it.allocatedOrigMinor } ?: 0L
            val isPaid = paidMinor >= doc.totalMinor && doc.status != "VOIDED"
            val isPartial = paidMinor > 0L && paidMinor < doc.totalMinor && doc.status != "VOIDED"
            val isCredit = paidMinor == 0L && doc.status != "VOIDED"

            val matchesFilter = when (statusFilter) {
                "PAID" -> isPaid
                "PARTIAL" -> isPartial
                "UNPAID" -> isCredit
                "VOIDED" -> doc.status == "VOIDED"
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FloatingActionButton(
                    onClick = { showQuickSaleSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("fab_quick_sale")
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FlashOn, contentDescription = "بيع سريع")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("بيع سريع", fontWeight = FontWeight.Bold)
                    }
                }

                FloatingActionButton(
                    onClick = { showNewInvoiceSheet = true },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.testTag("fab_new_invoice")
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = "فاتورة جديدة")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("فاتورة جديدة")
                    }
                }
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
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("بحث برقم الفاتورة أو اسم العميل/الوكيل") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sales_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "ALL" to "الكل",
                    "UNPAID" to "آجلة (غير مسددة)",
                    "PARTIAL" to "سداد جزئي",
                    "PAID" to "مسددة بالكامل",
                    "VOIDED" to "ملغية"
                ).forEach { (code, title) ->
                    FilterChip(
                        selected = statusFilter == code,
                        onClick = { statusFilter = code },
                        label = { Text(title, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // List of Invoices
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (salesInvoices.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا توجد فواتير مبيعات تطابق البحث", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                items(salesInvoices) { inv ->
                    val partyName = partyMap[inv.partyId]?.name ?: "عميل غير معروف"
                    val paidMinor = allocationsByInvoice[inv.id]?.sumOf { it.allocatedOrigMinor } ?: 0L
                    val remainingMinor = (inv.totalMinor - paidMinor).coerceAtLeast(0L)

                    val derivedStatus = when {
                        inv.status == "VOIDED" -> "ملغية (REVERSED)"
                        paidMinor >= inv.totalMinor -> "مسددة (PAID)"
                        paidMinor > 0L -> "جزئية (PARTIAL)"
                        else -> "آجلة (UNPAID)"
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                            .clickable { selectedInvoiceForDetail = inv }
                            .testTag("invoice_item_${inv.docNumber}")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "فاتورة مبيعات #${inv.docNumber}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    StatusChip(status = derivedStatus)
                                }
                                AmountText(
                                    money = Money(inv.totalMinor, CurrencyCode.fromString(inv.currency)),
                                    semanticType = AmountSemanticType.INCOME,
                                    fontSize = 14
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "العميل: $partyName",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (remainingMinor > 0L && inv.status != "VOIDED") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "المتبقي: ${Money(remainingMinor, CurrencyCode.fromString(inv.currency)).format()}",
                                        fontSize = 11.sp,
                                        color = SemanticWarningAmber,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "المدفوع: ${Money(paidMinor, CurrencyCode.fromString(inv.currency)).format()}",
                                        fontSize = 11.sp,
                                        color = SemanticIncomeGreen,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Sale Bottom Sheet
    if (showQuickSaleSheet) {
        QuickSaleBottomSheet(
            parties = parties,
            packages = packages,
            treasuries = treasuries,
            onDismiss = { showQuickSaleSheet = false },
            onSubmit = { partyId, pkgId, desc, qty, unitPrice, cashPaid, treasuryId ->
                viewModel.postQuickSale(
                    partyId = partyId,
                    packageId = pkgId,
                    description = desc,
                    quantity = qty,
                    unitPriceMinor = unitPrice,
                    cashPaidMinor = cashPaid,
                    treasuryId = treasuryId,
                    onSuccess = { showQuickSaleSheet = false }
                )
            }
        )
    }

    // New Multi-Line Invoice Sheet
    if (showNewInvoiceSheet) {
        NewInvoiceBottomSheet(
            parties = parties,
            packages = packages,
            onDismiss = { showNewInvoiceSheet = false },
            onSubmit = { partyId, cardItems, serviceItems, currency, rate, notes ->
                viewModel.postSalesInvoice(
                    partyId = partyId,
                    cardItems = cardItems,
                    serviceItems = serviceItems,
                    currency = currency,
                    exchangeRate = rate,
                    notes = notes,
                    onSuccess = { showNewInvoiceSheet = false }
                )
            }
        )
    }

    // Invoice Detail & Receipt Dialog
    selectedInvoiceForDetail?.let { inv ->
        val party = partyMap[inv.partyId]
        val allocs = allocationsByInvoice[inv.id] ?: emptyList()
        val paidMinor = allocs.sumOf { it.allocatedOrigMinor }
        val remainingMinor = (inv.totalMinor - paidMinor).coerceAtLeast(0L)

        AlertDialog(
            onDismissRequest = { selectedInvoiceForDetail = null },
            title = {
                Text("تفاصيل فاتورة مبيعات #${inv.docNumber}")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("العميل / الوكيل: ${party?.name ?: "نقدي"}")
                    Text("التاريخ: يوم ${inv.dateEpochDay}")
                    Text("إجمالي الفاتورة: ${Money(inv.totalMinor, CurrencyCode.fromString(inv.currency)).format()}")
                    Text("المبلغ المسدد: ${Money(paidMinor, CurrencyCode.fromString(inv.currency)).format()}")
                    Text("المبلغ المتبقي: ${Money(remainingMinor, CurrencyCode.fromString(inv.currency)).format()}")
                    if (inv.notes.isNotBlank()) {
                        Text("ملاحظات: ${inv.notes}")
                    }

                    if (inv.status != "VOIDED") {
                        HorizontalDivider()
                        OutlinedButton(
                            onClick = {
                                invoiceToVoid = inv
                                selectedInvoiceForDetail = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SemanticExpenseRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إلغاء الفاتورة بقيد عكسي")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedInvoiceForDetail = null }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Void Confirmation Dialog
    invoiceToVoid?.let { inv ->
        AlertDialog(
            onDismissRequest = { invoiceToVoid = null },
            title = { Text("تأكيد إلغاء الفاتورة #${inv.docNumber}", color = SemanticExpenseRed) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("وفق معايير IFRS: لا يمكن حذف الفاتورة. سيتم إنشاء قيد عكسي تعويضي وإعادة أرصدة الذمم والمخزون.")
                    OutlinedTextField(
                        value = voidReason,
                        onValueChange = { voidReason = it },
                        label = { Text("سبب الإلغاء (إلزامي)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (voidReason.isNotBlank()) {
                            viewModel.voidDocument(inv.id, voidReason) {
                                invoiceToVoid = null
                                voidReason = ""
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SemanticExpenseRed)
                ) {
                    Text("تأكيد الإلغاء والعكس")
                }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToVoid = null }) {
                    Text("تراجع")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSaleBottomSheet(
    parties: List<com.example.data.local.entity.PartyEntity>,
    packages: List<com.example.data.local.entity.CardPackageEntity>,
    treasuries: List<com.example.data.local.entity.TreasuryAccountEntity>,
    onDismiss: () -> Unit,
    onSubmit: (partyId: String, packageId: String?, desc: String, qty: Int, unitPriceMinor: Long, cashPaidMinor: Long, treasuryId: String) -> Unit
) {
    var selectedPartyId by remember { mutableStateOf(AppDatabase.WALK_IN_CASH_PARTY_ID) }
    var selectedPackageId by remember { mutableStateOf(packages.firstOrNull()?.id ?: "") }
    var quantityText by remember { mutableStateOf("1") }
    var customPriceText by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("CASH") } // CASH, CREDIT, PARTIAL
    var cashPaidText by remember { mutableStateOf("") }
    var selectedTreasuryId by remember { mutableStateOf(treasuries.firstOrNull()?.id ?: "TR_MAIN_YER") }

    val activePackage = packages.firstOrNull { it.id == selectedPackageId }
    val unitPriceMinor = customPriceText.toLongOrNull()?.times(100L) ?: (activePackage?.wholesalePriceMinor ?: 50000L)
    val qty = quantityText.toIntOrNull() ?: 1
    val totalInvoiceMinor = unitPriceMinor * qty

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("بيع سريع - نقطة التوزيع الميداني", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            // Select Party
            Text("الطرف المشتري:", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedPartyId == AppDatabase.WALK_IN_CASH_PARTY_ID,
                    onClick = { selectedPartyId = AppDatabase.WALK_IN_CASH_PARTY_ID },
                    label = { Text("عميل نقدي فوري") }
                )
                parties.filter { it.isCustomer && it.id != AppDatabase.WALK_IN_CASH_PARTY_ID }.take(3).forEach { p ->
                    FilterChip(
                        selected = selectedPartyId == p.id,
                        onClick = { selectedPartyId = p.id },
                        label = { Text(p.name) }
                    )
                }
            }

            // Select Package
            Text("باقة الكروت:", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                packages.take(4).forEach { pkg ->
                    FilterChip(
                        selected = selectedPackageId == pkg.id,
                        onClick = {
                            selectedPackageId = pkg.id
                            customPriceText = (pkg.wholesalePriceMinor / 100L).toString()
                        },
                        label = { Text("${pkg.name} (${Money(pkg.wholesalePriceMinor, CurrencyCode.YER).format()})") }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("الكمية") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = if (customPriceText.isBlank()) (unitPriceMinor / 100L).toString() else customPriceText,
                    onValueChange = { customPriceText = it },
                    label = { Text("سعر الوحدة (ر.ي)") },
                    modifier = Modifier.weight(1f)
                )
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("إجمالي الفاتورة:", fontWeight = FontWeight.Bold)
                    AmountText(money = Money(totalInvoiceMinor, CurrencyCode.YER), fontSize = 18)
                }
            }

            // Payment Mode
            Text("طريقة التحصيل:", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = paymentMode == "CASH", onClick = { paymentMode = "CASH" }, label = { Text("نقد كامل فوراً") })
                FilterChip(selected = paymentMode == "CREDIT", onClick = { paymentMode = "CREDIT" }, label = { Text("آجل بالكامل") })
                FilterChip(selected = paymentMode == "PARTIAL", onClick = { paymentMode = "PARTIAL" }, label = { Text("دفعة جزئية") })
            }

            if (paymentMode == "PARTIAL") {
                OutlinedTextField(
                    value = cashPaidText,
                    onValueChange = { cashPaidText = it },
                    label = { Text("المبلغ المدفوع نقداً (ر.ي)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = {
                    val cashToPayMinor = when (paymentMode) {
                        "CASH" -> totalInvoiceMinor
                        "CREDIT" -> 0L
                        "PARTIAL" -> (cashPaidText.toLongOrNull() ?: 0L) * 100L
                        else -> totalInvoiceMinor
                    }
                    val desc = activePackage?.name ?: "كروت إنترنت"
                    onSubmit(selectedPartyId, selectedPackageId, desc, qty, unitPriceMinor, cashToPayMinor, selectedTreasuryId)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submit_quick_sale")
            ) {
                Text("ترحيل البيع السريع وتوليد القيود", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewInvoiceBottomSheet(
    parties: List<com.example.data.local.entity.PartyEntity>,
    packages: List<com.example.data.local.entity.CardPackageEntity>,
    onDismiss: () -> Unit,
    onSubmit: (partyId: String, cardItems: List<SalesItemSpec>, serviceItems: List<SalesItemSpec>, currency: CurrencyCode, rate: ExchangeRate, notes: String) -> Unit
) {
    var selectedPartyId by remember { mutableStateOf(parties.firstOrNull { it.isCustomer }?.id ?: AppDatabase.WALK_IN_CASH_PARTY_ID) }
    var notes by remember { mutableStateOf("") }
    val cardItems = remember { mutableStateListOf<SalesItemSpec>() }

    var itemDesc by remember { mutableStateOf("") }
    var itemQty by remember { mutableStateOf("10") }
    var itemPrice by remember { mutableStateOf("500") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("فاتورة مبيعات جديدة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            // Select Party
            Text("اختر العميل/الوكيل:")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                parties.filter { it.isCustomer }.take(4).forEach { p ->
                    FilterChip(
                        selected = selectedPartyId == p.id,
                        onClick = { selectedPartyId = p.id },
                        label = { Text(p.name, fontSize = 11.sp) }
                    )
                }
            }

            SectionHeader(title = "إضافة بنود الفاتورة")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = itemDesc,
                    onValueChange = { itemDesc = it },
                    label = { Text("الوصف/الباقة") },
                    modifier = Modifier.weight(1.5f)
                )
                OutlinedTextField(
                    value = itemQty,
                    onValueChange = { itemQty = it },
                    label = { Text("الكمية") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = itemPrice,
                    onValueChange = { itemPrice = it },
                    label = { Text("السعر") },
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = {
                    val q = itemQty.toIntOrNull() ?: 1
                    val p = (itemPrice.toLongOrNull() ?: 500L) * 100L
                    val d = if (itemDesc.isBlank()) "كروت إنترنت" else itemDesc
                    cardItems.add(SalesItemSpec(description = d, quantity = q, unitPriceMinor = p))
                    itemDesc = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة السطر للفاتورة")
            }

            // Added Items Preview
            cardItems.forEachIndexed { idx, itm ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${itm.description} (${itm.quantity} × ${itm.unitPriceMinor / 100L})")
                    IconButton(onClick = { cardItems.removeAt(idx) }) {
                        Icon(Icons.Default.Close, contentDescription = "حذف")
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (cardItems.isNotEmpty()) {
                        onSubmit(
                            selectedPartyId,
                            cardItems.toList(),
                            emptyList(),
                            CurrencyCode.YER,
                            ExchangeRate.parity(CurrencyCode.YER),
                            notes
                        )
                    }
                },
                enabled = cardItems.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ترحيل الفاتورة", fontWeight = FontWeight.Bold)
            }
        }
    }
}
