package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ledger.AccountConstants
import com.example.core.model.CurrencyCode
import com.example.core.model.ExchangeRate
import com.example.core.model.Money
import com.example.data.ledger.InvariantCheckResult
import com.example.data.ledger.InvoiceAllocationSpec
import com.example.data.ledger.PaymentVoucherType
import com.example.data.ledger.PurchaseItemSpec
import com.example.data.ledger.SalesItemSpec
import com.example.data.local.AppDatabase
import com.example.data.local.dao.AccountBalanceRow
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AllocationEntity
import com.example.data.local.entity.AssetEntity
import com.example.data.local.entity.CardPackageEntity
import com.example.data.local.entity.CurrencyRateEntity
import com.example.data.local.entity.DocumentEntity
import com.example.data.local.entity.DocumentItemEntity
import com.example.data.local.entity.FiscalPeriodEntity
import com.example.data.local.entity.PartyEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.data.local.entity.TreasuryAccountEntity
import com.example.data.repository.AccountingRepository
import com.example.domain.usecase.AgingReport
import com.example.domain.usecase.BackupRestoreUseCase
import com.example.domain.usecase.BalanceSheetReport
import com.example.domain.usecase.BatchImportUseCase
import com.example.domain.usecase.FinancialStatementsUseCase
import com.example.domain.usecase.ImportBatchReport
import com.example.domain.usecase.IncomeStatementReport
import com.example.domain.usecase.StatementOfAccountReport
import com.example.domain.usecase.StatementOfAccountUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardSummary(
    val totalSalesPeriodMinor: Long = 0L,
    val totalReceiptsPeriodMinor: Long = 0L,
    val totalExpensesPeriodMinor: Long = 0L,
    val netCashFlowMinor: Long = 0L,
    val totalReceivablesMinor: Long = 0L,
    val totalPayablesMinor: Long = 0L,
    val treasuryBalancesByCurrency: Map<String, Long> = emptyMap(),
    val openInvoiceCount: Int = 0,
    val isBalanced: Boolean = true
)

enum class PeriodFilter(val title: String) {
    TODAY("اليوم"),
    THIS_WEEK("هذا الأسبوع"),
    THIS_MONTH("هذا الشهر"),
    ALL_TIME("الكل")
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    val db = AppDatabase.getInstance(application)
    val repository = AccountingRepository(db)
    val writer = repository.ledgerWriter
    val invariants = repository.invariants

    val statementsUseCase = FinancialStatementsUseCase(db)
    val statementOfAccountUseCase = StatementOfAccountUseCase(db)
    val backupRestoreUseCase = BackupRestoreUseCase(db)
    val batchImportUseCase = BatchImportUseCase(db, writer)

    // Base Flows
    val allParties: StateFlow<List<PartyEntity>> = repository.allParties
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPackages: StateFlow<List<CardPackageEntity>> = repository.allPackages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTreasuries: StateFlow<List<TreasuryAccountEntity>> = repository.allTreasuries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDocuments: StateFlow<List<DocumentEntity>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAllocations: StateFlow<List<AllocationEntity>> = db.allocationDao().getAllActiveAllocationsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAssets: StateFlow<List<AssetEntity>> = repository.allAssets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPeriods: StateFlow<List<FiscalPeriodEntity>> = db.fiscalPeriodDao().getAllPeriodsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRates: StateFlow<List<CurrencyRateEntity>> = db.currencyRateDao().getAllRatesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStockMovements: StateFlow<List<StockMovementEntity>> = db.cardPackageDao().getAllStockMovementsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trialBalance: StateFlow<List<AccountBalanceRow>> = repository.trialBalance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAccounts: StateFlow<List<AccountEntity>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard State
    val selectedPeriodFilter = MutableStateFlow(PeriodFilter.THIS_MONTH)

    private val _dashboardSummary = MutableStateFlow(DashboardSummary())
    val dashboardSummary: StateFlow<DashboardSummary> = _dashboardSummary.asStateFlow()

    // Generated Reports State
    private val _incomeStatement = MutableStateFlow<IncomeStatementReport?>(null)
    val incomeStatement: StateFlow<IncomeStatementReport?> = _incomeStatement.asStateFlow()

    private val _balanceSheet = MutableStateFlow<BalanceSheetReport?>(null)
    val balanceSheet: StateFlow<BalanceSheetReport?> = _balanceSheet.asStateFlow()

    private val _agingReport = MutableStateFlow<AgingReport?>(null)
    val agingReport: StateFlow<AgingReport?> = _agingReport.asStateFlow()

    private val _currentPartyStatement = MutableStateFlow<StatementOfAccountReport?>(null)
    val currentPartyStatement: StateFlow<StatementOfAccountReport?> = _currentPartyStatement.asStateFlow()

    private val _invariantResult = MutableStateFlow<InvariantCheckResult?>(null)
    val invariantResult: StateFlow<InvariantCheckResult?> = _invariantResult.asStateFlow()

    // UI Feedback Messages
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    init {
        refreshDashboard()
        runInvariantCheck()
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            val todayEpoch = System.currentTimeMillis() / 86400000L
            val filter = selectedPeriodFilter.value

            val startEpoch: Long? = when (filter) {
                PeriodFilter.TODAY -> todayEpoch
                PeriodFilter.THIS_WEEK -> todayEpoch - 7
                PeriodFilter.THIS_MONTH -> todayEpoch - 30
                PeriodFilter.ALL_TIME -> null
            }

            val income = statementsUseCase.generateIncomeStatement(startEpoch, todayEpoch)
            val receivables = repository.getNetBalanceForAccount(AccountConstants.ACCOUNTS_RECEIVABLE)
            val payables = -repository.getNetBalanceForAccount(AccountConstants.ACCOUNTS_PAYABLE)

            // Treasury balances
            val treasuries = db.treasuryDao().getAllTreasuriesSync()
            val trMap = mutableMapOf<String, Long>()
            treasuries.forEach { tr ->
                val bal = repository.getTreasuryBalance(tr.id)
                trMap[tr.currency] = (trMap[tr.currency] ?: 0L) + bal
            }

            // Invariant check
            val invRes = repository.runInvariantCheck()
            _invariantResult.value = invRes

            _dashboardSummary.value = DashboardSummary(
                totalSalesPeriodMinor = income.netRevenueMinor,
                totalReceiptsPeriodMinor = 0L, // Derived
                totalExpensesPeriodMinor = income.directIspCostMinor + income.totalOperatingExpensesMinor,
                netCashFlowMinor = income.netProfitMinor,
                totalReceivablesMinor = receivables,
                totalPayablesMinor = payables,
                treasuryBalancesByCurrency = trMap,
                openInvoiceCount = 0,
                isBalanced = invRes.isValid
            )
        }
    }

    fun setPeriodFilter(filter: PeriodFilter) {
        selectedPeriodFilter.value = filter
        refreshDashboard()
    }

    fun runInvariantCheck() {
        viewModelScope.launch {
            val result = repository.runInvariantCheck()
            _invariantResult.value = result
        }
    }

    // --- Action Methods ---

    fun postQuickSale(
        partyId: String,
        packageId: String?,
        description: String,
        quantity: Int,
        unitPriceMinor: Long,
        cashPaidMinor: Long,
        treasuryId: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val today = System.currentTimeMillis() / 86400000L
                writer.postQuickSale(
                    partyId = partyId,
                    packageId = packageId,
                    description = description,
                    quantity = quantity,
                    unitPriceMinor = unitPriceMinor,
                    cashPaidMinor = cashPaidMinor,
                    treasuryId = treasuryId,
                    fiscalYear = 2026,
                    dateEpochDay = today
                )
                _userMessage.emit("تم تسجيل البيع السريع وترحيله بنجاح")
                refreshDashboard()
                onSuccess()
            } catch (e: Exception) {
                _userMessage.emit("خطأ في البيع السريع: ${e.message}")
            }
        }
    }

    fun postSalesInvoice(
        partyId: String,
        cardItems: List<SalesItemSpec>,
        serviceItems: List<SalesItemSpec>,
        currency: CurrencyCode,
        exchangeRate: ExchangeRate,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val today = System.currentTimeMillis() / 86400000L
                writer.postSalesInvoice(
                    partyId = partyId,
                    fiscalYear = 2026,
                    dateEpochDay = today,
                    currency = currency,
                    exchangeRate = exchangeRate,
                    cardItems = cardItems,
                    serviceItems = serviceItems,
                    notes = notes
                )
                _userMessage.emit("تم ترحيل فاتورة المبيعات بنجاح")
                refreshDashboard()
                onSuccess()
            } catch (e: Exception) {
                _userMessage.emit("فشل إصدار الفاتورة: ${e.message}")
            }
        }
    }

    fun postCustomerReceipt(
        partyId: String,
        treasuryId: String,
        amountOrigMinor: Long,
        currency: CurrencyCode,
        exchangeRate: ExchangeRate,
        allocations: List<InvoiceAllocationSpec>,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val today = System.currentTimeMillis() / 86400000L
                writer.postCustomerReceipt(
                    partyId = partyId,
                    treasuryId = treasuryId,
                    fiscalYear = 2026,
                    dateEpochDay = today,
                    amountOrigMinor = amountOrigMinor,
                    currency = currency,
                    exchangeRate = exchangeRate,
                    allocations = allocations,
                    notes = notes
                )
                _userMessage.emit("تم ترحيل سند القبض وتخصيصه بنجاح")
                refreshDashboard()
                onSuccess()
            } catch (e: Exception) {
                _userMessage.emit("فشل تسجيل سند القبض: ${e.message}")
            }
        }
    }

    fun postPaymentVoucher(
        recipientPartyId: String,
        treasuryId: String,
        amountOrigMinor: Long,
        currency: CurrencyCode,
        exchangeRate: ExchangeRate,
        paymentType: PaymentVoucherType,
        customExpenseCode: String? = null,
        invoiceAllocations: List<InvoiceAllocationSpec> = emptyList(),
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val today = System.currentTimeMillis() / 86400000L
                writer.postPaymentVoucher(
                    recipientPartyId = recipientPartyId,
                    treasuryId = treasuryId,
                    fiscalYear = 2026,
                    dateEpochDay = today,
                    amountOrigMinor = amountOrigMinor,
                    currency = currency,
                    exchangeRate = exchangeRate,
                    paymentType = paymentType,
                    customExpenseCode = customExpenseCode,
                    invoiceAllocations = invoiceAllocations,
                    notes = notes
                )
                _userMessage.emit("تم ترحيل سند الصرف بنجاح")
                refreshDashboard()
                onSuccess()
            } catch (e: Exception) {
                _userMessage.emit("فشل تسجيل سند الصرف: ${e.message}")
            }
        }
    }

    fun postPurchaseInvoice(
        vendorPartyId: String,
        currency: CurrencyCode,
        exchangeRate: ExchangeRate,
        items: List<PurchaseItemSpec>,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val today = System.currentTimeMillis() / 86400000L
                writer.postPurchaseInvoice(
                    vendorPartyId = vendorPartyId,
                    fiscalYear = 2026,
                    dateEpochDay = today,
                    currency = currency,
                    exchangeRate = exchangeRate,
                    items = items,
                    notes = notes
                )
                _userMessage.emit("تم ترحيل فاتورة المشتريات وتسجيل الأصول إن وُجدت")
                refreshDashboard()
                onSuccess()
            } catch (e: Exception) {
                _userMessage.emit("فشل تسجيل فاتورة المشتريات: ${e.message}")
            }
        }
    }

    fun postCreditNote(
        partyId: String,
        amountOrigMinor: Long,
        currency: CurrencyCode,
        exchangeRate: ExchangeRate,
        returnPackageId: String?,
        returnQty: Int,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val today = System.currentTimeMillis() / 86400000L
                writer.postCreditNote(
                    partyId = partyId,
                    fiscalYear = 2026,
                    dateEpochDay = today,
                    currency = currency,
                    exchangeRate = exchangeRate,
                    amountOrigMinor = amountOrigMinor,
                    returnPackageId = returnPackageId,
                    returnQty = returnQty,
                    notes = notes
                )
                _userMessage.emit("تم ترحيل الإشعار الدائن وإعادة الكميات إن وُجدت")
                refreshDashboard()
                onSuccess()
            } catch (e: Exception) {
                _userMessage.emit("فشل ترحيل الإشعار الدائن: ${e.message}")
            }
        }
    }

    fun postTreasuryTransfer(
        sourceTreasuryId: String,
        sourceAmountOrigMinor: Long,
        sourceCurrency: CurrencyCode,
        sourceRate: ExchangeRate,
        destTreasuryId: String,
        destAmountOrigMinor: Long,
        destCurrency: CurrencyCode,
        destRate: ExchangeRate,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val today = System.currentTimeMillis() / 86400000L
                writer.postTreasuryTransfer(
                    sourceTreasuryId = sourceTreasuryId,
                    sourceAmountOrigMinor = sourceAmountOrigMinor,
                    sourceCurrency = sourceCurrency,
                    sourceRate = sourceRate,
                    destTreasuryId = destTreasuryId,
                    destAmountOrigMinor = destAmountOrigMinor,
                    destCurrency = destCurrency,
                    destRate = destRate,
                    fiscalYear = 2026,
                    dateEpochDay = today,
                    notes = notes
                )
                _userMessage.emit("تم التحويل بين الخزائن وتسجيل فروق الصرف بنجاح")
                refreshDashboard()
                onSuccess()
            } catch (e: Exception) {
                _userMessage.emit("فشل التحويل: ${e.message}")
            }
        }
    }

    fun runDepreciation(assetId: String, year: Int, month: Int) {
        viewModelScope.launch {
            try {
                val today = System.currentTimeMillis() / 86400000L
                val success = writer.runMonthlyDepreciation(assetId, year, month, today)
                if (success) {
                    _userMessage.emit("تم ترحيل قيد الإهلاك الشهري للأصل بنجاح")
                } else {
                    _userMessage.emit("تنبيه: تم إهلاك هذا الأصل مسبقاً لهذه الفترة أو تم استبعاده بالكامل")
                }
                refreshDashboard()
            } catch (e: Exception) {
                _userMessage.emit("فشل الإهلاك: ${e.message}")
            }
        }
    }

    fun disposeAsset(
        assetId: String,
        salvageProceedsMinor: Long,
        treasuryId: String?,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val today = System.currentTimeMillis() / 86400000L
                writer.disposeAsset(
                    assetId = assetId,
                    disposalDateEpochDay = today,
                    salvageProceedsMinor = salvageProceedsMinor,
                    treasuryId = treasuryId,
                    notes = notes
                )
                _userMessage.emit("تم استبعاد الأصل وترحيل قيود التخريد والأرباح/الخسائر الرأسمالية بنجاح")
                refreshDashboard()
                onSuccess()
            } catch (e: Exception) {
                _userMessage.emit("فشل استبعاد الأصل: ${e.message}")
            }
        }
    }

    fun voidDocument(docId: String, reason: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val today = System.currentTimeMillis() / 86400000L
                val success = writer.voidDocument(docId, today, reason)
                if (success) {
                    _userMessage.emit("تم إلغاء المستند وإدراج قيد عكسي تعويضي بنجاح")
                    refreshDashboard()
                    onSuccess()
                } else {
                    _userMessage.emit("المستند ملغي مسبقاً")
                }
            } catch (e: Exception) {
                _userMessage.emit("فشل الإلغاء: ${e.message}")
            }
        }
    }

    fun receiveCardStock(packageId: String, quantity: Int, notes: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val today = System.currentTimeMillis() / 86400000L
                writer.receiveCardStock(packageId, quantity, today, notes)
                _userMessage.emit("تم تسجيل استلام دفعة الكروت وزيادة الرصيد")
                onSuccess()
            } catch (e: Exception) {
                _userMessage.emit("فشل استلام الكروت: ${e.message}")
            }
        }
    }

    fun adjustCardStock(packageId: String, adjustmentQty: Int, reason: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val today = System.currentTimeMillis() / 86400000L
                writer.adjustCardStock(packageId, adjustmentQty, today, reason)
                _userMessage.emit("تم تسجيل تسوية رصيد الكروت")
                onSuccess()
            } catch (e: Exception) {
                _userMessage.emit("فشل تسوية الرصيد: ${e.message}")
            }
        }
    }

    fun reconcileTreasuryCash(treasuryId: String, actualCountMinor: Long, notes: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val today = System.currentTimeMillis() / 86400000L
                val doc = writer.reconcileTreasuryCash(treasuryId, actualCountMinor, 2026, today, notes)
                if (doc != null) {
                    _userMessage.emit("تم تسجيل قيد تسوية فارق جرد الصندوق بنجاح")
                } else {
                    _userMessage.emit("الرصيد الفعلي مطابق تماماً للرصيد الدفتري، لا يلزم قيد")
                }
                refreshDashboard()
                onSuccess()
            } catch (e: Exception) {
                _userMessage.emit("فشل تسوية جرد الخزينة: ${e.message}")
            }
        }
    }

    fun executeYearEndClosing(fiscalYear: Int, memo: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val closingDate = 20365L // End of year
                writer.executeYearEndClosing(fiscalYear, closingDate, memo)
                _userMessage.emit("تم الإقفال السنوي وترحيل الأرباح والخسائر لحساب الأرباح المرحّلة 3301")
                refreshDashboard()
                onSuccess()
            } catch (e: Exception) {
                _userMessage.emit("فشل الإقفال السنوي: ${e.message}")
            }
        }
    }

    fun setPeriodClosed(year: Int, month: Int, isClosed: Boolean) {
        viewModelScope.launch {
            try {
                db.fiscalPeriodDao().setPeriodClosed(year, month, isClosed, if (isClosed) System.currentTimeMillis() else null)
                _userMessage.emit("تم ${if (isClosed) "قفل" else "فتح"} الفترة المالية بنجاح")
            } catch (e: Exception) {
                _userMessage.emit("فشل تعديل حالة الفترة: ${e.message}")
            }
        }
    }

    fun loadStatementOfAccount(partyId: String, controlAccountCode: String, startEpoch: Long?, endEpoch: Long?) {
        viewModelScope.launch {
            try {
                val report = statementOfAccountUseCase.generateStatement(partyId, controlAccountCode, startEpoch, endEpoch)
                _currentPartyStatement.value = report
            } catch (e: Exception) {
                _userMessage.emit("فشل توليد كشف الحساب: ${e.message}")
            }
        }
    }

    fun loadIncomeStatement(startEpoch: Long?, endEpoch: Long?) {
        viewModelScope.launch {
            val rep = statementsUseCase.generateIncomeStatement(startEpoch, endEpoch)
            _incomeStatement.value = rep
        }
    }

    fun loadBalanceSheet(asOfDateEpoch: Long) {
        viewModelScope.launch {
            val rep = statementsUseCase.generateBalanceSheet(asOfDateEpoch)
            _balanceSheet.value = rep
        }
    }

    fun loadAgingReport(asOfDateEpoch: Long) {
        viewModelScope.launch {
            val rep = statementsUseCase.generateAgingReport(asOfDateEpoch)
            _agingReport.value = rep
        }
    }

    fun insertParty(party: PartyEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.insertParty(party)
                _userMessage.emit("تمت إضافة الطرف بنجاح")
                onSuccess()
            } catch (e: Exception) {
                _userMessage.emit("فشل إضافة الطرف: ${e.message}")
            }
        }
    }

    fun setPartyActive(partyId: String, isActive: Boolean) {
        viewModelScope.launch {
            db.partyDao().setPartyActive(partyId, isActive)
            _userMessage.emit(if (isActive) "تم تنشيط الطرف" else "تمت أرشفة الطرف")
        }
    }

    fun insertPackage(pkg: CardPackageEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.insertPackage(pkg)
                _userMessage.emit("تم حفظ الباقة بنجاح")
                onSuccess()
            } catch (e: Exception) {
                _userMessage.emit("فشل حفظ الباقة: ${e.message}")
            }
        }
    }

    fun setPackageActive(packageId: String, isActive: Boolean) {
        viewModelScope.launch {
            db.cardPackageDao().setPackageActive(packageId, isActive)
            _userMessage.emit(if (isActive) "تم تنشيط الباقة" else "تمت أرشفة الباقة")
        }
    }

    fun insertTreasury(treasury: TreasuryAccountEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.insertTreasury(treasury)
                _userMessage.emit("تمت إضافة الخزينة بنجاح")
                onSuccess()
            } catch (e: Exception) {
                _userMessage.emit("فشل إضافة الخزينة: ${e.message}")
            }
        }
    }

    fun exportBackup(onExported: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = backupRestoreUseCase.exportDatabaseToJson()
                onExported(json)
                _userMessage.emit("تم تصدير النسخة الاحتياطية بنجاح")
            } catch (e: Exception) {
                _userMessage.emit("فشل تصدير النسخة: ${e.message}")
            }
        }
    }

    fun restoreBackup(json: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val res = backupRestoreUseCase.restoreDatabaseFromJson(json)
                if (res.isSuccess) {
                    _userMessage.emit(res.getOrNull() ?: "تمت الاستعادة بنجاح")
                    refreshDashboard()
                    onComplete()
                } else {
                    _userMessage.emit("فشل استعادة النسخة: ${res.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _userMessage.emit("خطأ أثناء الاستعادة: ${e.message}")
            }
        }
    }

    fun importInvoicesBatch(jsonString: String, onReport: (ImportBatchReport) -> Unit) {
        viewModelScope.launch {
            try {
                val report = batchImportUseCase.importInvoicesBatch(jsonString)
                onReport(report)
                _userMessage.emit("اكتمل الاستيراد: نجح ${report.successCount}، تخطي ${report.duplicateSkippedCount}، أخطاء ${report.errorCount}")
                refreshDashboard()
            } catch (e: Exception) {
                _userMessage.emit("فشل استيراد الدفعة: ${e.message}")
            }
        }
    }
}
