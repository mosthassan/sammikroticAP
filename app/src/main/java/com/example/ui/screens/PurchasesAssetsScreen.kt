package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ledger.AccountConstants
import com.example.core.model.CurrencyCode
import com.example.core.model.ExchangeRate
import com.example.core.model.Money
import com.example.data.ledger.PurchaseItemSpec
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AssetEntity
import com.example.data.local.entity.DocumentEntity
import com.example.ui.components.AmountSemanticType
import com.example.ui.components.AmountText
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusChip
import com.example.ui.theme.SemanticExpenseRed
import com.example.ui.theme.SemanticIncomeGreen
import com.example.ui.viewmodel.AppViewModel

@Composable
fun PurchasesAssetsScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val documents by viewModel.allDocuments.collectAsState()
    val assets by viewModel.allAssets.collectAsState()
    val parties by viewModel.allParties.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Purchases, 1: Fixed Assets Register
    var showNewPurchaseSheet by remember { mutableStateOf(false) }
    var selectedAssetForDeprecate by remember { mutableStateOf<AssetEntity?>(null) }
    var selectedAssetForDisposal by remember { mutableStateOf<AssetEntity?>(null) }
    var disposalProceedsText by remember { mutableStateOf("0") }

    val partyMap = remember(parties) { parties.associateBy { it.id } }
    val purchaseInvoices = remember(documents) { documents.filter { it.type == "PURCHASE_INVOICE" } }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showNewPurchaseSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("fab_new_purchase")
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("فاتورة مشتريات", fontWeight = FontWeight.Bold)
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
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("فواتير المشتريات") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("سجل الأصول ومعدات الشبكة") })
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedTab == 0) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    if (purchaseInvoices.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text("لا توجد فواتير مشتريات مسجلة")
                            }
                        }
                    }

                    items(purchaseInvoices) { inv ->
                        val vendorName = partyMap[inv.partyId]?.name ?: "مورد عام"
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("فاتورة مشتريات #${inv.docNumber}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        StatusChip(status = inv.status)
                                    }
                                    Text("المورد: $vendorName", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    if (inv.notes.isNotBlank()) Text(inv.notes, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                AmountText(
                                    money = Money(inv.totalMinor, CurrencyCode.fromString(inv.currency)),
                                    semanticType = AmountSemanticType.EXPENSE,
                                    fontSize = 14
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    if (assets.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text("لا توجد أصول مسجلة في سجل الأصول الثابتة")
                            }
                        }
                    }

                    items(assets) { asset ->
                        val netBookValue = asset.purchaseCostMinor - asset.accumulatedDepreciationMinor
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Router, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(asset.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Text("صافي القيمة: ${Money(netBookValue, CurrencyCode.FUNCTIONAL).format()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SemanticIncomeGreen)
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("التكلفة: ${Money(asset.purchaseCostMinor, CurrencyCode.FUNCTIONAL).format()}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    Text("مجمع الإهلاك: ${Money(asset.accumulatedDepreciationMinor, CurrencyCode.FUNCTIONAL).format()}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = SemanticExpenseRed)
                                    Text("العمر: ${asset.usefulLifeMonths} شهر", fontSize = 11.sp)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { selectedAssetForDeprecate = asset },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تشغيل الإهلاك الشهري", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { selectedAssetForDisposal = asset },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SemanticExpenseRed),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("استبعاد/بيع الأصل", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // New Purchase Sheet
    if (showNewPurchaseSheet) {
        NewPurchaseBottomSheet(
            parties = parties,
            onDismiss = { showNewPurchaseSheet = false },
            onSubmit = { vendorId, currency, rate, items, notes ->
                viewModel.postPurchaseInvoice(
                    vendorPartyId = vendorId,
                    currency = currency,
                    exchangeRate = rate,
                    items = items,
                    notes = notes,
                    onSuccess = { showNewPurchaseSheet = false }
                )
            }
        )
    }

    // Depreciation Dialog
    selectedAssetForDeprecate?.let { ast ->
        AlertDialog(
            onDismissRequest = { selectedAssetForDeprecate = null },
            title = { Text("تشغيل إهلاك الأصل: ${ast.name}") },
            text = {
                val monthlyAmount = (ast.purchaseCostMinor - ast.salvageValueMinor) / ast.usefulLifeMonths.coerceAtLeast(1)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("القسط الشهري المحسوب: ${Money(monthlyAmount, CurrencyCode.FUNCTIONAL).format()}")
                    Text("سيتم ترحيل القيد فوراً (مدين 5203، دائن 1599). التكرار للشهر نفسه محمي بـ Idempotency.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.runDepreciation(ast.id, 2026, 1)
                        selectedAssetForDeprecate = null
                    }
                ) { Text("اعتماد وترحيل الإهلاك") }
            },
            dismissButton = { TextButton(onClick = { selectedAssetForDeprecate = null }) { Text("إلغاء") } }
        )
    }

    // Disposal Dialog
    selectedAssetForDisposal?.let { ast ->
        AlertDialog(
            onDismissRequest = { selectedAssetForDisposal = null },
            title = { Text("استبعاد/بيع الأصل: ${ast.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل عائد البيع النقدي إن وجد (0 للتخريد):")
                    OutlinedTextField(
                        value = disposalProceedsText,
                        onValueChange = { disposalProceedsText = it },
                        label = { Text("عائد البيع (ر.ي)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val proceeds = (disposalProceedsText.toLongOrNull() ?: 0L) * 100L
                        viewModel.disposeAsset(
                            assetId = ast.id,
                            salvageProceedsMinor = proceeds,
                            treasuryId = if (proceeds > 0) "TR_MAIN_YER" else null,
                            notes = "استبعاد أصل وتخريد"
                        ) {
                            selectedAssetForDisposal = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SemanticExpenseRed)
                ) { Text("تأكيد الاستبعاد") }
            },
            dismissButton = { TextButton(onClick = { selectedAssetForDisposal = null }) { Text("تراجع") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPurchaseBottomSheet(
    parties: List<com.example.data.local.entity.PartyEntity>,
    onDismiss: () -> Unit,
    onSubmit: (vendorId: String, currency: CurrencyCode, rate: ExchangeRate, items: List<PurchaseItemSpec>, notes: String) -> Unit
) {
    var selectedVendorId by remember { mutableStateOf(parties.firstOrNull { it.isVendor }?.id ?: AppDatabase.WALK_IN_CASH_PARTY_ID) }
    var selectedCurrency by remember { mutableStateOf(CurrencyCode.USD) }
    var exchangeRateText by remember { mutableStateOf("530") }
    var itemDesc by remember { mutableStateOf("") }
    var itemQtyText by remember { mutableStateOf("1") }
    var itemPriceText by remember { mutableStateOf("100") }
    var isFixedAsset by remember { mutableStateOf(true) }
    var usefulMonthsText by remember { mutableStateOf("24") }
    var notes by remember { mutableStateOf("") }

    val itemsList = remember { androidx.compose.runtime.mutableStateListOf<PurchaseItemSpec>() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("فاتورة مشتريات وتجهيز شبكة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Text("المورد:")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                parties.filter { it.isVendor }.take(3).forEach { v ->
                    FilterChip(selected = selectedVendorId == v.id, onClick = { selectedVendorId = v.id }, label = { Text(v.name) })
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.weight(1f)) {
                    FilterChip(selected = selectedCurrency == CurrencyCode.USD, onClick = { selectedCurrency = CurrencyCode.USD }, label = { Text("USD ($)") })
                    Spacer(modifier = Modifier.width(4.dp))
                    FilterChip(selected = selectedCurrency == CurrencyCode.YER, onClick = { selectedCurrency = CurrencyCode.YER }, label = { Text("YER (ر.ي)") })
                }
                if (selectedCurrency != CurrencyCode.FUNCTIONAL) {
                    OutlinedTextField(
                        value = exchangeRateText,
                        onValueChange = { exchangeRateText = it },
                        label = { Text("سعر الصرف (YER/USD)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            SectionHeader(title = "إضافة بند مشتريات")
            OutlinedTextField(value = itemDesc, onValueChange = { itemDesc = it }, label = { Text("اسم الجهاز أو المادة (مثال: راوتر MikroTik CCR)") }, modifier = Modifier.fillMaxWidth())

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = itemQtyText, onValueChange = { itemQtyText = it }, label = { Text("الكمية") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = itemPriceText, onValueChange = { itemPriceText = it }, label = { Text("السعر بالـ ${selectedCurrency.name}") }, modifier = Modifier.weight(1.5f))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isFixedAsset, onCheckedChange = { isFixedAsset = it })
                Text("أصل شبكة ثابت (يُدرج تلقائياً في سجل الأصول 1501)")
            }

            if (isFixedAsset) {
                OutlinedTextField(value = usefulMonthsText, onValueChange = { usefulMonthsText = it }, label = { Text("العمر الافتراضي (بالأشهر)") }, modifier = Modifier.fillMaxWidth())
            }

            Button(
                onClick = {
                    val q = itemQtyText.toIntOrNull() ?: 1
                    val p = (itemPriceText.toLongOrNull() ?: 0L) * 100L
                    val m = usefulMonthsText.toIntOrNull() ?: 24
                    val code = if (isFixedAsset) AccountConstants.FIXED_ASSETS_NETWORK else AccountConstants.OPERATING_EXPENSES
                    itemsList.add(PurchaseItemSpec(itemDesc.ifBlank { "معدات شبكة" }, code, q, p, isFixedAsset, m))
                    itemDesc = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إضافة البند للفاتورة")
            }

            itemsList.forEachIndexed { idx, itm ->
                Text("• ${itm.description}: ${itm.quantity} × ${itm.unitPriceMinor / 100L} ${selectedCurrency.name}")
            }

            Button(
                onClick = {
                    if (itemsList.isNotEmpty()) {
                        val rateMicros = (exchangeRateText.toLongOrNull() ?: 530L) * 1_000_000L
                        val rate = if (selectedCurrency == CurrencyCode.FUNCTIONAL) ExchangeRate.parity(CurrencyCode.FUNCTIONAL) else ExchangeRate(selectedCurrency, CurrencyCode.FUNCTIONAL, rateMicros)
                        onSubmit(selectedVendorId, selectedCurrency, rate, itemsList.toList(), notes)
                    }
                },
                enabled = itemsList.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ترحيل فاتورة المشتريات", fontWeight = FontWeight.Bold)
            }
        }
    }
}
