package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ledger.AccountConstants
import com.example.core.model.CurrencyCode
import com.example.core.model.ExchangeRate
import com.example.core.model.Money
import com.example.data.ledger.InvoiceAllocationSpec
import com.example.data.ledger.PaymentVoucherType
import com.example.data.local.AppDatabase
import com.example.data.local.entity.DocumentEntity
import com.example.ui.components.AmountSemanticType
import com.example.ui.components.AmountText
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusChip
import com.example.ui.theme.SemanticExpenseRed
import com.example.ui.theme.SemanticIncomeGreen
import com.example.ui.viewmodel.AppViewModel

@Composable
fun VouchersScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val documents by viewModel.allDocuments.collectAsState()
    val parties by viewModel.allParties.collectAsState()
    val treasuries by viewModel.allTreasuries.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Receipts (قبض), 1: Payments (صرف)
    var showNewReceiptSheet by remember { mutableStateOf(false) }
    var showNewPaymentSheet by remember { mutableStateOf(false) }

    var selectedVoucherForDetail by remember { mutableStateOf<DocumentEntity?>(null) }
    var voucherToVoid by remember { mutableStateOf<DocumentEntity?>(null) }
    var voidReason by remember { mutableStateOf("") }

    val partyMap = remember(parties) { parties.associateBy { it.id } }

    val filteredVouchers = remember(documents, selectedTab) {
        val targetType = if (selectedTab == 0) "RECEIPT_VOUCHER" else "PAYMENT_VOUCHER"
        documents.filter { it.type == targetType }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) showNewReceiptSheet = true
                    else showNewPaymentSheet = true
                },
                containerColor = if (selectedTab == 0) SemanticIncomeGreen else SemanticExpenseRed,
                modifier = Modifier.testTag("fab_new_voucher")
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (selectedTab == 0) "سند قبض جديد" else "سند صرف جديد", fontWeight = FontWeight.Bold)
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
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = SemanticIncomeGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("سندات القبض (مقبوضات)")
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = SemanticExpenseRed)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("سندات الصرف (مدفوعات)")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (filteredVouchers.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("لا توجد سندات مسجلة في هذا القسم", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                items(filteredVouchers) { voucher ->
                    val partyName = partyMap[voucher.partyId]?.name ?: "طرف عام"
                    val isReceipt = voucher.type == "RECEIPT_VOUCHER"

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                            .clickable { selectedVoucherForDetail = voucher }
                            .testTag("voucher_item_${voucher.docNumber}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${if (isReceipt) "سند قبض" else "سند صرف"} #${voucher.docNumber}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    StatusChip(status = voucher.status)
                                }
                                Text("الطرف: $partyName", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                if (voucher.notes.isNotBlank()) {
                                    Text(voucher.notes, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            AmountText(
                                money = Money(voucher.totalMinor, CurrencyCode.fromString(voucher.currency)),
                                semanticType = if (isReceipt) AmountSemanticType.INCOME else AmountSemanticType.EXPENSE,
                                fontSize = 14
                            )
                        }
                    }
                }
            }
        }
    }

    // New Receipt Sheet
    if (showNewReceiptSheet) {
        NewReceiptBottomSheet(
            parties = parties,
            treasuries = treasuries,
            onDismiss = { showNewReceiptSheet = false },
            onSubmit = { partyId, treasuryId, amountMinor, currency, rate, notes ->
                viewModel.postCustomerReceipt(
                    partyId = partyId,
                    treasuryId = treasuryId,
                    amountOrigMinor = amountMinor,
                    currency = currency,
                    exchangeRate = rate,
                    allocations = emptyList(),
                    notes = notes,
                    onSuccess = { showNewReceiptSheet = false }
                )
            }
        )
    }

    // New Payment Sheet
    if (showNewPaymentSheet) {
        NewPaymentBottomSheet(
            parties = parties,
            treasuries = treasuries,
            onDismiss = { showNewPaymentSheet = false },
            onSubmit = { recipientId, treasuryId, amountMinor, currency, rate, paymentType, customExp, notes ->
                viewModel.postPaymentVoucher(
                    recipientPartyId = recipientId,
                    treasuryId = treasuryId,
                    amountOrigMinor = amountMinor,
                    currency = currency,
                    exchangeRate = rate,
                    paymentType = paymentType,
                    customExpenseCode = customExp,
                    invoiceAllocations = emptyList(),
                    notes = notes,
                    onSuccess = { showNewPaymentSheet = false }
                )
            }
        )
    }

    // Voucher Detail Dialog
    selectedVoucherForDetail?.let { voucher ->
        val partyName = partyMap[voucher.partyId]?.name ?: "طرف عام"
        AlertDialog(
            onDismissRequest = { selectedVoucherForDetail = null },
            title = { Text("تفاصيل المستند #${voucher.docNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("النوع: ${voucher.type}")
                    Text("الطرف: $partyName")
                    Text("المبلغ: ${Money(voucher.totalMinor, CurrencyCode.fromString(voucher.currency)).format()}")
                    Text("العملة: ${voucher.currency}")
                    Text("الحالة: ${voucher.status}")
                    if (voucher.notes.isNotBlank()) Text("ملاحظات: ${voucher.notes}")

                    if (voucher.status != "VOIDED") {
                        HorizontalDivider()
                        OutlinedButton(
                            onClick = {
                                voucherToVoid = voucher
                                selectedVoucherForDetail = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SemanticExpenseRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إلغاء السند بقيد عكسي")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedVoucherForDetail = null }) { Text("إغلاق") }
            }
        )
    }

    // Void Dialog
    voucherToVoid?.let { doc ->
        AlertDialog(
            onDismissRequest = { voucherToVoid = null },
            title = { Text("إلغاء السند #${doc.docNumber}", color = SemanticExpenseRed) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("وفق معايير IFRS: لا يمكن حذف السند. سيتم إدراج قيد عكسي تعويضي وإرجاع الأرصدة.")
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
                            viewModel.voidDocument(doc.id, voidReason) {
                                voucherToVoid = null
                                voidReason = ""
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SemanticExpenseRed)
                ) { Text("تأكيد الإلغاء والعكس") }
            },
            dismissButton = { TextButton(onClick = { voucherToVoid = null }) { Text("تراجع") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReceiptBottomSheet(
    parties: List<com.example.data.local.entity.PartyEntity>,
    treasuries: List<com.example.data.local.entity.TreasuryAccountEntity>,
    onDismiss: () -> Unit,
    onSubmit: (partyId: String, treasuryId: String, amountMinor: Long, currency: CurrencyCode, rate: ExchangeRate, notes: String) -> Unit
) {
    var selectedPartyId by remember { mutableStateOf(parties.firstOrNull { it.isCustomer }?.id ?: AppDatabase.WALK_IN_CASH_PARTY_ID) }
    var selectedTreasuryId by remember { mutableStateOf(treasuries.firstOrNull()?.id ?: "TR_MAIN_YER") }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val activeTreasury = treasuries.firstOrNull { it.id == selectedTreasuryId }
    val currency = CurrencyCode.fromString(activeTreasury?.currency ?: "YER")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("سند قبض نقدي جديد", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Text("العميل / الطرف الدافع:")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                parties.filter { it.isCustomer }.take(4).forEach { p ->
                    FilterChip(
                        selected = selectedPartyId == p.id,
                        onClick = { selectedPartyId = p.id },
                        label = { Text(p.name, fontSize = 11.sp) }
                    )
                }
            }

            Text("الصندوق / الخزينة المستلمة:")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                treasuries.forEach { tr ->
                    FilterChip(
                        selected = selectedTreasuryId == tr.id,
                        onClick = { selectedTreasuryId = tr.id },
                        label = { Text("${tr.name} (${tr.currency})", fontSize = 11.sp) }
                    )
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("المبلغ المستلم (${currency.symbol})") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات / رقم الشيك / الحوالة") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val amountMinor = (amountText.toLongOrNull() ?: 0L) * 100L
                    if (amountMinor > 0L) {
                        onSubmit(
                            selectedPartyId,
                            selectedTreasuryId,
                            amountMinor,
                            currency,
                            ExchangeRate.parity(currency),
                            notes
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ترحيل سند القبض", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPaymentBottomSheet(
    parties: List<com.example.data.local.entity.PartyEntity>,
    treasuries: List<com.example.data.local.entity.TreasuryAccountEntity>,
    onDismiss: () -> Unit,
    onSubmit: (recipientId: String, treasuryId: String, amountMinor: Long, currency: CurrencyCode, rate: ExchangeRate, paymentType: PaymentVoucherType, customExp: String?, notes: String) -> Unit
) {
    var paymentType by remember { mutableStateOf(PaymentVoucherType.DIRECT_ISP_SERVICE) }
    var selectedRecipientId by remember { mutableStateOf(parties.firstOrNull { it.isVendor }?.id ?: AppDatabase.WALK_IN_CASH_PARTY_ID) }
    var selectedTreasuryId by remember { mutableStateOf(treasuries.firstOrNull()?.id ?: "TR_MAIN_YER") }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val activeTreasury = treasuries.firstOrNull { it.id == selectedTreasuryId }
    val currency = CurrencyCode.fromString(activeTreasury?.currency ?: "YER")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("سند صرف نقدي جديد", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Text("نوع وغرض الصرف (حساب صريح):", fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Direct ISP 5101 highlighted first!
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (paymentType == PaymentVoucherType.DIRECT_ISP_SERVICE) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { paymentType = PaymentVoucherType.DIRECT_ISP_SERVICE }
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingDown, contentDescription = null, tint = SemanticExpenseRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("5101: اشتراك الإنترنت الرئيسي (Starlink / الألياف)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("تكلفة الخدمة المباشرة للشبكة (موصى به لاشتراكات السيرفر)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = paymentType == PaymentVoucherType.OPERATING_EXPENSE,
                        onClick = { paymentType = PaymentVoucherType.OPERATING_EXPENSE },
                        label = { Text("5201: ديزل وكهرباء") }
                    )
                    FilterChip(
                        selected = paymentType == PaymentVoucherType.VENDOR_SETTLEMENT,
                        onClick = { paymentType = PaymentVoucherType.VENDOR_SETTLEMENT },
                        label = { Text("2101: سداد مورد") }
                    )
                    FilterChip(
                        selected = paymentType == PaymentVoucherType.PARTNER_DRAWINGS,
                        onClick = { paymentType = PaymentVoucherType.PARTNER_DRAWINGS },
                        label = { Text("3201: سحب شريك") }
                    )
                }
            }

            Text("صندوق الدفع:")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                treasuries.forEach { tr ->
                    FilterChip(
                        selected = selectedTreasuryId == tr.id,
                        onClick = { selectedTreasuryId = tr.id },
                        label = { Text("${tr.name} (${tr.currency})", fontSize = 11.sp) }
                    )
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("المبلغ المدفوع (${currency.symbol})") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات / تفاصيل الصرف") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val amountMinor = (amountText.toLongOrNull() ?: 0L) * 100L
                    if (amountMinor > 0L) {
                        onSubmit(
                            selectedRecipientId,
                            selectedTreasuryId,
                            amountMinor,
                            currency,
                            ExchangeRate.parity(currency),
                            paymentType,
                            null,
                            notes
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ترحيل سند الصرف", fontWeight = FontWeight.Bold)
            }
        }
    }
}
