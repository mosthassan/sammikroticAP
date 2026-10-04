package com.example.core.ledger

import com.example.core.model.CurrencyCode
import com.example.core.model.ExchangeRate

/**
 * Pure functions generating balanced JournalDraft objects for every document type.
 * Zero Android dependencies, zero side-effects, 100% unit-testable on JVM.
 */
object PostingRules {

    /**
     * Sales Invoice:
     * DR 1201 (Customer/Agent) for full invoice total
     * CR 4101 (Card Sales) for card lines
     * CR 4201 (Service Sales) for service lines
     */
    fun createSalesInvoiceDraft(
        partyId: String,
        cardTotalOrigMinor: Long,
        serviceTotalOrigMinor: Long,
        currency: CurrencyCode,
        exchangeRate: ExchangeRate,
        dateEpochDay: Long,
        memo: String
    ): JournalDraft {
        require(cardTotalOrigMinor >= 0L && serviceTotalOrigMinor >= 0L) { "Revenue amounts must be non-negative" }
        val totalOrigMinor = cardTotalOrigMinor + serviceTotalOrigMinor
        require(totalOrigMinor > 0L) { "Invoice total must be strictly positive" }

        val totalBaseMinor = exchangeRate.convert(totalOrigMinor)
        val origAmounts = mutableListOf<Long>()
        if (cardTotalOrigMinor > 0L) origAmounts.add(cardTotalOrigMinor)
        if (serviceTotalOrigMinor > 0L) origAmounts.add(serviceTotalOrigMinor)

        val convertedCredits = ExchangeRate.distributeConvertedLines(origAmounts, exchangeRate, totalBaseMinor)

        val lines = mutableListOf<JournalDraftLine>()
        // 1. DR Accounts Receivable (1201)
        lines.add(
            JournalDraftLine(
                lineNo = 1,
                accountCode = AccountConstants.ACCOUNTS_RECEIVABLE,
                partyId = partyId,
                origMinor = totalOrigMinor,
                currency = currency,
                exchangeRateMicros = exchangeRate.rateMicros,
                baseDebitMinor = totalBaseMinor,
                baseCreditMinor = 0L,
                memo = memo
            )
        )

        var creditIdx = 0
        var currentLineNo = 2
        if (cardTotalOrigMinor > 0L) {
            lines.add(
                JournalDraftLine(
                    lineNo = currentLineNo++,
                    accountCode = AccountConstants.CARD_SALES_REVENUE,
                    origMinor = cardTotalOrigMinor,
                    currency = currency,
                    exchangeRateMicros = exchangeRate.rateMicros,
                    baseDebitMinor = 0L,
                    baseCreditMinor = convertedCredits[creditIdx++],
                    memo = "مبيعات كروت إنترنت: $memo"
                )
            )
        }

        if (serviceTotalOrigMinor > 0L) {
            lines.add(
                JournalDraftLine(
                    lineNo = currentLineNo,
                    accountCode = AccountConstants.DIRECT_SERVICE_REVENUE,
                    origMinor = serviceTotalOrigMinor,
                    currency = currency,
                    exchangeRateMicros = exchangeRate.rateMicros,
                    baseDebitMinor = 0L,
                    baseCreditMinor = convertedCredits[creditIdx],
                    memo = "إيرادات خدمات واشتراكات: $memo"
                )
            )
        }

        return JournalDraft(
            type = JournalEntryType.NORMAL,
            entryDateEpochDay = dateEpochDay,
            memo = memo,
            lines = lines
        )
    }

    /**
     * Customer Receipt Voucher:
     * DR Treasury (1101/1102)
     * CR 1201 (Customer/Agent)
     */
    fun createCustomerReceiptDraft(
        treasuryGlCode: String,
        treasuryId: String,
        partyId: String,
        amountOrigMinor: Long,
        currency: CurrencyCode,
        exchangeRate: ExchangeRate,
        dateEpochDay: Long,
        memo: String
    ): JournalDraft {
        require(amountOrigMinor > 0L) { "Receipt amount must be positive" }
        val baseMinor = exchangeRate.convert(amountOrigMinor)

        val lines = listOf(
            JournalDraftLine(
                lineNo = 1,
                accountCode = treasuryGlCode,
                treasuryId = treasuryId,
                origMinor = amountOrigMinor,
                currency = currency,
                exchangeRateMicros = exchangeRate.rateMicros,
                baseDebitMinor = baseMinor,
                baseCreditMinor = 0L,
                memo = memo
            ),
            JournalDraftLine(
                lineNo = 2,
                accountCode = AccountConstants.ACCOUNTS_RECEIVABLE,
                partyId = partyId,
                origMinor = amountOrigMinor,
                currency = currency,
                exchangeRateMicros = exchangeRate.rateMicros,
                baseDebitMinor = 0L,
                baseCreditMinor = baseMinor,
                memo = memo
            )
        )

        return JournalDraft(
            type = JournalEntryType.NORMAL,
            entryDateEpochDay = dateEpochDay,
            memo = memo,
            lines = lines
        )
    }

    /**
     * Capital / Partner Contribution Receipt Voucher:
     * DR Treasury
     * CR 3101 (Capital) or 3201 (Partner Current)
     */
    fun createCapitalContributionDraft(
        treasuryGlCode: String,
        treasuryId: String,
        targetAccountCode: String,
        partyId: String?,
        amountOrigMinor: Long,
        currency: CurrencyCode,
        exchangeRate: ExchangeRate,
        dateEpochDay: Long,
        memo: String
    ): JournalDraft {
        require(amountOrigMinor > 0L) { "Contribution amount must be positive" }
        require(targetAccountCode == AccountConstants.CAPITAL || targetAccountCode == AccountConstants.PARTNER_CURRENT) {
            "Capital receipt target account must be 3101 or 3201"
        }
        val baseMinor = exchangeRate.convert(amountOrigMinor)

        val lines = listOf(
            JournalDraftLine(
                lineNo = 1,
                accountCode = treasuryGlCode,
                treasuryId = treasuryId,
                origMinor = amountOrigMinor,
                currency = currency,
                exchangeRateMicros = exchangeRate.rateMicros,
                baseDebitMinor = baseMinor,
                baseCreditMinor = 0L,
                memo = memo
            ),
            JournalDraftLine(
                lineNo = 2,
                accountCode = targetAccountCode,
                partyId = partyId,
                origMinor = amountOrigMinor,
                currency = currency,
                exchangeRateMicros = exchangeRate.rateMicros,
                baseDebitMinor = 0L,
                baseCreditMinor = baseMinor,
                memo = memo
            )
        )

        return JournalDraft(
            type = JournalEntryType.NORMAL,
            entryDateEpochDay = dateEpochDay,
            memo = memo,
            lines = lines
        )
    }

    /**
     * Credit Note (Sales Return):
     * DR 4102 (Sales Returns)
     * CR 1201 (Customer/Agent)
     */
    fun createCreditNoteDraft(
        partyId: String,
        amountOrigMinor: Long,
        currency: CurrencyCode,
        exchangeRate: ExchangeRate,
        dateEpochDay: Long,
        memo: String
    ): JournalDraft {
        require(amountOrigMinor > 0L) { "Credit note amount must be positive" }
        val baseMinor = exchangeRate.convert(amountOrigMinor)

        val lines = listOf(
            JournalDraftLine(
                lineNo = 1,
                accountCode = AccountConstants.SALES_RETURNS,
                origMinor = amountOrigMinor,
                currency = currency,
                exchangeRateMicros = exchangeRate.rateMicros,
                baseDebitMinor = baseMinor,
                baseCreditMinor = 0L,
                memo = memo
            ),
            JournalDraftLine(
                lineNo = 2,
                accountCode = AccountConstants.ACCOUNTS_RECEIVABLE,
                partyId = partyId,
                origMinor = amountOrigMinor,
                currency = currency,
                exchangeRateMicros = exchangeRate.rateMicros,
                baseDebitMinor = 0L,
                baseCreditMinor = baseMinor,
                memo = memo
            )
        )

        return JournalDraft(
            type = JournalEntryType.NORMAL,
            entryDateEpochDay = dateEpochDay,
            memo = memo,
            lines = lines
        )
    }

    /**
     * Purchase Invoice:
     * DR 1501 for Fixed Assets or 5xxx for Expenses
     * CR 2101 (Vendor) in full
     */
    fun createPurchaseInvoiceDraft(
        vendorPartyId: String,
        items: List<PurchaseItemDraft>,
        currency: CurrencyCode,
        exchangeRate: ExchangeRate,
        dateEpochDay: Long,
        memo: String
    ): JournalDraft {
        require(items.isNotEmpty()) { "Purchase invoice must have at least one line item" }
        val totalOrigMinor = items.sumOf { it.origMinor }
        require(totalOrigMinor > 0L) { "Purchase total must be positive" }

        val totalBaseMinor = exchangeRate.convert(totalOrigMinor)
        val origAmounts = items.map { it.origMinor }
        val convertedDebits = ExchangeRate.distributeConvertedLines(origAmounts, exchangeRate, totalBaseMinor)

        val lines = mutableListOf<JournalDraftLine>()
        var lineNo = 1
        items.forEachIndexed { index, item ->
            lines.add(
                JournalDraftLine(
                    lineNo = lineNo++,
                    accountCode = item.accountCode,
                    origMinor = item.origMinor,
                    currency = currency,
                    exchangeRateMicros = exchangeRate.rateMicros,
                    baseDebitMinor = convertedDebits[index],
                    baseCreditMinor = 0L,
                    memo = item.description
                )
            )
        }

        // CR 2101 Accounts Payable
        lines.add(
            JournalDraftLine(
                lineNo = lineNo,
                accountCode = AccountConstants.ACCOUNTS_PAYABLE,
                partyId = vendorPartyId,
                origMinor = totalOrigMinor,
                currency = currency,
                exchangeRateMicros = exchangeRate.rateMicros,
                baseDebitMinor = 0L,
                baseCreditMinor = totalBaseMinor,
                memo = memo
            )
        )

        return JournalDraft(
            type = JournalEntryType.NORMAL,
            entryDateEpochDay = dateEpochDay,
            memo = memo,
            lines = lines
        )
    }

    /**
     * Payment Voucher: Vendor Settlement
     * DR 2101 (Vendor)
     * CR Treasury (1101/1102)
     */
    fun createVendorPaymentDraft(
        treasuryGlCode: String,
        treasuryId: String,
        vendorPartyId: String,
        amountOrigMinor: Long,
        currency: CurrencyCode,
        exchangeRate: ExchangeRate,
        dateEpochDay: Long,
        memo: String
    ): JournalDraft {
        require(amountOrigMinor > 0L) { "Payment amount must be positive" }
        val baseMinor = exchangeRate.convert(amountOrigMinor)

        val lines = listOf(
            JournalDraftLine(
                lineNo = 1,
                accountCode = AccountConstants.ACCOUNTS_PAYABLE,
                partyId = vendorPartyId,
                origMinor = amountOrigMinor,
                currency = currency,
                exchangeRateMicros = exchangeRate.rateMicros,
                baseDebitMinor = baseMinor,
                baseCreditMinor = 0L,
                memo = memo
            ),
            JournalDraftLine(
                lineNo = 2,
                accountCode = treasuryGlCode,
                treasuryId = treasuryId,
                origMinor = amountOrigMinor,
                currency = currency,
                exchangeRateMicros = exchangeRate.rateMicros,
                baseDebitMinor = 0L,
                baseCreditMinor = baseMinor,
                memo = memo
            )
        )

        return JournalDraft(
            type = JournalEntryType.NORMAL,
            entryDateEpochDay = dateEpochDay,
            memo = memo,
            lines = lines
        )
    }

    /**
     * Payment Voucher: Direct Expense (e.g. 5101 Upstream ISP or 5201 Operating)
     * DR Expense Account (5xxx)
     * CR Treasury
     */
    fun createDirectExpensePaymentDraft(
        treasuryGlCode: String,
        treasuryId: String,
        expenseAccountCode: String,
        amountOrigMinor: Long,
        currency: CurrencyCode,
        exchangeRate: ExchangeRate,
        dateEpochDay: Long,
        memo: String
    ): JournalDraft {
        require(amountOrigMinor > 0L) { "Expense amount must be positive" }
        require(expenseAccountCode.startsWith("5")) { "Expense account code must start with 5xxx" }
        val baseMinor = exchangeRate.convert(amountOrigMinor)

        val lines = listOf(
            JournalDraftLine(
                lineNo = 1,
                accountCode = expenseAccountCode,
                origMinor = amountOrigMinor,
                currency = currency,
                exchangeRateMicros = exchangeRate.rateMicros,
                baseDebitMinor = baseMinor,
                baseCreditMinor = 0L,
                memo = memo
            ),
            JournalDraftLine(
                lineNo = 2,
                accountCode = treasuryGlCode,
                treasuryId = treasuryId,
                origMinor = amountOrigMinor,
                currency = currency,
                exchangeRateMicros = exchangeRate.rateMicros,
                baseDebitMinor = 0L,
                baseCreditMinor = baseMinor,
                memo = memo
            )
        )

        return JournalDraft(
            type = JournalEntryType.NORMAL,
            entryDateEpochDay = dateEpochDay,
            memo = memo,
            lines = lines
        )
    }

    /**
     * Payment Voucher: Partner Drawings
     * DR 3201 (Partner Current)
     * CR Treasury
     */
    fun createPartnerDrawingsDraft(
        treasuryGlCode: String,
        treasuryId: String,
        partnerPartyId: String,
        amountOrigMinor: Long,
        currency: CurrencyCode,
        exchangeRate: ExchangeRate,
        dateEpochDay: Long,
        memo: String
    ): JournalDraft {
        require(amountOrigMinor > 0L) { "Drawing amount must be positive" }
        val baseMinor = exchangeRate.convert(amountOrigMinor)

        val lines = listOf(
            JournalDraftLine(
                lineNo = 1,
                accountCode = AccountConstants.PARTNER_CURRENT,
                partyId = partnerPartyId,
                origMinor = amountOrigMinor,
                currency = currency,
                exchangeRateMicros = exchangeRate.rateMicros,
                baseDebitMinor = baseMinor,
                baseCreditMinor = 0L,
                memo = memo
            ),
            JournalDraftLine(
                lineNo = 2,
                accountCode = treasuryGlCode,
                treasuryId = treasuryId,
                origMinor = amountOrigMinor,
                currency = currency,
                exchangeRateMicros = exchangeRate.rateMicros,
                baseDebitMinor = 0L,
                baseCreditMinor = baseMinor,
                memo = memo
            )
        )

        return JournalDraft(
            type = JournalEntryType.NORMAL,
            entryDateEpochDay = dateEpochDay,
            memo = memo,
            lines = lines
        )
    }

    /**
     * Treasury Transfer (with optional foreign exchange gain/loss)
     * DR Dest Treasury (destBaseMinor)
     * CR Source Treasury (sourceBaseMinor)
     * Balanced by CR 4901 (Gain) or DR 5901 (Loss) if sourceBaseMinor != destBaseMinor
     */
    fun createTreasuryTransferDraft(
        sourceTreasuryGlCode: String,
        sourceTreasuryId: String,
        sourceAmountOrigMinor: Long,
        sourceCurrency: CurrencyCode,
        sourceRate: ExchangeRate,
        destTreasuryGlCode: String,
        destTreasuryId: String,
        destAmountOrigMinor: Long,
        destCurrency: CurrencyCode,
        destRate: ExchangeRate,
        dateEpochDay: Long,
        memo: String
    ): JournalDraft {
        val sourceBase = sourceRate.convert(sourceAmountOrigMinor)
        val destBase = destRate.convert(destAmountOrigMinor)
        val lines = mutableListOf<JournalDraftLine>()

        var lineNo = 1
        lines.add(
            JournalDraftLine(
                lineNo = lineNo++,
                accountCode = destTreasuryGlCode,
                treasuryId = destTreasuryId,
                origMinor = destAmountOrigMinor,
                currency = destCurrency,
                exchangeRateMicros = destRate.rateMicros,
                baseDebitMinor = destBase,
                baseCreditMinor = 0L,
                memo = "إيداع تحويل: $memo"
            )
        )

        if (destBase < sourceBase) {
            // Outflow was higher than inflow value -> Realized FX Loss (DR 5901)
            val lossMinor = sourceBase - destBase
            lines.add(
                JournalDraftLine(
                    lineNo = lineNo++,
                    accountCode = AccountConstants.REALIZED_FX_LOSS,
                    origMinor = lossMinor,
                    currency = CurrencyCode.FUNCTIONAL,
                    exchangeRateMicros = ExchangeRate.SCALE_MICROS,
                    baseDebitMinor = lossMinor,
                    baseCreditMinor = 0L,
                    memo = "خسارة فروق عملة: $memo"
                )
            )
        }

        lines.add(
            JournalDraftLine(
                lineNo = lineNo++,
                accountCode = sourceTreasuryGlCode,
                treasuryId = sourceTreasuryId,
                origMinor = sourceAmountOrigMinor,
                currency = sourceCurrency,
                exchangeRateMicros = sourceRate.rateMicros,
                baseDebitMinor = 0L,
                baseCreditMinor = sourceBase,
                memo = "سحب تحويل: $memo"
            )
        )

        if (destBase > sourceBase) {
            // Inflow was higher than outflow value -> Realized FX Gain (CR 4901)
            val gainMinor = destBase - sourceBase
            lines.add(
                JournalDraftLine(
                    lineNo = lineNo,
                    accountCode = AccountConstants.REALIZED_FX_GAIN,
                    origMinor = gainMinor,
                    currency = CurrencyCode.FUNCTIONAL,
                    exchangeRateMicros = ExchangeRate.SCALE_MICROS,
                    baseDebitMinor = 0L,
                    baseCreditMinor = gainMinor,
                    memo = "أرباح فروق عملة: $memo"
                )
            )
        }

        return JournalDraft(
            type = JournalEntryType.NORMAL,
            entryDateEpochDay = dateEpochDay,
            memo = memo,
            lines = lines
        )
    }

    /**
     * Straight-line monthly depreciation:
     * DR 5203 (Depreciation Expense)
     * CR 1599 (Accumulated Depreciation)
     */
    fun createDepreciationDraft(
        depreciationAmountMinor: Long,
        dateEpochDay: Long,
        memo: String
    ): JournalDraft {
        require(depreciationAmountMinor > 0L) { "Depreciation amount must be positive" }

        val lines = listOf(
            JournalDraftLine(
                lineNo = 1,
                accountCode = AccountConstants.DEPRECIATION_EXPENSE,
                origMinor = depreciationAmountMinor,
                currency = CurrencyCode.FUNCTIONAL,
                exchangeRateMicros = ExchangeRate.SCALE_MICROS,
                baseDebitMinor = depreciationAmountMinor,
                baseCreditMinor = 0L,
                memo = memo
            ),
            JournalDraftLine(
                lineNo = 2,
                accountCode = AccountConstants.ACCUMULATED_DEPRECIATION,
                origMinor = depreciationAmountMinor,
                currency = CurrencyCode.FUNCTIONAL,
                exchangeRateMicros = ExchangeRate.SCALE_MICROS,
                baseDebitMinor = 0L,
                baseCreditMinor = depreciationAmountMinor,
                memo = memo
            )
        )

        return JournalDraft(
            type = JournalEntryType.NORMAL,
            entryDateEpochDay = dateEpochDay,
            memo = memo,
            lines = lines
        )
    }

    /**
     * Reversal Entry (Compensating entry flipping debits and credits)
     */
    fun createReversalDraft(
        originalLines: List<JournalDraftLine>,
        reversalDateEpochDay: Long,
        reversalMemo: String
    ): JournalDraft {
        require(originalLines.isNotEmpty()) { "Cannot reverse an empty journal entry" }

        val invertedLines = originalLines.mapIndexed { idx, line ->
            JournalDraftLine(
                lineNo = idx + 1,
                accountCode = line.accountCode,
                partyId = line.partyId,
                treasuryId = line.treasuryId,
                origMinor = line.origMinor,
                currency = line.currency,
                exchangeRateMicros = line.exchangeRateMicros,
                baseDebitMinor = line.baseCreditMinor, // Flip
                baseCreditMinor = line.baseDebitMinor, // Flip
                memo = "عكس: ${line.memo}"
            )
        }

        return JournalDraft(
            type = JournalEntryType.REVERSAL,
            entryDateEpochDay = reversalDateEpochDay,
            memo = reversalMemo,
            lines = invertedLines
        )
    }

    /**
     * Customer Receipt with FX settlement gain/loss (IAS 21):
     * Settling an invoice where original rate != settlement rate.
     */
    fun createCustomerReceiptWithFxDraft(
        treasuryGlCode: String,
        treasuryId: String,
        partyId: String,
        amountOrigMinor: Long,
        currency: CurrencyCode,
        settlementRate: ExchangeRate,
        invoiceRate: ExchangeRate,
        dateEpochDay: Long,
        memo: String
    ): JournalDraft {
        val cashReceivedBase = settlementRate.convert(amountOrigMinor)
        val receivableRelievedBase = invoiceRate.convert(amountOrigMinor)

        val lines = mutableListOf<JournalDraftLine>()
        var lineNo = 1

        // DR Treasury with actual cash received at settlement rate
        lines.add(
            JournalDraftLine(
                lineNo = lineNo++,
                accountCode = treasuryGlCode,
                treasuryId = treasuryId,
                origMinor = amountOrigMinor,
                currency = currency,
                exchangeRateMicros = settlementRate.rateMicros,
                baseDebitMinor = cashReceivedBase,
                baseCreditMinor = 0L,
                memo = memo
            )
        )

        if (cashReceivedBase < receivableRelievedBase) {
            // Received less base currency than booked invoice -> FX Loss (DR 5901)
            val lossMinor = receivableRelievedBase - cashReceivedBase
            lines.add(
                JournalDraftLine(
                    lineNo = lineNo++,
                    accountCode = AccountConstants.REALIZED_FX_LOSS,
                    origMinor = lossMinor,
                    currency = CurrencyCode.FUNCTIONAL,
                    exchangeRateMicros = ExchangeRate.SCALE_MICROS,
                    baseDebitMinor = lossMinor,
                    baseCreditMinor = 0L,
                    memo = "خسارة تسوية فروق عملة: $memo"
                )
            )
        }

        // CR Accounts Receivable (1201) with book value of the invoice
        lines.add(
            JournalDraftLine(
                lineNo = lineNo++,
                accountCode = AccountConstants.ACCOUNTS_RECEIVABLE,
                partyId = partyId,
                origMinor = amountOrigMinor,
                currency = currency,
                exchangeRateMicros = invoiceRate.rateMicros,
                baseDebitMinor = 0L,
                baseCreditMinor = receivableRelievedBase,
                memo = memo
            )
        )

        if (cashReceivedBase > receivableRelievedBase) {
            // Received more base currency than booked invoice -> FX Gain (CR 4901)
            val gainMinor = cashReceivedBase - receivableRelievedBase
            lines.add(
                JournalDraftLine(
                    lineNo = lineNo,
                    accountCode = AccountConstants.REALIZED_FX_GAIN,
                    origMinor = gainMinor,
                    currency = CurrencyCode.FUNCTIONAL,
                    exchangeRateMicros = ExchangeRate.SCALE_MICROS,
                    baseDebitMinor = 0L,
                    baseCreditMinor = gainMinor,
                    memo = "أرباح تسوية فروق عملة: $memo"
                )
            )
        }

        return JournalDraft(
            type = JournalEntryType.NORMAL,
            entryDateEpochDay = dateEpochDay,
            memo = memo,
            lines = lines
        )
    }

    /**
     * Year-end closing draft:
     * Clears all Revenue (4xxx) balances with Debits
     * Clears all Expense (5xxx) balances with Credits
     * Net difference transferred to 3301 (Retained Earnings)
     */
    fun createClosingDraft(
        revenueBalances: Map<String, Long>, // accountCode -> credit balance minor
        expenseBalances: Map<String, Long>, // accountCode -> debit balance minor
        dateEpochDay: Long,
        memo: String
    ): JournalDraft {
        val totalRevenue = revenueBalances.values.sum()
        val totalExpense = expenseBalances.values.sum()
        val netIncome = totalRevenue - totalExpense

        val lines = mutableListOf<JournalDraftLine>()
        var lineNo = 1

        // Debit revenues to zero them out
        revenueBalances.filter { it.value > 0L }.forEach { (account, balance) ->
            lines.add(
                JournalDraftLine(
                    lineNo = lineNo++,
                    accountCode = account,
                    origMinor = balance,
                    currency = CurrencyCode.FUNCTIONAL,
                    exchangeRateMicros = ExchangeRate.SCALE_MICROS,
                    baseDebitMinor = balance,
                    baseCreditMinor = 0L,
                    memo = "إقفال إيراد $account"
                )
            )
        }

        // Credit expenses to zero them out
        expenseBalances.filter { it.value > 0L }.forEach { (account, balance) ->
            lines.add(
                JournalDraftLine(
                    lineNo = lineNo++,
                    accountCode = account,
                    origMinor = balance,
                    currency = CurrencyCode.FUNCTIONAL,
                    exchangeRateMicros = ExchangeRate.SCALE_MICROS,
                    baseDebitMinor = 0L,
                    baseCreditMinor = balance,
                    memo = "إقفال مصروف $account"
                )
            )
        }

        // Net income/loss to Retained Earnings (3301)
        if (netIncome > 0L) {
            // Net profit: Credit 3301
            lines.add(
                JournalDraftLine(
                    lineNo = lineNo,
                    accountCode = AccountConstants.RETAINED_EARNINGS,
                    origMinor = netIncome,
                    currency = CurrencyCode.FUNCTIONAL,
                    exchangeRateMicros = ExchangeRate.SCALE_MICROS,
                    baseDebitMinor = 0L,
                    baseCreditMinor = netIncome,
                    memo = "صافي ربح العام المحول للأرباح المبقاة"
                )
            )
        } else if (netIncome < 0L) {
            // Net loss: Debit 3301
            val absLoss = kotlin.math.abs(netIncome)
            lines.add(
                JournalDraftLine(
                    lineNo = lineNo,
                    accountCode = AccountConstants.RETAINED_EARNINGS,
                    origMinor = absLoss,
                    currency = CurrencyCode.FUNCTIONAL,
                    exchangeRateMicros = ExchangeRate.SCALE_MICROS,
                    baseDebitMinor = absLoss,
                    baseCreditMinor = 0L,
                    memo = "صافي خسارة العام المحولة للأرباح المبقاة"
                )
            )
        }

        return JournalDraft(
            type = JournalEntryType.CLOSING,
            entryDateEpochDay = dateEpochDay,
            memo = memo,
            lines = lines
        )
    }
}

data class PurchaseItemDraft(
    val accountCode: String,
    val description: String,
    val origMinor: Long,
    val isAsset: Boolean = false
)
