# Spendly website copy

## Hero

**H1:** Private expense tracker for Android

**Lead:** Record expenses quickly, review monthly spending, and let optional Android access identify supported payment sessions and local payment alerts.

**Fact:** Android 10+ · No internet permission · Local encrypted database

**Action:** Download Spendly

## How payment tracking works

Spendly uses optional Usage Access to detect supported UPI app sessions. With Notification Access, it parses payment alerts on your phone. If a session ends without a matching transaction, a local reminder lets you confirm, dismiss, or postpone it.

## Activity

Search by merchant, note, or purpose. Filter by date, type, category, or source. When more than one record may describe the same transaction, Spendly compares UTR, amount, and time, then asks you to resolve an ambiguous match.

## Privacy

Your money stays local. Spendly has no internet permission, cloud sync, or remote telemetry. It does not request SMS-reading access or use an Accessibility Service.

## Monthly insights

Compare income and expenses, check daily averages, and review spending by category. Spendly calculates the monthly view on your device, so insights work offline.

## Database and app lock

Room stores transactions in a SQLCipher-encrypted database. A random database passphrase is protected by an AES-GCM key in Android Keystore. Optional biometrics or a PIN can lock the app and hide its screen in screenshots and the task switcher.

## Backup

Export a password-protected local backup and restore it on your device. The file uses PBKDF2 with 100,000 iterations, AES-256-GCM encryption, and SHA-256 integrity validation.

## Categories and merchants

Start with 12 default categories. Correct a merchant once and Spendly can learn that correction in its on-device dictionary.

## Questions

**Which Android versions work?** Android 10 and later.

**Does Spendly read SMS?** No. It does not request READ_SMS or RECEIVE_SMS. Payment alerts are parsed through optional Notification Access.

**Does Spendly use cloud storage?** No. Transactions stay in encrypted app storage on the phone. A backup is a separate encrypted file you choose to export.

**What is this download?** The site links Spendly version 1.0.1 as a debug APK, about 80 MB. This is a local project build; a public store listing or signed release download is not available here.

## Close

**Heading:** Download Spendly

**Fact:** Version 1.0.1 · Debug APK · Android 10+

**Action:** Download Spendly
