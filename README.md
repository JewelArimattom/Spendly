# Spendly — Privacy-First Automatic Money Tracker for Android

Spendly is a production-quality, offline-first personal finance and expense tracking application for Android designed around a simple core philosophy:
**Track money with minimum user effort, detect transactions automatically from native signals, and never let financial data leave the user's phone.**

Unlike traditional personal finance apps that demand cloud account registrations, invasive SMS reading permissions, or third-party servers, Spendly operates **100% offline and stores all data inside an AES-encrypted database protected by Android Keystore.**

---

## 🌟 Key Highlights

- **🔒 Local-First & Zero Cloud**: No Firebase, no Supabase, no external APIs, no remote telemetries, no cloud sync. All transaction and financial history lives strictly on the local device.
- **🛡️ SQLCipher + Android Keystore Encryption**: The Room database is encrypted with SQLCipher using a 256-bit passphrase wrapped with an AES-GCM key inside the hardware-backed Android Keystore.
- **⚡ Automatic UPI Session Detection**: Native `UsageStatsManager` engine identifies when the user opens and finishes payment sessions in supported UPI apps (Google Pay, PhonePe, Paytm, BHIM, CRED).
- **🔔 Local Payment Notification Parsing**: An extensible `NotificationListenerService` parser extracts amounts, merchant names, UPI references (UTR), and transaction types (Debit, Credit, Refund, Failure) from payment alerts locally without sending notification contents off-device.
- **💡 Smart Missing-Expense Reminders**: Intelligently posts gentle local notification prompts (*"Did you make a payment using PhonePe?"*) only when a monitored UPI session ends without an associated transaction. Includes customizable cooldown periods, daily limits, and quiet hours.
- **🏷️ Local Merchant Recognition & Learning**: Normalizes raw merchant strings (e.g. `UPI-PAYTO-ZOMATO-OKAXIS` → `Zomato`), maintains an offline dictionary of major Indian merchants mapped to 12 default categories, and learns corrections from user feedback.
- **🔄 Multi-Source Transaction Reconciliation**: Merges duplicate records originating from multiple sources (e.g., PhonePe notification + Bank SMS/notification + manual entry) based on UTR numbers, amounts, and time windows. Flags ambiguous matches for user resolution.
- **🔐 Biometric App Lock & PIN**: Supports fingerprint / face unlock (`BiometricPrompt`) and 4-digit PIN with salted SHA-256 hashing. Automatically engages `FLAG_SECURE` to prevent screenshot leaks and task switcher previews.
- **📦 Encrypted Offline Backups**: Export and restore your complete database using password-based PBKDF2 (100,000 iterations) + AES-256-GCM encryption with SHA-256 integrity validation.

---

## 🛠️ Technology Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin 1.9.24 (Coroutines 1.8.1, StateFlow) |
| **Minimum SDK** | Android 10 (API 29) |
| **Target SDK** | Android 15 (API 35) |
| **UI Framework** | Jetpack Compose (BOM 2024.06.00) + Material 3 Design System |
| **Architecture** | MVVM + Clean Architecture |
| **Dependency Injection**| Dagger Hilt 2.51.1 |
| **Local Database** | Room 2.6.1 + KSP |
| **Database Encryption**| SQLCipher for Android 4.5.7 (`net.zetetic:android-database-sqlcipher`) |
| **Key Management** | Android Keystore (AES-256-GCM authenticated key wrapping) |
| **Background Tasks** | AndroidX WorkManager 2.9.0 |
| **Usage Tracking** | `UsageStatsManager` and `UsageEvents` |
| **Notification Monitor**| `NotificationListenerService` |
| **Biometrics** | AndroidX Biometric 1.2.0-alpha05 (`BiometricPrompt`) |
| **Testing** | JUnit 4.13.2, MockK 1.13.12 |

---

## 📱 Architecture & Package Structure

Spendly follows Clean Architecture principles with clear layer separation:

```
com.spendly.app/
├── MainActivity.kt                          # Edge-to-edge FragmentActivity host
├── SpendlyApplication.kt                    # @HiltAndroidApp application entrypoint
│
├── core/
│   ├── database/                            # Room database, 7 entities, 7 DAOs
│   │   ├── dao/                             # TransactionDao, CategoryDao, MerchantRuleDao, etc.
│   │   ├── entity/                          # TransactionEntity, CategoryEntity, UpiSessionEntity, etc.
│   │   ├── Converters.kt                    # Room type converters
│   │   └── SpendlyDatabase.kt               # @Database definition
│   ├── designsystem/                        # Material 3 colors, shapes, typography, icons
│   │   ├── CategoryUiHelper.kt              # Category icons and 12 default categories
│   │   └── theme/                           # SpendlyTheme, Light/Dark color schemes
│   ├── privacy/                             # PrivacyManager (permissions check, wipe all data)
│   ├── security/                            # Security architecture
│   │   ├── AppLockManager.kt                # Lifecycle-aware PIN/Biometric lock + FLAG_SECURE
│   │   ├── BiometricAuthManager.kt          # AndroidX BiometricPrompt wrapper
│   │   ├── DatabaseKeyManager.kt            # Android Keystore key generation & wrapping
│   │   ├── EncryptedDatabaseManager.kt      # SQLCipher SupportFactory integration & DB seed
│   │   └── LocalBackupManager.kt            # AES-GCM + PBKDF2 encrypted backup export/restore
│   └── util/                                # Currency and date format utilities
│
├── detection/
│   ├── parsing/                             # Local notification parsing engine
│   │   ├── AmountParser.kt                  # Robust regex money extractor (paisa minor units)
│   │   ├── BankNotificationParser.kt        # HDFC, SBI, ICICI, Axis, Kotak, PNB parser
│   │   ├── NotificationFilter.kt            # Discards OTPs, promos, security alerts
│   │   ├── NotificationTransactionParser.kt # Strategy coordinator returning TransactionCandidate
│   │   └── UpiAppNotificationParser.kt      # GPay, PhonePe, Paytm, BHIM, CRED parser
│   ├── reminders/                           # Smart payment reminders
│   │   └── SmartReminderScheduler.kt        # Debounce timer, quiet hours, cooldown, notification
│   ├── sessions/                            # UPI app session monitoring
│   │   └── UpiAppSessionMonitor.kt          # UsageStatsManager foreground/background tracking
│   └── work/                                # WorkManager periodic jobs
│       └── SessionReconciliationWorker.kt   # Unreconciled session & reminder cleaner
│
├── domain/                                  # Domain business models and repository interfaces
│   ├── model/                               # Transaction, Category, MerchantRule, UpiSession, etc.
│   └── repository/                          # TransactionRepository, CategoryRepository, etc.
│
├── data/                                    # Data access layer & repository implementations
│   ├── di/                                  # RepositoryModule (@Binds)
│   └── repository/                          # TransactionRepositoryImpl, CategoryRepositoryImpl, etc.
│
├── features/                                # Jetpack Compose presentation layer
│   ├── activity/                            # ActivityScreen & ActivityViewModel (Search, filters)
│   ├── home/                                # HomeScreen & HomeViewModel (Balance, summaries)
│   ├── insights/                            # InsightsScreen & InsightsViewModel (Monthly trends)
│   ├── lock/                                # LockScreen (PIN pad & Biometric prompt)
│   ├── onboarding/                          # OnboardingScreen & OnboardingViewModel (Permission cards)
│   ├── quickentry/                          # QuickExpenseEntrySheet (Frictionless 1-tap entry)
│   └── settings/                            # SettingsScreen & SettingsViewModel (Preferences, backup)
│
├── navigation/                              # Navigation Compose scaffold
│   ├── SpendlyApp.kt                        # Scaffold with bottom bar, app lock overlay, quick sheet
│   ├── SpendlyNavHost.kt                    # NavHost connecting 4 tabs + onboarding
│   └── TopLevelDestination.kt               # Home, Insights, Activity, Settings destinations
│
├── receiver/                                # Broadcast receivers
│   └── ReminderActionReceiver.kt            # Handles "Yes I Paid", "No Payment", "Later" actions
│
└── service/                                 # Foreground / system services
    └── SpendlyNotificationListenerService.kt# Android NotificationListenerService implementation
```

---

## 🔒 Security & Privacy Model

### 1. Database Encryption
- When the app is launched for the first time, `DatabaseKeyManager` checks for an existing encryption key alias in the Android Keystore (`spendly_db_key`).
- If no key exists, it generates an AES-256 key with `PURPOSE_ENCRYPT or PURPOSE_DECRYPT` and `BLOCK_MODE_GCM`.
- A 32-byte cryptographically secure random database passphrase is generated, encrypted using the Keystore AES key with a 12-byte random IV, and saved to private SharedPreferences.
- `EncryptedDatabaseManager` opens the Room SQLite database using SQLCipher's `SupportOpenHelperFactory(passphrase)`.
- If the hardware key is permanently invalidated (e.g., biometric enrollment removed on lock screen), Spendly halts and informs the user rather than silently creating an unencrypted database.

### 2. Android Backup Protection
- Automatic cloud backups (`android:allowBackup="false"`) and `fullBackupContent="false"` are explicitly disabled in `AndroidManifest.xml`.
- Custom `xml/backup_rules.xml` and `xml/data_extraction_rules.xml` exclude databases, shared preferences, and datastore files from device-to-device transfer and cloud extraction.

### 3. App Lock & Window Protection
- When App Lock is enabled, Spendly applies `WindowManager.LayoutParams.FLAG_SECURE` to the application window, preventing screenshot capture and concealing app contents in Android's recent apps switcher.
- Lifecycle tracking automatically engages the lock when the application is placed in the background beyond the configurable threshold.

### 4. Permitted Permissions Policy
- ✅ **PACKAGE_USAGE_STATS**: Used strictly to detect when supported UPI applications move into foreground and background.
- ✅ **BIND_NOTIFICATION_LISTENER_SERVICE**: Used strictly to parse incoming financial payment confirmation notifications locally.
- ✅ **POST_NOTIFICATIONS**: Required for posting smart payment reminders and confirmation prompts.
- ❌ **NO READ_SMS / RECEIVE_SMS**: Avoided in MVP to protect user privacy and comply with Google Play policies.
- ❌ **NO ACCESSIBILITY SERVICES**: No screen scraping or hidden overlays.
- ❌ **NO INTERNET ACCESS**: Spendly requests **zero network permissions** (`android.permission.INTERNET` is omitted from `AndroidManifest.xml`).

---

## 🚀 Building and Running

### Prerequisites
- JDK 17 or higher
- Android SDK with Platform API 35 (Android 15)
- Gradle 8.7 (managed via Gradle Wrapper)

### Command-Line Build

```bash
# Set environment variables (Windows PowerShell)
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.12.7-hotspot"
$env:ANDROID_HOME = "C:\Users\<user>\AppData\Local\Android\Sdk"

# Run all unit tests
.\gradlew.bat testDebugUnitTest

# Assemble debug APK
.\gradlew.bat assembleDebug
```

The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## ✅ Acceptance Criteria Verification

| Requirement | Implementation Details | Status |
|---|---|---|
| **1. Track expenses without internet** | The app does not request `INTERNET` permission; all calculations occur locally. | **Verified** |
| **2. Track income and expenses** | Full support for `DEBIT`, `CREDIT`, `TRANSFER`, and `REFUND` transactions with minor-unit storage. | **Verified** |
| **3. Local storage only** | Zero cloud backends (no Firebase, no Supabase, no external telemetry). | **Verified** |
| **4. Database encryption** | SQLCipher integration with Room using random 32-byte passphrase. | **Verified** |
| **5. Keystore key protection** | Passphrase wrapped with AES-256-GCM key inside Android Keystore. | **Verified** |
| **6. No financial data sent externally** | Zero remote network endpoints; 100% offline-first. | **Verified** |
| **7. UPI application session monitoring** | `UpiAppSessionMonitor` tracking Google Pay, PhonePe, Paytm, BHIM, CRED via `UsageStatsManager`. | **Verified** |
| **8. Local payment notification parsing** | `NotificationTransactionParser` extracting amount, merchant, and UTR from payment notifications. | **Verified** |
| **9. Smart payment reminders** | `SmartReminderScheduler` gentle local alerts with cooldown, quiet hours, and daily limits. | **Verified** |
| **10. Confirm/reject reminder flow** | "Yes, I paid" (prefilled Quick Entry), "No payment" (learns non-payment), "Remind later" (snooze). | **Verified** |
| **11. Safe duplicate handling** | `TransactionReconciliationEngine` deduplicating across sources and flagging ambiguous matches. | **Verified** |
| **12. Editable categories & merchants** | Full editing support with merchant correction learning in `MerchantRecognitionEngine`. | **Verified** |
| **13. Offline monthly spending insights** | `InsightsScreen` calculating income vs expense, daily averages, and category breakdown offline. | **Verified** |
| **14. Encrypted backup & restore** | `LocalBackupManager` exporting/importing AES-256-GCM + PBKDF2 encrypted files with SHA-256 checksums. | **Verified** |
| **15. Clear all local data** | `PrivacyManager.clearAllFinancialData()` wiping records with user confirmation. | **Verified** |
| **16. Graceful permission fallback** | Friendly step-by-step onboarding explaining Usage Access and Notification Access. | **Verified** |
| **17. Non-invasive monitoring** | No Accessibility Services, no root, no overlays, no SMS reading. | **Verified** |

---

## 📄 License

Spendly is open-source software built for privacy-conscious users. All rights reserved.
