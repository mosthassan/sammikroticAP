# SamMikrotik - Product & Engineering Roadmap

## Phase 1: Accounting Core & Architecture (Current)
- [x] Comprehensive Documentation (`AGENTS.md`, Architecture, Accounting Spec, Design System, Test Plan, Decisions).
- [x] Zero-Float Money Engine (`Money`, `CurrencyCode`, `ExchangeRate`, integer micro-rates, rounding distribution).
- [x] Room Database v1 Schema (Organizations, Parties, Treasuries, Chart of Accounts, Periods, Journal Entries & Lines, Documents & Items, Allocations, Assets, Depreciation Runs, Card Packages, Stock Movements, Number Sequences, Audit Log, Idempotency Keys).
- [x] Immutable SQLite triggers (`BEFORE UPDATE/DELETE` abort on `journal_entries` and `journal_lines`).
- [x] Pure Business Posting Rules (`PostingRules`).
- [x] Unified Transaction Pipeline (`LedgerWriter`).
- [x] Mathematical Invariant Validator (`LedgerInvariants`).
- [x] Seed Data (Locked IFRS COA, Walk-in Cash Party, Currencies, Standard Treasuries).
- [x] Clean Dark/Light Theme with Arabic RTL support and Tabular Numerals.
- [x] Reusable Financial UI Components (`AmountText`, `MoneyField`, `StatCard`, `InvariantBanner`, etc.).
- [x] App Structure: Onboarding Flow + Developer/Inspector Screens (COA, Journal Browser, Trial Balance, Health Check).
- [x] Unit & Robolectric Integration Test Suite.

## Phase 2: Business Operations & Workflows
- [ ] Sales & Distribution Module:
  - Batch Card Voucher generation & tracking (Package details, wholesale/retail prices).
  - Grocery Agent delivery sheets & receivables management.
  - Direct Internet Subscription contracts & billing.
- [ ] Expense & Upstream ISP Management:
  - Direct upstream bandwidth payment vouchers (Starlink, Wholesale Fiber).
  - Fuel, generator maintenance, site rentals, technician salary slips.
- [ ] Treasury & Multi-Currency Transfers:
  - Cash drawers, bank accounts, electronic wallets (Floosak, Kuraimi, Jawali, etc.).
  - Inter-treasury currency conversions with realized FX tracking.
- [ ] Partner Equity & Profit Distribution:
  - Capital contributions, monthly drawings, automated dividend calculator.
- [ ] Financial Statements:
  - Real-time Income Statement (P&L - Service Costing).
  - Balance Sheet (IFRS).
  - Statement of Cash Flows.

## Phase 3: Hardware Integrations & Cloud Sync
- [ ] MikroTik RouterOS API / Hotspot Integration:
  - Sync active users, pull generated voucher batches, disable expired users.
- [ ] Bluetooth ESC/POS Thermal Printing for voucher slips and customer receipts.
- [ ] Offline-First Cloud Sync (Firebase Firestore / Cloud SQL backend) with conflict resolution.
- [ ] Role-Based Access Control (Owner, Accountant, Cashier, Auditor).
- [ ] Encrypted Automated Backups (Local storage & Google Drive).
