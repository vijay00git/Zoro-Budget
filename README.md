# Smart Budget

[![Android CI](https://github.com/vijay00git/Zoro-Budget/actions/workflows/android-ci.yml/badge.svg)](https://github.com/vijay00git/Zoro-Budget/actions/workflows/android-ci.yml)
[![Latest release](https://img.shields.io/github/v/release/vijay00git/Zoro-Budget?label=latest%20APK)](https://github.com/vijay00git/Zoro-Budget/releases/latest)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
![Android 7.0+](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)

An Android budget and expense tracker that builds your ledger from bank SMS. It reads debit and credit messages from any bank, files them under the right account and category, and shows where the month's money went. Your savings and assets (FDs, RDs, stocks, gold, land and more) live in the same app. Everything is stored on the phone.

Built with Kotlin and Jetpack Compose.

## Contents

- [Features](#features)
- [Install](#install)
- [Build from source](#build-from-source)
- [How it works](#how-it-works)
- [Privacy and permissions](#privacy-and-permissions)
- [Project structure](#project-structure)
- [Releases and CI](#releases-and-ci)
- [Contributing](#contributing)
- [Roadmap](#roadmap)
- [License](#license)

## Features

**Automatic ledger from SMS**
- Detects transaction SMS by their wording (debited, credited, sent, received, spent, withdrawn), so a bank or sender the app has never seen is still picked up. There is no fixed bank list.
- Filters out OTPs, promotions, due-date reminders, declined payments and telecom balance alerts.
- Matches each message to an account by sender and the last four digits. An unknown card or account is flagged once, and the app learns it after you confirm.
- Never imports the same SMS twice. Messages you dismiss stay dismissed, even after a restore.
- A review queue for anything that needs a category or a second look.

**Overview**
- Total balance, hidden behind an eye button until you choose to show it. Account tiles open on request.
- Low-balance, SMS-review and financial-health tiles.
- The month's spending as a ring against your budget, plus a countdown to the next bill.
- Spending by category with progress bars, a six-month trend chart, and a daily spending heatmap.

**Ledger**
- Every transaction, grouped by day, with each day's total.
- Search, quick filters (date, type, category) and a filter sheet for account and amount range.
- A running money in / money out / net summary for whatever is currently shown.

**Planning**
- Monthly cash flow, an overall budget ring and a savings goal.
- One tile per category with its limit; over-budget and near-limit categories come first.
- Recurring bills: link the SMS payment that already arrived, so it isn't counted twice. Bills are marked paid automatically when a matching payment SMS comes in. Undo, skip, pause and payment history are included.

**Savings and assets**
- **Sections:** fixed and recurring deposits, stocks, SIPs and mutual funds, gold, silver, land and property, and other schemes (PPF, EPF, NPS, Sukanya Samriddhi, insurance, chit funds, bonds, money lent, crypto).
- **Calculations shown step by step:** FD compounding, RD instalments, mutual-fund XIRR, gold valued from 24K/22K rates, land CAGR, and more.
- **Investment history:** a dated history of what you put in, with totals per financial year.
- **Holders:** a holder on every record (yourself, family members), with filtering by holder.
- **A record, not a total:** gold, silver and land can be recorded without a weight or price, and are kept out of the money totals.

**Your data**
- One-file full backup (ZIP) of everything: transactions, bills, savings, settings and attachments. Restore checks the file first, keeps a copy of the current data, then replaces it.
- CSV and JSON export.

**Design**
- Bento-style tiles, a colour and icon for every account and category, and light and dark themes that follow the phone.

## Install

1. Open the [latest release](https://github.com/vijay00git/Zoro-Budget/releases/latest) on your phone.
2. Download `smart-budget-build-N.apk` and open it. Android may ask you to allow installs from your browser or file manager.
3. Open Smart Budget and allow SMS access when asked.

Each new release installs over the previous one as an update and keeps your data.

> The release APKs are debug-signed builds for personal use. They are not on Google Play.

## Build from source

**Requirements**
- JDK 17
- Android SDK with platform 36 (Android Studio installs it)
- An Android 7.0+ device or emulator

**Steps**

```bash
git clone https://github.com/vijay00git/Zoro-Budget.git
cd Zoro-Budget
./gradlew assembleDebug        # APK at app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug         # install on a connected device
```

**Checks** (the same ones CI runs):

```bash
./gradlew ktlintCheck          # code style
./gradlew testDebugUnitTest    # unit tests (Robolectric, runs on the JVM)
```

**Signing:** debug builds use `debug.keystore` in the project root if it exists, otherwise your default Android debug key. Use the same key every time if you want to update an installed build without uninstalling it. The keystore is listed in `.gitignore`; never commit it.

## How it works

**SMS to ledger**
1. `SmsReceiver` handles new messages, and `SmsInboxScanner` reads past ones from a date you choose.
2. `SmsParser` decides whether a message is a transaction and extracts the amount, type, payee, last four digits and available balance.
3. `SmsAccountMatcher` picks the account.
4. The transaction is saved in the Room database. Before saving, the same transaction checks whether that exact message is already recorded, so it is never stored twice.

**Storage:** a single Room database (`smart_budget_db`, schema version 10). Every schema change has a tested migration; exported schemas are in `app/schemas/`.

**Architecture:** Compose screens on top of ViewModels (`ExpenseViewModel`, `SavingsViewModel`) and repositories (`BudgetRepository`, `SavingsRepository`). The money calculations (`SavingsCalculator`, `BillSchedule`, `CategoryTotals`, `FinancialHealth`) are plain Kotlin with unit tests.

## Privacy and permissions

Your financial data stays on your phone:
- The app's own code does not send data anywhere.
- The database is excluded from Android cloud backup and device transfer.
- Backups are files you create and keep yourself.

| Permission | Why |
|---|---|
| `READ_SMS` | Scan past bank messages from the sync date you set |
| `RECEIVE_SMS` | Record new bank messages as they arrive |
| `POST_NOTIFICATIONS` | Bill reminders and alerts |

**Known issue:** the build still includes Firebase, Retrofit and OkHttp, left over from the project template. The app never uses them, but they add `INTERNET` and a few other permissions to the installed APK. Removing them is on the roadmap.

## Project structure

```
app/src/main/java/com/example/
├── MainActivity.kt            # app shell and bottom navigation
├── data/
│   ├── local/                 # Room database, entities, DAOs, migrations
│   ├── repository/            # BudgetRepository
│   ├── sms/                   # SMS parsing, inbox scan, account matching
│   ├── savings/               # savings models, calculator, repository
│   ├── backup/                # full ZIP backup and restore
│   ├── export/                # CSV and JSON export
│   ├── notifications/         # bill reminders (WorkManager)
│   └── utils/                 # bill schedule, category totals, health score
└── ui/
    ├── screens/               # Overview, Ledger, Planning, Settings
    ├── savings/               # Savings tab, forms, calculation cards
    ├── components/            # bento tiles, charts, heatmap, nav bar
    ├── dialogs/               # add/edit dialogs and pop-ups
    └── theme/                 # colours, accents, typography
app/src/test/                  # unit tests
app/schemas/                   # exported Room schemas
docs/                          # plans and design notes
```

## Releases and CI

`.github/workflows/android-ci.yml` runs on every push and pull request:
1. ktlint and unit tests;
2. building the APK.

On a push to `main` it also publishes a GitHub Release, `build-N`, with `smart-budget-build-N.apk` attached. The app's version is `1.0.N`; Settings shows it at the bottom.

Release builds are signed with the key stored in the repository secret `DEBUG_KEYSTORE_BASE64`, so each one installs over the last. If the secret is missing, a push fails instead of releasing an APK that couldn't update the installed app.

## Contributing

Issues and pull requests are welcome.

1. Fork the repo and create a branch from `main`.
2. Make your change. Follow the existing style; `./gradlew ktlintFormat` fixes most formatting.
3. Add or update unit tests for any logic you change (parsing, calculations, migrations).
4. Run `./gradlew ktlintCheck testDebugUnitTest` before opening the pull request.
5. If you change the database, bump the version, add a migration, and commit the new schema in `app/schemas/`.

**When reporting a bug**, include:
- the Android version;
- what you did and what you expected;
- for SMS parsing problems, a **masked** copy of the message, with amounts, account numbers and names changed.

## Roadmap

Planned work is tracked in [`docs/`](docs/):

- **[Improvement plan](docs/IMPROVEMENT_PLAN.md):**
  - app lock with fingerprint or PIN;
  - scheduled automatic backups;
  - learned rules ("always put this payee in this category");
  - budget history and multiple savings goals.
- **[Savings plan](docs/SAVINGS_PLAN.md):** PDF and photo attachments, and charts for each savings section.
- **[Redesign plan](docs/REDESIGN_PLAN.md):** a new app icon, plus the remaining polish.
- **Cleanup:** remove the unused network libraries.

## License

[MIT](LICENSE) © 2026 Vijay
