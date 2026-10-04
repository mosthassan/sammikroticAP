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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.core.model.CurrencyCode
import com.example.core.model.Money
import com.example.core.model.UuidUtils
import com.example.data.local.entity.CardPackageEntity
import com.example.ui.components.AmountSemanticType
import com.example.ui.components.AmountText
import com.example.ui.components.SectionHeader
import com.example.ui.theme.SemanticExpenseRed
import com.example.ui.theme.SemanticIncomeGreen
import com.example.ui.theme.SemanticWarningAmber
import com.example.ui.viewmodel.AppViewModel

@Composable
fun PackagesStockScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val packages by viewModel.allPackages.collectAsState()
    val movements by viewModel.allStockMovements.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Packages & Balances, 1: Stock Movements Log
    var showAddPackageSheet by remember { mutableStateOf(false) }
    var showReceiveStockSheet by remember { mutableStateOf(false) }
    var showAdjustStockSheet by remember { mutableStateOf(false) }

    val packageMap = remember(packages) { packages.associateBy { it.id } }

    Scaffold(
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FloatingActionButton(
                    onClick = { showReceiveStockSheet = true },
                    containerColor = SemanticIncomeGreen,
                    modifier = Modifier.testTag("fab_receive_stock")
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Inventory, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("استلام كروت", fontWeight = FontWeight.Bold)
                    }
                }

                FloatingActionButton(
                    onClick = { showAddPackageSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("fab_add_package")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "باقة جديدة")
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
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("أرصدة الباقات والكروت") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("سجل حركات المخزون") })
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedTab == 0) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (packages.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text("لا توجد باقات كروت مضافة. أضف باقة جديدة للبدء.")
                            }
                        }
                    }

                    items(packages) { pkg ->
                        var stockBalance by remember { mutableIntStateOf(0) }

                        LaunchedEffect(pkg.id, movements) {
                            stockBalance = viewModel.db.cardPackageDao().getStockBalance(pkg.id)
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pkg.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("المدة / السعة: ${pkg.durationOrQuota}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("جملة: ${Money(pkg.wholesalePriceMinor, CurrencyCode.YER).format()}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        Text("تجزئة: ${Money(pkg.retailPriceMinor, CurrencyCode.YER).format()}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Surface(
                                        color = if (stockBalance > 10) SemanticIncomeGreen.copy(alpha = 0.15f) else SemanticWarningAmber.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "$stockBalance كرت",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (stockBalance > 10) SemanticIncomeGreen else SemanticWarningAmber,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    TextButton(onClick = { showAdjustStockSheet = true }) {
                                        Text("تسوية جرد", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(movements) { m ->
                        val pkgName = packageMap[m.packageId]?.name ?: "باقة غير معروفة"
                        val (color, title) = when (m.type) {
                            "RECEIVE" -> SemanticIncomeGreen to "استلام دفعة (+)"
                            "SELL" -> Color(0xFF3B82F6) to "بيع كروت (-)"
                            "RETURN" -> SemanticWarningAmber to "مرتجع (+)"
                            "ADJUST" -> Color.Gray to "تسوية جرد"
                            else -> Color.Gray to m.type
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(pkgName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("نوع الحركة: $title | التاريخ: ${m.movementDateEpochDay}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "${if (m.type in listOf("RECEIVE", "RETURN")) "+" else "-"}${m.quantity} كرت",
                                    color = color,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Package Sheet
    if (showAddPackageSheet) {
        AddPackageBottomSheet(
            onDismiss = { showAddPackageSheet = false },
            onSubmit = { name, quota, wholesale, retail ->
                val pkg = CardPackageEntity(
                    id = UuidUtils.newTimeOrderedId(),
                    name = name,
                    durationOrQuota = quota,
                    wholesalePriceMinor = wholesale,
                    retailPriceMinor = retail
                )
                viewModel.insertPackage(pkg) { showAddPackageSheet = false }
            }
        )
    }

    // Receive Stock Sheet
    if (showReceiveStockSheet) {
        ReceiveStockBottomSheet(
            packages = packages,
            onDismiss = { showReceiveStockSheet = false },
            onSubmit = { pkgId, qty, notes ->
                viewModel.receiveCardStock(pkgId, qty, notes) { showReceiveStockSheet = false }
            }
        )
    }

    // Adjust Stock Sheet
    if (showAdjustStockSheet) {
        AdjustStockBottomSheet(
            packages = packages,
            onDismiss = { showAdjustStockSheet = false },
            onSubmit = { pkgId, adjQty, reason ->
                viewModel.adjustCardStock(pkgId, adjQty, reason) { showAdjustStockSheet = false }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPackageBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: (name: String, quota: String, wholesaleMinor: Long, retailMinor: Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quota by remember { mutableStateOf("") }
    var wholesaleText by remember { mutableStateOf("") }
    var retailText by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("إضافة باقة كروت إنترنت جديدة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الباقة (مثال: باقة اليوم الواحد)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = quota, onValueChange = { quota = it }, label = { Text("السعة / المدة (مثال: 1GB / 24 ساعة)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = wholesaleText, onValueChange = { wholesaleText = it }, label = { Text("سعر الجملة للبقالات (ر.ي)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = retailText, onValueChange = { retailText = it }, label = { Text("سعر التجزئة المقترح للمستخدم (ر.ي)") }, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val w = (wholesaleText.toLongOrNull() ?: 0L) * 100L
                        val r = (retailText.toLongOrNull() ?: 0L) * 100L
                        onSubmit(name, quota, w, r)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("حفظ الباقة", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveStockBottomSheet(
    packages: List<CardPackageEntity>,
    onDismiss: () -> Unit,
    onSubmit: (packageId: String, quantity: Int, notes: String) -> Unit
) {
    var selectedPackageId by remember { mutableStateOf(packages.firstOrNull()?.id ?: "") }
    var qtyText by remember { mutableStateOf("100") }
    var notes by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("استلام دفعة كروت مطبوعة (زيادة الرصيد)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Text("اختر الباقة المستلمة:")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                packages.take(4).forEach { pkg ->
                    FilterChip(
                        selected = selectedPackageId == pkg.id,
                        onClick = { selectedPackageId = pkg.id },
                        label = { Text(pkg.name) }
                    )
                }
            }

            OutlinedTextField(value = qtyText, onValueChange = { qtyText = it }, label = { Text("الكمية المستلمة (عدد الكروت)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("ملاحظات / رقم الدفعة أو المطبعة") }, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = {
                    val q = qtyText.toIntOrNull() ?: 0
                    if (selectedPackageId.isNotBlank() && q > 0) {
                        onSubmit(selectedPackageId, q, notes)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("تأكيد استلام الكمية", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustStockBottomSheet(
    packages: List<CardPackageEntity>,
    onDismiss: () -> Unit,
    onSubmit: (packageId: String, adjustmentQty: Int, reason: String) -> Unit
) {
    var selectedPackageId by remember { mutableStateOf(packages.firstOrNull()?.id ?: "") }
    var adjText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("تسوية جرد كروت", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Text("الباقة المراد تسويتها:")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                packages.take(4).forEach { pkg ->
                    FilterChip(
                        selected = selectedPackageId == pkg.id,
                        onClick = { selectedPackageId = pkg.id },
                        label = { Text(pkg.name) }
                    )
                }
            }

            OutlinedTextField(value = adjText, onValueChange = { adjText = it }, label = { Text("الكمية المضافة (+) أو المخصومة (-)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("سبب التسوية (إلزامي)") }, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = {
                    val q = adjText.toIntOrNull() ?: 0
                    if (q != 0 && reason.isNotBlank()) {
                        onSubmit(selectedPackageId, q, reason)
                    }
                },
                enabled = reason.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("اعتماد تسوية الرصيد", fontWeight = FontWeight.Bold)
            }
        }
    }
}
