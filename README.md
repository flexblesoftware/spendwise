# SpendWise 💳
> **Advanced Offline Expense Tracker for Android**  
> Package: `com.shahid.spendwise` | Architecture: Clean Architecture + MVVM + Jetpack Compose + Room + Hilt

---

## 🌟 Features Overview

- **Offline-First:** All financial data stays strictly on your device. Zero cloud sync required.
- **Accurate Money Math:** Money stored strictly as `Long` minor units (cents/paise) to prevent floating-point calculation errors.
- **Expenses & Incomes:** Add, edit, filter, search, custom categories, tags, receipts via CameraX.
- **Budgets & Recurring:** Monthly budgets with progress indicators, recurring bills (rent, EMIs, subscriptions) auto-logged by WorkManager.
- **Multi-Currency:** Multi-currency entries with offline exchange rate conversions to your base currency.
- **Split Bills & Debts:** Track "Who owes whom", settle up, and track lent/borrowed money with repayments.
- **Savings Goals:** Set goals, deadlines, contribution logs, and visual progress rings.
- **Insights & Visuals:** Category breakdown, month-over-month comparisons, trends, and Sunday weekly summary alerts.
- **Voice Entry:** Natural language speech parser (e.g., `"250 coffee"`, `"rent 15000 yesterday"`).
- **Import/Export & Tax:** JSON ZIP backup/restore via Storage Access Framework (SAF), CSV export/import with column mapping, PDF reports & Tax summaries.
- **Biometric Security:** Biometric app lock with timeout, plus per-entry privacy lock.
- **In-App Self-Updating:** Checks GitHub Releases for updates; displays a blocking update screen with animated progress and a persistent foreground service download notification.

---

## 🔐 GitHub Actions Secrets Setup

For automated signed APK releases, configure these four repository secrets in:  
**Settings ➔ Secrets and variables ➔ Actions ➔ New repository secret**  
(`https://github.com/flexblesoftware/spendwise/settings/secrets/actions`)

| Secret Name | Description |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded string of `spendwise-release.jks` (`base64 -w 0 spendwise-release.jks`) |
| `KEYSTORE_PASSWORD` | Store password for `spendwise-release.jks` |
| `KEY_ALIAS` | Key alias (default: `spendwise`) |
| `KEY_PASSWORD` | Key password for alias `spendwise` |

> ⚠️ **Important:** The signing keystore must NEVER change after the first release. If it changes, existing app installs will fail Android's signature verification and cannot update.
> Keystore credentials are saved locally in `spendwise-signing-backup.txt` (and backed up to `~/storage/shared/Projects/spendwise/`).

---

## 🚀 Release Process

To create a new signed release:
1. Create and push a Git tag matching `v*.*.*`:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
2. GitHub Actions will automatically:
   - Compute `versionName = "1.0.0"` and `versionCode = 10000` (`major*10000 + minor*100 + patch`)
   - Decode `KEYSTORE_BASE64` to `spendwise-release.jks`
   - Build a signed release APK with Proguard/R8 shrinking
   - Rename it to `SpendWise-v1.0.0.apk`
   - Create a GitHub Release with auto-generated notes and attach the signed APK asset.

---

## 🛠️ Tech Stack

- **UI:** Jetpack Compose, Material 3, Navigation Compose
- **Architecture:** MVVM, Clean Architecture, Kotlin Coroutines, Flow
- **Dependency Injection:** Hilt
- **Local Storage:** Room Database, Encrypted DataStore
- **Background Tasks:** AndroidX WorkManager
- **Camera:** CameraX for receipt photos
- **Security:** AndroidX Biometric with device credential fallback
- **Network:** OkHttp 4.12.0 for GitHub Release queries and APK downloads
- **Build System:** Gradle Kotlin DSL with Version Catalog (`gradle/libs.versions.toml`)

---

## 👤 Author
Developed with ❤️ by **MD Shahid**.
