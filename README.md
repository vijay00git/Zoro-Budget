# Smart Budget & Expense Tracker

Smart Budget is an Android application built with Kotlin and Jetpack Compose for managing personal finances in a simple, visual way. The app helps users track accounts, categorize spending, monitor budgets, review recurring bills, and import transactions from SMS messages.

## Overview

This project is a mobile-first budgeting app designed for everyday money management. It combines local persistence, interactive charts, and a modern Material 3 interface to make financial tracking easier.

## Key Features

- Multi-account support for checking, savings, credit card, and cash-style accounts
- Manual transaction entry and editing
- SMS-based transaction import with pending-review workflow
- Category-based budgeting and monthly spending insights
- Savings goal tracking and low-balance alerts
- Recurring bill and subscription management
- Reports and overview cards for income, expenses, and net balance
- Backup and CSV export support
- Theme support for light, dark, or system mode

## Tech Stack

- Kotlin
- Jetpack Compose + Material 3
- AndroidX Lifecycle + ViewModel
- Room for local database storage
- Coroutines and StateFlow
- Retrofit, OkHttp, Moshi for networking-related support
- Firebase/Google services integration support in the build configuration

## Project Structure

- app/src/main/java/com/example/MainActivity.kt — app entry point and navigation shell
- app/src/main/java/com/example/ui/ — Compose screens, dialogs, and UI components
- app/src/main/java/com/example/data/ — repositories, database access, and SMS parsing logic
- app/src/main/java/com/example/data/local/ — Room entities, DAOs, and database setup
- app/src/main/java/com/example/data/sms/ — SMS parsing and receiver logic
- app/src/test/ — unit and screenshot-based tests

## Getting Started

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 11 or newer
- An Android emulator or physical device

### Setup

1. Clone the repository.
2. Open the project in Android Studio.
3. Sync Gradle dependencies.
4. Create a local .env file based on .env.example if you want to configure environment secrets for the build.
5. Run the app on an emulator or device.

### SMS Import

The app requests SMS permissions to support automatic transaction import. On Android devices, you may need to grant SMS access manually when running the app.

## Build and Run

From the project root, you can build the app using:

```bash
./gradlew assembleDebug
```

To install it on a connected device or emulator:

```bash
./gradlew installDebug
```

## Notes

- The app seeds sample accounts, categories, and transactions on first launch so the UI has realistic data to display.
- Release builds expect signing environment variables such as KEYSTORE_PATH, STORE_PASSWORD, and KEY_PASSWORD to be available.
- The project currently focuses on local-first finance management, with SMS import as a key enhancement.

## License

This project is intended for personal and educational use. Please review the repository settings and licensing details before distributing it.
