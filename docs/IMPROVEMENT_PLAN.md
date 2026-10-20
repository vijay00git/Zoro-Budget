# Smart Budget improvement plan

Drafted 2026-10-20 from:
- a full code audit;
- screenshots of the current build on the Elo test device;
- aggregate statistics from the user's real data (backup `~/zoro-budget-backups/20261020-154847`).

## What the real data shows

| Fact | Number | Consequence |
|---|---|---|
| Transactions from SMS | 453 of 456 (99%) | SMS import is the product; manual entry is rarely used |
| SMS imports dismissed by the user | 221 (48%) | Review is mostly cleaning up junk |
| Dismissed SMS that were not transactions | ~197: 72 telco call-charge alerts, 41 Tamil/Hindi notices, 8 bill-due reminders, 5 OTPs, Splitwise invites… | The parser treats **any** SMS with a sender as financial (`SmsParser.kt:42-45`) |
| Kept SMS whose debit/credit type was wrong | 0 of 232 | HDFC "Sent Rs…" parses correctly today. Card spends and ICICI-style UPI would not (credit keywords are checked first, as plain substrings) |
| Custom categories | 13 of 23 (Ulundai, Non-Veg, Vegetable, snacks…) | Categories are personal; learning from past choices matters |
| Recurring bill templates | 1 (₹825) | Its amount was debited when it was created, and "Mark paid" would debit it again |

## Phase 1: fix what makes the numbers wrong (before any redesign)

**Done (2026-10-20):** items 1 and 2, and the Quick Review skip from item 5.
- **Parser results on the real SMS:** all 232 kept SMS still accepted (0 debit/credit errors); 195 of 221 dismissed now rejected.
- **New banks:** detection depends on message content, not a bank list, so a bank or sender the app has never seen is still detected. An SMS no account recognises is imported as pending and flagged in review. Once the user picks its account, the sender is learned.
- **Database:** DB v8 stores the SMS sender code and the account/card digits.
- **Overview month (item 4, done):** Overview shows the current month by default, with a ‹ month › selector and a "This month" reset. It ignores the Ledger filters. Planning now uses unfiltered data too. Transfers between the user's own accounts are excluded from income and spending. Checked against the real data for October and September.
- **Recurring bills (item 3, done):** bills now live in their own table (DB v9), so creating a bill no longer moves money.
  - **Marking paid:** the user either links the SMS payment already in the ledger (so it isn't counted twice) or records a new payment. The bill then moves to its next due date and leaves "Bills to pay" until that cycle comes round.
  - **Automatic:** an incoming SMS payment of the exact amount to the bill's payee marks the bill paid on its own.
  - **Other features:** Undo snackbar, payment history, skip (with confirmation), pause, quarterly bills, per-bill reminder days, "Make this a recurring bill" from a ledger entry, and the Ledger "Bill payments" filter.
  - **Migration of the real data:** AIRTEL BLACK, with its Aug 15 and Sep 15 payments linked; balances unchanged.
  - **Not yet done:** bills are not in JSON backups yet; they go into the backup rework (item 6).
- **Overview redesign (done 2026-10-20):**
  - **New order:** balance with account switcher and quick actions → SMS to review → low balance → bills to pay → month selector → month summary (income / spent / saved) → spending by category → financial health → charts → daily spending heatmap.
  - **Category netting:** expense and income in the same category net out (₹1,000 − ₹800 = ₹200 spent), on Overview, the donut chart and Planning.
  - **Spending by category:** a planned / spent / left / unplanned summary at the top, top 5 shown with "Show all", and tapping a category opens the Ledger filtered to that category and month.
  - **Health score:** tapping it opens an explanation built from the user's own numbers.
  - **Low balance:** a redesigned card with Add money (a transfer into that account) and Change alert level; credit cards are excluded.
  - **Heatmap:** tapping a day opens that day's transactions.
  - **Moved and removed:** SMS Auto-Sync was removed from Overview (it is in Settings). The SMS test parser and the debug log capture were removed from Settings.
- **Total budget (item 5, done):** "Set total budget" now scales the existing category limits in proportion instead of flattening them to an equal split.
- **Ledger date filters (part of item 4, done):** "Today" now works, and a custom end date includes the whole day without overshooting.
- **Known gap:** a second card or account at an already-known bank (for example an HDFC credit card next to the HDFC savings account) still matches the existing account by sender keyword. An "always ignore this card" option belongs in Phase 3's review flow.

1. **SMS classifier.** A message must pass every gate below to become a transaction:
   - it comes from a bank or UPI sender (DLT headers such as `XX-HDFCBK`) or a sender the user allowed;
   - it uses transaction wording: debited, credited, sent, received, spent, withdrawn;
   - it is not an OTP, promotion, "due" or statement reminder, declined/failed notice, mandate notice or telco balance alert.

   Debit/credit detection moves to word-boundary rules where debit wins, so "debited … credited to X" is a debit and "spent on Credit Card" is a debit. The parser also extracts `Avl Bal`. This gets regression tests built from masked versions of the user's own SMS formats.
2. **Never import twice.** Add a unique index on the SMS hash and run the duplicate check inside the DB transaction.
3. **Recurring bills become their own table (DB v8).** A bill is no longer a transaction. "Mark paid" creates the single expense. The migration moves the existing template and, if the user agrees, reverses its creation debit.
4. **Ledger filters stop changing Home and Planning.** Those screens get unfiltered, month-scoped data.

   Date-filter fixes:
   - the "Today" filter, which currently shows everything;
   - the custom end date, which runs a day too far;
   - the Home daily chart, which sums every month's same date together.

   Home totals switch from all-time to this month.
5. **Quick Review** skips every second item; it walks a list that shrinks as items are removed, so it needs a stable queue. "Set total budget" overwrites every category limit without warning.
6. **Backup and restore.**
   - Export every field, including dismissed rows, so a restore does not re-import old SMS.
   - Restore replaces existing data instead of appending to it (with a confirmation step) and uses a file picker.
7. **Defaults.**
   - Currency defaults to ₹ with lakh/crore grouping (1,00,000).
   - Demo data appears only on an explicitly chosen demo install.
   - Credit cards are excluded from low-balance alerts.
   - Deleting an account asks for confirmation and handles its transactions.

## Phase 2: navigation and structure
- Move to Navigation-Compose so Back works and the selected tab survives rotation. Use a NavigationRail on wide screens.
- Tabs: **Home · Transactions · Budgets · More**.
  - Accounts get their own screen.
  - Budgets, savings goal and category limits move out of Settings.
  - The SMS simulator and debug log go behind a hidden Developer section.
- Add first-run onboarding: currency, accounts with their SMS sender and last-4 digits, SMS permission with an explanation, and a sync-from date.
- Split the 733-line ViewModel into Transactions, Accounts, Budgets and Settings ViewModels, each exposing one UI-state object.

## Phase 3: Home, forms and the review flow
- **Home, top to bottom:**
  1. Pending-review banner.
  2. Balance.
  3. This month: spent against budget.
  4. Recent transactions.
  5. Next 3 bills.
  6. Alerts.

  Charts and the health score move to Budgets.
- **Review inbox:** a single flow for every entry point, with Keep / Change category / Ignore, swipe gestures and Undo. Add "Always ignore this sender" and "Always use this category for this payee".
- **One transaction form** for both Add and Edit:
  - a full-screen sheet with the amount first;
  - amount, date and type are editable;
  - a category must be chosen;
  - inline validation and the M3 date picker.
- **Shared components** to replace about 600 duplicated lines: AmountField, CategoryPicker, AccountPicker, DateSelector, RecurrenceEditor, SingleAmountDialog, ColorSwatchPicker.
- Undo snackbars and confirmations for every destructive action.

## Phase 4: visual polish and accessibility
- **Visual system:** full M3 colour scheme (with dynamic colour as an option), a real type scale, one card style, fixed dark-mode colours, and pending amber with readable contrast.
- **Charts:** axes, month scoping, transfers excluded from spending, semantics for screen readers.
- **Accessibility and scale:** 48dp touch targets, no hardcoded sp sizes or colours, layouts that hold up at large font sizes and on the 1920×1080 test device.
- **Performance:** hoisted formatters, pre-built lookup maps, lazy lists and correct `pointerInput` keys.

## Phase 5: new features

**User priority (2026-10-20):** first app lock + scheduled auto backup, then budget history + multiple savings goals. The rest are later.

- Balance reconciliation from the bank's "Avl Bal", with drift warnings.
- Learned rules: payee → category and sender → ignore.
- An Indian merchant dictionary: Swiggy, Zomato, BigBasket, IRCTC, Jio, TNPDCL…
- Budget history and rollover; multiple savings goals.
- Linked transfers, with self-transfer detection between the user's own accounts.
- App lock (biometric) and scheduled automatic backup to a chosen folder.
- Credit card statement cycle and limit; EMI tracking; split expenses.

## How each phase is tested
1. Unit tests: parser fixtures, repository, migration v7→v8.
2. Install on the Elo test device (`A265EM0049`), first with demo data, then with a copy of the real backup if the user agrees.
3. Only after that, the phone (`ZD222Q5JZY`): restore from the backup and confirm the transaction count and per-account balances match the backup.
