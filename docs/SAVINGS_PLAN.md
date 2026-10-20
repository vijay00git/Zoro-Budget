# Savings & Assets page plan

Drafted 2026-10-20. Adds a **Savings** page that records the user's savings and assets: FD, RD, stocks, SIP / mutual funds, gold, silver, land and property, and other schemes. Each record can have PDF and photo attachments.

## Scope

- **Record keeping only.** The user types every value by hand. There is no live price feed, broker link or bank sync.
- **Separate from the ledger.** Adding, editing or deleting a savings record never changes account balances, budgets or Overview spending.
- **Every section** shows:
  - its own visual (see "Visuals");
  - its own totals;
  - for each record, a step-by-step calculation that shows the formula with the user's numbers filled in.
- All calculations run offline in pure Kotlin, so they can be unit tested.

## Page layout

1. **Summary card** (top of the page):
   - total current value, total invested, overall gain/loss in ₹ and %;
   - an allocation donut by section;
   - a "Net worth" line: bank/cash account balances plus savings, with credit-card balances subtracted;
   - a strip of upcoming maturities: the next 3 FD/RD/policy maturities, with days left.
2. **Sections, one below another**, in this order:
   1. Fixed Deposits
   2. Recurring Deposits
   3. Stocks
   4. SIP / Mutual Funds
   5. Gold
   6. Silver
   7. Land & Property
   8. Other schemes
3. **Each section header** shows:
   - the section name and record count;
   - current value and gain;
   - the section visual;
   - an expand/collapse arrow and a **+ Add** button.

   Empty sections collapse to a single "+ Add your first FD" row. The expanded/collapsed state is remembered.
4. **Each record card** shows the name, institution, current value, gain/loss and a status chip (Active / Matured / Closed / Sold). A paperclip appears when the record has attachments.
5. **Record detail screen:**
   - every input the user entered;
   - a **Calculation** card that lists each step with numbers;
   - the record's own small chart;
   - its attachments;
   - Edit and Delete (with confirmation and Undo).
6. **Filters and grouping:**
   - **Holder chips:** All, plus one chip per holder name in use. Selecting one filters the whole page, including the summary card and every chart.
   - **Group by:** Section (default) or Holder. Grouping by holder shows one block per person, with that person's total, gain and allocation donut, and their records below.
   - **Status:** Active by default; Matured, Closed and Sold are hidden until selected.
   - The selected filters are remembered.

## Fields shared by every record

| Field | Notes |
|---|---|
| Name | e.g. "SBI FD – 2 yr", "Plot at Avadi" |
| Holder | Required. A name the user types, with names already used suggested as they type. Matching ignores case and extra spaces, so "Vijay" and "vijay " are one holder. Renaming a holder in Settings renames it on every record |
| Institution / platform | Bank, broker, AMC, jeweller, sub-registrar office |
| Reference no. | FD/RD account number, folio, demat ID, survey no. Masked on cards (••••4821) |
| Nominee | Optional |
| Start / purchase date | Required |
| Maturity / end date | Where it applies |
| Status | Active, Matured, Closed, Sold |
| Notes | Free text |
| Attachments | Any number of PDFs and photos |
| Investment history | Dated entries, see below |

### Investment history (every record)

Each record keeps a list of dated entries, so the page shows exactly how much was invested, and when.

**Each entry has:**
- a date;
- a type: Invest, Withdraw/Sell, or Income (interest, dividend, rent);
- an amount;
- optional units, quantity or grams, and the price per unit;
- a note.

**How it is filled:**
- FD: one Invest entry at the start date, created from the principal.
- RD and SIP: entries can be generated from the schedule (amount, day and start date). The user then edits or deletes missed months.
- Stocks, gold, silver, PPF and others: the user adds an entry for each purchase.

**How it is used:**
- Invested = sum of Invest entries − sum of Withdraw entries.
- XIRR and CAGR use the real entry dates.
- The record detail screen lists the entries newest first, with a running total.
- A "this year" figure shows the amount invested per financial year (April–March).

**Ledger:** these entries are records only and never touch the ledger. See Decision 2.

## Sections, inputs and calculations

Worked examples use illustrative numbers and are checked in the unit tests. Today = 20 Oct 2026.

### 1. Fixed Deposit (also NSC, KVP, Post Office TD, company FDs)

**Inputs:**
- principal;
- interest rate % p.a.;
- start date;
- tenure (years + months + days) or maturity date;
- compounding: quarterly (default), monthly, half-yearly, yearly or simple;
- payout: cumulative, or monthly/quarterly interest payout;
- auto-renew (yes/no).

**Calculation:**
- Cumulative: `Maturity = P × (1 + r/n)^(n × t)`, where n is compounding periods per year and t is years.
- Tenure under 6 months: simple interest, `P × r × days/365` (Indian bank rule). This is applied automatically, with a note shown.
- Payout FD: interest per payout = `P × r / payouts per year`. Maturity = P. Total interest = interest per payout × number of payouts.
- **Value today:** the same formula with t = time elapsed so far, capped at maturity.
- Also shown: interest earned so far, interest still to come, and days to maturity.
- TDS is not deducted; a note says so.

**Example:** ₹1,00,000 at 7.0% for 2 years, compounded quarterly.

| Step | Value |
|---|---|
| r/n = 0.07 / 4 | 0.0175 |
| Periods n × t = 4 × 2 | 8 |
| (1.0175)^8 | 1.148882 |
| Maturity amount | ₹1,14,888 |
| Interest | ₹14,888 |
| Value after 1 year | ₹1,07,186 |

### 2. Recurring Deposit (also Post Office RD)

**Inputs:**
- monthly instalment;
- rate %;
- start date;
- tenure in months;
- instalment day;
- instalments paid. This defaults to the months elapsed and can be edited for missed months.

**Calculation:** quarterly compounding, the way Indian banks calculate RDs.
- `Maturity = Σ (i = 1..N) R × (1 + r/4)^(4 × i / 12)`. Each instalment earns interest for the months it stays in the account.
- Total deposited = R × N.
- Interest = maturity − deposited.
- Value today = deposited so far plus interest accrued on each paid instalment to date.
- Instalments left, and the next instalment date.

**Example:** ₹5,000 per month, 6.5%, 12 months.

| Step | Value |
|---|---|
| Total deposited (5,000 × 12) | ₹60,000 |
| Maturity amount | ₹62,143 |
| Interest | ₹2,143 |

### 3. Stocks

**Inputs, per holding:**
- company name and symbol (NSE/BSE);
- quantity;
- average buy price;
- buy date;
- current price, with an "as of" date;
- dividends received (optional running total);
- sector (optional).

**Calculation:**
- Invested = qty × avg price.
- Current = qty × current price.
- P/L = current − invested.
- P/L % = P/L ÷ invested.
- Total return including dividends.
- Days held, with a short-term or long-term label (12 months is the boundary for listed equity).

**Example:** 50 shares, average ₹1,200, current ₹1,450. Invested ₹60,000, current ₹72,500, gain ₹12,500 (+20.83%).

**Stale price warning:** if the price is more than 30 days old, the card shows "price from 12 Aug".

**Later (v2):** buy/sell lots, and realised P/L for sold quantity.

### 4. SIP / Mutual Funds

**Inputs:**
- fund name, AMC, folio;
- plan (Direct/Regular) and type (Equity/Debt/Hybrid/ELSS);
- SIP amount and SIP day;
- start date;
- stopped date (optional);
- lump-sum amounts (optional, with dates);
- units held;
- current NAV, with an "as of" date.

**Calculation:**
- Instalments: generated into the investment history from the start date, the SIP day and the stopped date. The user deletes missed months and adds lump sums.
- Invested = sum of the investment history.
- Current value = units × NAV.
- Gain = current − invested. Absolute return % = gain ÷ invested.
- **XIRR:** Newton's method over the dated investment history, with the current value as the final cash flow.
- ELSS: shows the lock-in end date (3 years per instalment) and how many units are free.

**Example:** ₹5,000 SIP on the 10th since Jan 2024, 2,150.432 units, NAV ₹92.15.

| Step | Value |
|---|---|
| Instalments, Jan 2024 – Oct 2026 | 34 |
| Invested | ₹1,70,000 |
| Current value (2,150.432 × 92.15) | ₹1,98,162 |
| Gain | ₹28,162 (+16.57%) |
| XIRR | 11.25% |

### 5. Gold (jewellery, coins, bars, digital gold, Sovereign Gold Bonds)

**Inputs:**
- form;
- weight in grams;
- purity (24K / 22K / 18K);
- purchase date;
- purchase amount, with making charges and GST entered separately (optional);
- jeweller/bill no.

For SGB instead: units (grams), issue price, issue date and series.

**Shared rates:** one "Gold rate today" entry at the top of the section, per gram for 24K, with an optional separate 22K rate (Indian jewellers quote it separately). Updating the rate revalues every gold record and stores a rate history point.

**Calculation:**
- Value = weight × rate for that purity. If no 22K rate is entered, it is derived as 24K × 22/24 (91.6%).
- Gain = value − purchase amount.
- A note explains that making charges and GST are not recovered when jewellery is sold.
- SGB: value = units × rate. Interest = 2.5% p.a. on the issue price, paid half-yearly; it shows interest received so far and the 8-year maturity date.
- Section totals: total grams by purity, and total value.

**Example:** 20 g of 22K at a 24K rate of ₹10,000/g → 20 × 10,000 × 22/24 = ₹1,83,333.

### 6. Silver

**Inputs:**
- form (articles, coins, bars);
- weight in grams;
- purity (999 / 925 / custom %);
- purchase amount and date.

**Shared rate:** "Silver rate today", per kg (as quoted in India).

**Calculation:** value = grams ÷ 1000 × rate per kg × purity. Then gain, and total grams.

**Example:** 250 g, 999 purity, at ₹1,20,000/kg → ₹29,970.

### 7. Land & Property (plot, house, flat, agricultural land, commercial)

**Inputs:**
- type;
- address/location, village/taluk/district;
- survey no., patta no.;
- area and unit: sq ft, sq m, cent, ground, acre or hectare;
- purchase date and purchase price;
- stamp duty + registration, brokerage, and other costs (legal, development);
- current estimated value, with an "as of" date and a source (own estimate, guideline value or valuer);
- rental income per month (optional);
- loan against it (optional, outstanding amount).

**Calculation:**
- Area in sq ft, using 1 cent = 435.6 sq ft, 1 ground = 2,400 sq ft and 1 acre = 100 cents.
- Total cost = price + stamp duty/registration + brokerage + other.
- Gain = current value − total cost.
- CAGR = (current ÷ cost)^(1/years) − 1.
- Price per sq ft, at purchase and now.
- Net equity = current value − outstanding loan.
- Rental yield = rent × 12 ÷ current value.

**Example:** a 5-cent plot bought 15 Jun 2019 for ₹12,00,000.

| Step | Value |
|---|---|
| Area | 5 × 435.6 = 2,178 sq ft |
| Price per sq ft at purchase | ₹551 |
| Total cost (12,00,000 + 1,08,000 + 20,000) | ₹13,28,000 |
| Current estimate | ₹20,00,000 |
| Gain | ₹6,72,000 |
| CAGR over 7.35 years | 5.73% |

**Suggested document checklist:** sale deed, patta, chitta/adangal, EC, property tax receipt, approved plan. Each item is ticked automatically when an attachment is tagged with it.

### 8. Other schemes

Each sub-type has a small, specific form.

| Sub-type | Inputs | Calculation |
|---|---|---|
| PPF / EPF / NPS / Sukanya Samriddhi | balance as of date, yearly contribution, rate, account open date | balance; years to maturity (PPF 15 yrs, SSY 21 yrs); projected maturity if the contribution continues |
| Insurance (LIC endowment / money-back / ULIP) | policy no., premium, frequency, start, term, sum assured, bonus so far | premiums paid so far, premiums left, next due date, total payable |
| Chit fund | chit value, monthly instalment, members/months, months paid, prize amount taken (if any), dividends received | paid so far, remaining instalments, net cost = paid − dividends |
| Bonds / debentures | face value, coupon %, payout frequency, maturity | same as a payout FD |
| Crypto / foreign stocks / other | invested, current value | gain and % |
| Loan given to someone | person, amount, date, interest % (optional), received back so far | outstanding amount, interest due |

## Visuals (every section has its own)

These reuse the patterns in [Charts.kt](../app/src/main/java/com/example/ui/components/Charts.kt) (`CategoryDonutChart`, `MonthlyBarChart`, `MonthlySpendingTrendChart`).

**Colours:**
- Each section has one fixed colour, used everywhere for that section: the summary donut slice, the section header accent and its charts.
- Gain is green and loss is red, with a +/− sign as well as colour.
- Light and dark theme colours are both checked.

| Section | Section header visual | Record detail visual |
|---|---|---|
| Summary | Allocation donut by section; net-worth trend line (from value snapshots) | — |
| Fixed Deposit | **Maturity timeline:** one horizontal bar per FD from start to maturity, with a "today" marker | Stacked bar: principal vs interest; growth curve to maturity |
| Recurring Deposit | Progress bar per RD (instalments paid ÷ total) | Line chart: amount deposited vs value, month by month to maturity |
| Stocks | Allocation donut by holding (or sector); P/L bar per holding, green above zero and red below | Invested vs current bar; price-update history line |
| SIP / MF | Invested vs current value per fund (paired bars) | Line chart: cumulative invested (step line) vs value at each NAV update |
| Gold | Grams by form (stacked bar: jewellery/coin/bar/SGB); gold rate history line | Purchase price vs value today |
| Silver | Grams by form; silver rate history line | Purchase price vs value today |
| Land & Property | Cost vs current value per property (paired bars) | Cost breakdown (price / stamp duty / brokerage / other) vs current value; value history line |
| Other schemes | Donut by sub-type | PPF/EPF balance history line; insurance and chit progress bars |

**Requirements for every chart:**
- An empty state: "Add an FD to see its maturity timeline".
- Tap a bar or slice to see its exact value.
- `contentDescription` semantics that read out the values.
- Amounts in lakh/crore format, e.g. ₹1.83 L.

**Where history comes from:** every time the user changes a current value, price, NAV or rate, a snapshot row is saved. Trend lines use these snapshots and need at least 2 points. Until then they show "Update the value later to see a trend".

## Attachments

- **Ways to add:**
  - **PDF** with `OpenDocument("application/pdf")`;
  - **photos** from the gallery with `PickVisualMedia`;
  - a **camera** photo with `TakePicture`, through the existing FileProvider.
- **Stored inside the app**, not as links. Each file is copied to `filesDir/savings_attachments/<recordId>/`.
  - The existing transaction receipts store only the picker's `content://` URI. That stops working after a reboot or when the source app deletes the file, so savings attachments must not repeat it. Moving receipts to copied files can follow later.
- **Photos** are compressed to about 2000 px on the long side (JPEG at 85% quality). PDFs are kept as they are. Size limit: 25 MB per file.
- **Each attachment has:**
  - a label (e.g. "FD receipt", "Sale deed", "Gold bill");
  - an optional document-type tag (used by the property checklist);
  - the date it was added.
- **Viewing:**
  - photos open full screen with pinch-zoom (Coil is already a dependency);
  - PDFs show a first-page thumbnail (Android `PdfRenderer`) and open in the phone's PDF viewer through FileProvider;
  - a Share option is available.
- **Deleting** a record deletes its files. Deleting a single attachment asks for confirmation.
- **Privacy:**
  - `savings_attachments/` is excluded from Android cloud backup and device transfer, matching the existing rule for the database in `data_extraction_rules.xml`;
  - app lock (Phase 5) also protects this page.

## Data model (DB v10, new tables only)

The migration adds tables only and touches no existing data.

- **`savings_items`**
  - **Common columns:** id, section, subType, name, holder, institution, refNo, nominee, startDate, maturityDate, status, notes, createdAt, updatedAt.
  - **Stored for list totals and sorting:** investedAmount, currentValue.
  - **`detailsJson`** holds the section-specific inputs. They are parsed into a Kotlin sealed class (`FdDetails`, `RdDetails`, `StockDetails`, …) with `org.json`, as `DataExporter` already does. This way, adding a field later does not need a schema migration.
- **`savings_entries`:** id, itemId (foreign key with cascade delete), date, type (INVEST / WITHDRAW / INCOME), amount, units (nullable), unitPrice (nullable), note. `investedAmount` on the item is recalculated from these entries whenever they change.
- **`savings_attachments`:** id, itemId (foreign key with cascade delete), fileName, mimeType, relativePath, sizeBytes, label, docTag, addedAt.
- **`savings_snapshots`:** id, itemId (nullable), key (e.g. `GOLD_24K`, `SILVER_KG`, or an item's value), value, date. Powers the trend lines and holds the shared gold and silver rates.
- **New code:**
  - `SavingsDao` and a `SavingsRepository`;
  - `SavingsCalculator.kt`: pure functions that return both the result and the list of calculation steps shown in the Calculation card;
  - a `SavingsViewModel`. It is kept out of the 733-line `ExpenseViewModel`, in line with the Phase 2 split.

## Backup and restore

The current JSON backup cannot hold files, so this needs:
- a **ZIP backup**: `backup.json` (now including savings records and snapshots) plus `attachments/`;
- restore that unpacks the ZIP and rewrites the attachment paths;
- the planned scheduled auto backup (Phase 5) to write the same ZIP.

Without this, a phone reinstall (the only update path, since there is no keystore) would lose the land and FD documents. **This must ship together with the page.**

## Build order

**Done (2026-10-20): step 1.**
- **Database v10:** `MIGRATION_9_10` adds the four tables. `SavingsMigrationTest` builds a real v9 database from `9.json`, migrates it, and lets Room validate the result.
- **Calculations:** `data/savings/` holds `SavingsCalculator`, `SavingsValuation` and `SavingsRepository`. `SavingsCalculatorTest` reproduces every worked example in this plan.
- **Tests:** all 76 unit tests pass, and ktlint is clean.

**Done (2026-10-20): step 2.**
- **Savings tab:** added as the 4th of 5 tabs (`ui/savings/`, with its own `SavingsViewModel`).
- **List page:**
  - summary card (value, invested, gain, net worth, maturing next);
  - holder chips and By type / By holder grouping;
  - status filters;
  - collapsible sections, all remembered in `savings_prefs`.
- **FD and RD:**
  - **Form:** full screen, and asks before discarding typed input.
  - **Detail page:** numbered Calculation card, Details, and an investment history editor with per-FY totals and a running total.
  - **Delete:** asks for confirmation, then offers Undo.
- **Sections:** only FD and RD show until their forms exist (`availableSections` in `SavingsUi.kt`).
- **Tested on the Elo device (copy of real data):**
  - v9→v10 migration kept all 456 transactions and the 3 balances.
  - The FD and RD figures match the worked examples.
  - Deleting a missed RD month updated "5 of 12".
  - Undo restored a deleted record with its history.

**Done (2026-10-20): step 3.**
- **All 8 sections** now have forms, calculation steps and detail rows: Stocks, SIP / Mutual Funds, Gold (including SGB), Silver, Land & Property, and Other schemes (PPF, EPF, NPS, Sukanya, insurance, chit fund, bond, money lent, crypto, other).
- **Gold and Silver:**
  - each section has a rate row: today's rate, when it was entered, and total weight by purity;
  - entering a rate revalues every item and adds a point to its value history.
- **SIP:** a fund can have a SIP, a one-time amount, or both. Both go into the investment history (`MutualFund.lumpSum`). Other assets record what was put in (`Generic.invested`).
- **List:** once any record exists, empty sections show only their header and + button.
- **Tested on the Elo:**
  - a 22K necklace at a 24K rate of ₹10,000 shows ₹1,83,333.33;
  - the 5-cent plot matches every figure in the Land example above.
- **Tests:** 81 unit tests pass, and ktlint is clean.

1. Database v10 (items, entries, attachments, snapshots) and its migration test; `SavingsCalculator` with unit tests for every worked example above (FD, RD, SIP XIRR, gold, silver, land).
2. Savings tab (5th tab), summary card, a collapsible section list, holder filter chips and group-by-holder; the investment history editor; the add/edit form for FD and RD; the detail screen with the Calculation card.
3. Stocks, SIP, gold and silver (with shared rates); Land & Property; Other schemes.
4. Charts for each section, plus value snapshots.
5. Attachments: pick, camera, copy, view, delete, plus the property checklist.
6. ZIP backup and restore with attachments; exclude the folder from cloud backup.
7. Maturity reminders (optional): reuse `BillAlertWorker` to notify 7 days before an FD, RD or policy matures.
8. Test with demo records on the Elo device (`A265EM0049`). Then, on the phone (`ZD222Q5JZY`): take a backup first, then uninstall, install and restore, and confirm that the records and attachments are all there.

## Decisions (2026-10-20)

1. **Tab placement:** Savings becomes a 5th bottom tab now: Overview · Ledger · Planning · Savings · Settings. When Phase 2 reorganises navigation, it stays a tab: Home · Transactions · Budgets · Savings · More.
2. **No link to the ledger.** The user records RD, SIP and other investment debits in the ledger separately, by hand. The Savings page never creates, changes or excludes ledger transactions. Its purpose is to show how much was invested, on which date, and what it is worth now.
3. **Net worth** appears only on the Savings page, not on Home.
4. **Holders:** each record has a holder name, entered by the user. The page can filter by holder and group by holder.
