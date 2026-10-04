package com.example.domain.usecase

import androidx.room.withTransaction
import com.example.data.ledger.LedgerInvariants
import com.example.data.local.AppDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

class BackupRestoreUseCase(private val db: AppDatabase) {

    private val invariants = LedgerInvariants(db)

    suspend fun exportDatabaseToJson(): String {
        val root = JSONObject()
        root.put("schemaVersion", 1)
        root.put("timestamp", System.currentTimeMillis())

        // Parties
        val partiesArr = JSONArray()
        db.partyDao().getAllPartiesSync().forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("phone", p.phone)
            obj.put("isCustomer", p.isCustomer)
            obj.put("isVendor", p.isVendor)
            obj.put("isPartner", p.isPartner)
            obj.put("equityPercentageBasisPoints", p.equityPercentageBasisPoints)
            obj.put("creditLimitMinor", p.creditLimitMinor)
            obj.put("isActive", p.isActive)
            obj.put("createdAt", p.createdAt)
            partiesArr.put(obj)
        }
        root.put("parties", partiesArr)

        // Card Packages
        val pkgsArr = JSONArray()
        db.cardPackageDao().getAllPackagesSync().forEach { pkg ->
            val obj = JSONObject()
            obj.put("id", pkg.id)
            obj.put("name", pkg.name)
            obj.put("durationOrQuota", pkg.durationOrQuota)
            obj.put("wholesalePriceMinor", pkg.wholesalePriceMinor)
            obj.put("retailPriceMinor", pkg.retailPriceMinor)
            obj.put("isActive", pkg.isActive)
            pkgsArr.put(obj)
        }
        root.put("packages", pkgsArr)

        // Treasuries
        val treasuriesArr = JSONArray()
        db.treasuryDao().getAllTreasuriesSync().forEach { tr ->
            val obj = JSONObject()
            obj.put("id", tr.id)
            obj.put("name", tr.name)
            obj.put("glAccountCode", tr.glAccountCode)
            obj.put("currency", tr.currency)
            obj.put("isActive", tr.isActive)
            treasuriesArr.put(obj)
        }
        root.put("treasuries", treasuriesArr)

        // Documents
        val docsArr = JSONArray()
        db.documentDao().getAllDocumentsSync().forEach { d ->
            val obj = JSONObject()
            obj.put("id", d.id)
            obj.put("type", d.type)
            obj.put("fiscalYear", d.fiscalYear)
            obj.put("docNumber", d.docNumber)
            obj.put("partyId", d.partyId)
            obj.put("dateEpochDay", d.dateEpochDay)
            obj.put("currency", d.currency)
            obj.put("exchangeRateMicros", d.exchangeRateMicros)
            obj.put("totalMinor", d.totalMinor)
            obj.put("totalBaseMinor", d.totalBaseMinor)
            obj.put("status", d.status)
            obj.put("notes", d.notes)
            obj.put("reversalOfDocId", d.reversalOfDocId)
            obj.put("createdAt", d.createdAt)
            docsArr.put(obj)
        }
        root.put("documents", docsArr)

        // Document Items
        val itemsArr = JSONArray()
        db.documentDao().getAllDocumentItemsSync().forEach { itm ->
            val obj = JSONObject()
            obj.put("id", itm.id)
            obj.put("docId", itm.docId)
            obj.put("itemIndex", itm.itemIndex)
            obj.put("packageId", itm.packageId)
            obj.put("description", itm.description)
            obj.put("accountCode", itm.accountCode)
            obj.put("quantity", itm.quantity)
            obj.put("unitPriceMinor", itm.unitPriceMinor)
            obj.put("totalMinor", itm.totalMinor)
            obj.put("isAsset", itm.isAsset)
            itemsArr.put(obj)
        }
        root.put("document_items", itemsArr)

        // Journal Entries
        val entriesArr = JSONArray()
        db.journalDao().getAllEntriesSync().forEach { je ->
            val obj = JSONObject()
            obj.put("id", je.id)
            obj.put("docId", je.docId)
            obj.put("entryNumber", je.entryNumber)
            obj.put("entryDateEpochDay", je.entryDateEpochDay)
            obj.put("type", je.type)
            obj.put("memo", je.memo)
            obj.put("createdAt", je.createdAt)
            entriesArr.put(obj)
        }
        root.put("journal_entries", entriesArr)

        // Journal Lines
        val linesArr = JSONArray()
        db.journalDao().getAllLinesSync().forEach { jl ->
            val obj = JSONObject()
            obj.put("id", jl.id)
            obj.put("entryId", jl.entryId)
            obj.put("lineNo", jl.lineNo)
            obj.put("accountCode", jl.accountCode)
            obj.put("partyId", jl.partyId)
            obj.put("treasuryId", jl.treasuryId)
            obj.put("origMinor", jl.origMinor)
            obj.put("currency", jl.currency)
            obj.put("exchangeRateMicros", jl.exchangeRateMicros)
            obj.put("baseDebitMinor", jl.baseDebitMinor)
            obj.put("baseCreditMinor", jl.baseCreditMinor)
            obj.put("memo", jl.memo)
            linesArr.put(obj)
        }
        root.put("journal_lines", linesArr)

        // Allocations
        val allocArr = JSONArray()
        db.allocationDao().getAllActiveAllocationsSync().forEach { al ->
            val obj = JSONObject()
            obj.put("id", al.id)
            obj.put("paymentDocId", al.paymentDocId)
            obj.put("invoiceDocId", al.invoiceDocId)
            obj.put("allocatedOrigMinor", al.allocatedOrigMinor)
            obj.put("allocatedBaseMinor", al.allocatedBaseMinor)
            obj.put("exchangeGainLossMinor", al.exchangeGainLossMinor)
            obj.put("isVoided", al.isVoided)
            obj.put("createdAt", al.createdAt)
            allocArr.put(obj)
        }
        root.put("allocations", allocArr)

        // Assets
        val assetsArr = JSONArray()
        db.assetDao().getAllAssetsSync().forEach { ast ->
            val obj = JSONObject()
            obj.put("id", ast.id)
            obj.put("docId", ast.docId)
            obj.put("name", ast.name)
            obj.put("purchaseDateEpochDay", ast.purchaseDateEpochDay)
            obj.put("purchaseCostMinor", ast.purchaseCostMinor)
            obj.put("salvageValueMinor", ast.salvageValueMinor)
            obj.put("usefulLifeMonths", ast.usefulLifeMonths)
            obj.put("accumulatedDepreciationMinor", ast.accumulatedDepreciationMinor)
            obj.put("isDisposed", ast.isDisposed)
            assetsArr.put(obj)
        }
        root.put("assets", assetsArr)

        val rawJson = root.toString(2)
        val hash = sha256(rawJson)
        root.put("sha256", hash)
        return root.toString(2)
    }

    suspend fun restoreDatabaseFromJson(jsonString: String): Result<String> = runCatching {
        val root = JSONObject(jsonString)
        val schemaVer = root.optInt("schemaVersion", -1)
        require(schemaVer == 1) { "Unsupported backup schema version: $schemaVer" }

        db.withTransaction {
            val sdb = db.openHelper.writableDatabase

            // Clear existing data cleanly (drop triggers temporarily during bulk restore)
            sdb.execSQL("DROP TRIGGER IF EXISTS prevent_journal_entries_update")
            sdb.execSQL("DROP TRIGGER IF EXISTS prevent_journal_entries_delete")
            sdb.execSQL("DROP TRIGGER IF EXISTS prevent_journal_lines_update")
            sdb.execSQL("DROP TRIGGER IF EXISTS prevent_journal_lines_delete")

            sdb.execSQL("DELETE FROM journal_lines")
            sdb.execSQL("DELETE FROM journal_entries")
            sdb.execSQL("DELETE FROM document_items")
            sdb.execSQL("DELETE FROM allocations")
            sdb.execSQL("DELETE FROM documents")
            sdb.execSQL("DELETE FROM depreciation_runs")
            sdb.execSQL("DELETE FROM assets")
            sdb.execSQL("DELETE FROM stock_movements")
            sdb.execSQL("DELETE FROM card_packages")
            sdb.execSQL("DELETE FROM parties WHERE id != 'WALK_IN_CASH'")

            // 1. Restore Parties
            val partiesArr = root.optJSONArray("parties") ?: JSONArray()
            for (i in 0 until partiesArr.length()) {
                val p = partiesArr.getJSONObject(i)
                sdb.execSQL("""
                    INSERT OR REPLACE INTO parties (id, name, phone, isCustomer, isVendor, isPartner, equityPercentageBasisPoints, creditLimitMinor, isActive, createdAt)
                    VALUES ('${p.getString("id")}', '${p.getString("name")}', '${p.optString("phone")}', ${if (p.getBoolean("isCustomer")) 1 else 0}, ${if (p.getBoolean("isVendor")) 1 else 0}, ${if (p.getBoolean("isPartner")) 1 else 0}, ${p.optInt("equityPercentageBasisPoints")}, ${p.optLong("creditLimitMinor")}, ${if (p.optBoolean("isActive", true)) 1 else 0}, ${p.optLong("createdAt")})
                """)
            }

            // 2. Restore Packages
            val pkgsArr = root.optJSONArray("packages") ?: JSONArray()
            for (i in 0 until pkgsArr.length()) {
                val pkg = pkgsArr.getJSONObject(i)
                sdb.execSQL("""
                    INSERT OR REPLACE INTO card_packages (id, name, durationOrQuota, wholesalePriceMinor, retailPriceMinor, isActive)
                    VALUES ('${pkg.getString("id")}', '${pkg.getString("name")}', '${pkg.getString("durationOrQuota")}', ${pkg.getLong("wholesalePriceMinor")}, ${pkg.getLong("retailPriceMinor")}, ${if (pkg.optBoolean("isActive", true)) 1 else 0})
                """)
            }

            // 3. Restore Documents
            val docsArr = root.optJSONArray("documents") ?: JSONArray()
            for (i in 0 until docsArr.length()) {
                val d = docsArr.getJSONObject(i)
                sdb.execSQL("""
                    INSERT INTO documents (id, type, fiscalYear, docNumber, partyId, dateEpochDay, currency, exchangeRateMicros, totalMinor, totalBaseMinor, status, notes, reversalOfDocId, createdAt)
                    VALUES ('${d.getString("id")}', '${d.getString("type")}', ${d.getInt("fiscalYear")}, ${d.getLong("docNumber")}, '${d.getString("partyId")}', ${d.getLong("dateEpochDay")}, '${d.getString("currency")}', ${d.getLong("exchangeRateMicros")}, ${d.getLong("totalMinor")}, ${d.getLong("totalBaseMinor")}, '${d.getString("status")}', '${d.optString("notes")}', ${if (d.isNull("reversalOfDocId")) "NULL" else "'${d.getString("reversalOfDocId")}'"}, ${d.getLong("createdAt")})
                """)
            }

            // 4. Restore Document Items
            val itemsArr = root.optJSONArray("document_items") ?: JSONArray()
            for (i in 0 until itemsArr.length()) {
                val itm = itemsArr.getJSONObject(i)
                sdb.execSQL("""
                    INSERT INTO document_items (id, docId, itemIndex, packageId, description, accountCode, quantity, unitPriceMinor, totalMinor, isAsset)
                    VALUES ('${itm.getString("id")}', '${itm.getString("docId")}', ${itm.getInt("itemIndex")}, ${if (itm.isNull("packageId")) "NULL" else "'${itm.getString("packageId")}'"}, '${itm.getString("description")}', '${itm.getString("accountCode")}', ${itm.getInt("quantity")}, ${itm.getLong("unitPriceMinor")}, ${itm.getLong("totalMinor")}, ${if (itm.getBoolean("isAsset")) 1 else 0})
                """)
            }

            // 5. Restore Journal Entries
            val entriesArr = root.optJSONArray("journal_entries") ?: JSONArray()
            for (i in 0 until entriesArr.length()) {
                val je = entriesArr.getJSONObject(i)
                sdb.execSQL("""
                    INSERT INTO journal_entries (id, docId, entryNumber, entryDateEpochDay, type, memo, createdAt)
                    VALUES ('${je.getString("id")}', '${je.getString("docId")}', ${je.getLong("entryNumber")}, ${je.getLong("entryDateEpochDay")}, '${je.getString("type")}', '${je.getString("memo")}', ${je.getLong("createdAt")})
                """)
            }

            // 6. Restore Journal Lines
            val linesArr = root.optJSONArray("journal_lines") ?: JSONArray()
            for (i in 0 until linesArr.length()) {
                val jl = linesArr.getJSONObject(i)
                sdb.execSQL("""
                    INSERT INTO journal_lines (id, entryId, lineNo, accountCode, partyId, treasuryId, origMinor, currency, exchangeRateMicros, baseDebitMinor, baseCreditMinor, memo)
                    VALUES ('${jl.getString("id")}', '${jl.getString("entryId")}', ${jl.getInt("lineNo")}, '${jl.getString("accountCode")}', ${if (jl.isNull("partyId")) "NULL" else "'${jl.getString("partyId")}'"}, ${if (jl.isNull("treasuryId")) "NULL" else "'${jl.getString("treasuryId")}'"}, ${jl.getLong("origMinor")}, '${jl.getString("currency")}', ${jl.getLong("exchangeRateMicros")}, ${jl.getLong("baseDebitMinor")}, ${jl.getLong("baseCreditMinor")}, '${jl.getString("memo")}')
                """)
            }

            // 7. Restore Allocations
            val allocArr = root.optJSONArray("allocations") ?: JSONArray()
            for (i in 0 until allocArr.length()) {
                val al = allocArr.getJSONObject(i)
                sdb.execSQL("""
                    INSERT INTO allocations (id, paymentDocId, invoiceDocId, allocatedOrigMinor, allocatedBaseMinor, exchangeGainLossMinor, isVoided, createdAt)
                    VALUES ('${al.getString("id")}', '${al.getString("paymentDocId")}', '${al.getString("invoiceDocId")}', ${al.getLong("allocatedOrigMinor")}, ${al.getLong("allocatedBaseMinor")}, ${al.optLong("exchangeGainLossMinor")}, ${if (al.optBoolean("isVoided", false)) 1 else 0}, ${al.getLong("createdAt")})
                """)
            }

            // Re-install SQLite triggers
            AppDatabase.installTriggers(sdb)

            // Verify all mathematical and ledger invariants before finalizing
            invariants.verifyAll(failFast = true)
        }

        "تمت استعادة البيانات بنجاح مع مطابقة كافة الثوابت المحاسبية 100%"
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
