# Notium

**Your notes. Your privacy.**

Notium is an Android notes app that works **100% offline**. Your notes are stored in an encrypted database on your phone. There are no accounts, no cloud, no ads, no analytics, and the app does not even request the network permission.

- Platform: Android 8.0 or newer (`minSdk 26`, `targetSdk 35`)
- Language: Kotlin, Jetpack Compose, Material Design 3, MVVM
- App languages: English (default), Español, Italiano, 中文 (Mandarin)
- Version: 1.0.0

> **Project status:** the app is built on a Windows/Linux/macOS computer with the scripts in this folder. It has been written and reviewed carefully, but it has been tested only through the compile-and-fix cycle reported by the developer. If you find a bug, please report the error text shown by the build script or by the in-app crash screen.

---

## Table of contents

1. [Features](#features)
2. [Security model](#security-model)
3. [Function reference](#function-reference)
4. [Installation (building the APK)](#installation-building-the-apk)
5. [Running the app](#running-the-app)
6. [Usage examples](#usage-examples)
7. [Dependencies](#dependencies)
8. [Project structure](#project-structure)
9. [Limitations](#limitations)
10. [Troubleshooting](#troubleshooting)

---

## Features

### Notes
- Create, edit, and delete notes with automatic saving (the delay is configurable).
- **WYSIWYG rich text editor:** bold, italic, strikethrough, highlight, inline code, headings 1 to 3, bullet and numbered lists, checklists, quotes, indentation, dividers, date and time insertion. Formatting is shown directly, without Markdown symbols.
- Lists continue by themselves when you press Enter. Tap a checkbox to tick it.
- Double tap selects a word, triple tap selects the whole sentence. Tapping anywhere in the note puts the cursor at that point.
- Undo and redo, find and replace inside a note, word and character counters.
- Note templates: meeting, shopping list, daily plan, journal, project.
- Notes are stored as Markdown, so they can be exported and read anywhere.
- **Automatic title:** if you leave the title empty, the first sentence of the note becomes its title.

### Organization
- **Folders** (with sub-folders and colors), **tags** (with colors), **favorites** (star), **pinned notes** (pin, shown at the top of the home page), **archive**, and **trash** (notes are removed automatically after a configurable number of days).
- Quick star and pin buttons on every note card. The pin is shown only on the home page.
- Each note card shows its folder first, then its tags, and the date of the **last modification** on the right.
- Sorting by last modification, creation date, or title. 1 to 3 columns, comfortable or compact cards.
- Full-text search across titles, text, tags, and folders, with highlighted matches.
- **Multiple selection:** long press a note, or choose "Select" in its menu. Then delete, archive, favorite, or export many notes at once. Delete and archive show an **Undo** message for 5 seconds.
- Undo message (5 seconds) also for trash, archive, and move actions on a single note.

### Export and backup
- **Export one or many notes** to **PDF**, **TXT**, or **JSON**. The file is saved directly in the phone's **Download** folder. When several notes are exported, one separate file is created for each note.
- Export a note to Markdown (`.md`) or share its text from the editor menu.
- **Encrypted backups** (`.notium` files): AES-256-GCM with a key derived from a backup password using Argon2id. The file is checked for integrity and authenticity before anything is restored.
- **Import a backup** from Settings > Backup, or **at the very first start** of the app (before you create your main password).

### Security and privacy
- Encrypted database (SQLCipher, AES-256).
- Unlock with **password**, **PIN**, or **biometrics** (fingerprint or face, depending on the device).
- The PIN or password unlocks by itself as soon as the correct length is typed, with no button to press. Optional shuffled PIN keypad.
- If you do not set a PIN or biometrics, the app opens directly (the key is protected by the Android Keystore). Turning on a PIN or biometrics makes the lock mandatory again.
- Automatic lock after a configurable time, hidden previews in the recent apps list, screenshot blocking, and clipboard clearing.
- Temporary PIN lock after too many wrong attempts.
- Delete-all-data function (requires your password).

### Customization
Settings are organized in categories: Security, Privacy, Backup, Theme and colors, Home screen, Notes list, Editor, Animations, Data management, About, and Language (last).
- Themes: Dark, AMOLED black, Light, Automatic. Free accent color, corner radius, border width, card style and color, interface font and size, high contrast.
- Home: start section, grid columns, title, logo, "New note" button style and position, search bar, drawer folders and tags.
- Notes list: title size and lines, date format (short, medium, long, relative, with time), card padding and gaps, tag and folder display.
- Editor: font, size, line spacing, margins, letter spacing, auto-capitalize, auto-correct, auto-list, keep screen on, autosave delay.
- Animations: style (slide, from bottom, fade, zoom), speed, press effect, list motion, or off.
- Every settings category has "Restore defaults" for that category only.

### First start
1. Choose the language (English by default).
2. Either create your main password, or import an existing backup and then create the password.
3. Optionally turn on PIN and biometrics later in Settings > Security.

---

## Security model

| Item | Implementation |
|---|---|
| Database | SQLite with SQLCipher 4.6.1, AES-256, internal FTS5 search inside the encrypted database |
| Master key | Random key, never stored in clear text; it is *wrapped* separately for each unlock method |
| Password | Argon2id (t=3, m=32 MiB) wraps the master key with AES-256-GCM |
| PIN | Argon2id plus the Android Keystore; the PIN is rate-limited and disabled after 10 wrong attempts |
| Biometrics | BiometricPrompt with a Keystore key that requires authentication |
| No lock method | The master key is sealed with a device Keystore key so the app can open directly |
| Backup | Format `.notium`: header "NOTIUMBK", Argon2id parameters, salt, IV, AES-256-GCM over gzip-compressed JSON; the header is authenticated (AAD) |
| Network | No `INTERNET` permission. No cloud, no automatic Android backup |

The app cannot recover a forgotten password. **Create an encrypted backup** and keep it somewhere safe.

---

## Function reference

The most important functions and where they live (package `com.notium.app`).

### Security (`security/`)
| Function | What it does |
|---|---|
| `Crypto.deriveKey` | Derives a 256-bit key from a password with Argon2id |
| `Crypto.seal` / `Crypto.open` | Encrypts and decrypts data with AES-256-GCM, with authenticated header |
| `VaultManager.create` | Creates the vault: master key plus the password-wrapped copy |
| `VaultManager.unlockWithPassword` / `unlockWithPin` | Unwraps the master key; the PIN path counts failed attempts |
| `VaultManager.enablePin` / `enableAutoUnlock` | Turns on the PIN, or the Keystore-based direct opening |
| `BiometricHelper.prompt` | Shows the system biometric dialog and returns the unlocked cipher |
| `PasswordStrength.evaluate` / `problem` | Scores a password and explains why it is too weak |

### Data (`data/`)
| Function | What it does |
|---|---|
| `NotiumDatabase` | Opens the SQLCipher database with the master key |
| `Repository.saveNote` | Creates or updates a note (title, Markdown content, folder, tags) |
| `Repository.moveToTrash` / `restore` / `deletePermanently` | Trash handling |
| `Repository.setFavorite` / `setPinned` / `setArchived` / `moveNote` | Per-note flags and folder |
| `Repository.exportData` / `replaceAll` | Used by backup creation and import |
| `AutoTitle.from` | Builds a title from the first sentence of the text |
| `Prefs` / `Settings` | All app preferences, stored with DataStore |

### Backup (`backup/`)
| Function | What it does |
|---|---|
| `BackupManager.encrypt` | Serializes notes, folders and tags to JSON, compresses and encrypts them |
| `BackupManager.decrypt` | Verifies the file, the password and the content, and returns the data |

### View model (`AppViewModel.kt`)
| Function | What it does |
|---|---|
| `setupPassword` / `unlockWith*` / `lock` | First setup, unlock, and lock; after setup, imports a pending backup |
| `prepareRestore` / `confirmRestore` | Verify a backup, then replace all data after confirmation |
| `createBackup` | Writes an encrypted `.notium` file |
| `favoriteMany` / `archiveMany` / `trashMany` | Multiple-selection actions, each with a single Undo |
| `noteForExport` | Collects everything needed to export a note |
| `toggleFavorite` / `togglePinned` / `toggleArchived` / `trash` / `moveNote` | Single-note actions |

### User interface (`ui/`)
| File | Content |
|---|---|
| `AuthScreens.kt` | Language screen, setup (with backup import), lock screen with PIN keypad |
| `HomeScreen.kt` | Notes list, note cards, selection bar, drawer |
| `EditorScreen.kt` | Editor, toolbar, note menu |
| `RichText.kt` | The rich text engine (styles, lists, checkboxes, Markdown conversion) |
| `NoteExport.kt` | PDF, TXT and JSON export to the Download folder (`NoteExporter`, `rememberExporter`) |
| `PdfExport.kt` | PDF drawing with Android's `PdfDocument` |
| `SettingsScreen.kt` | All settings pages |
| `FoldersTagsScreens.kt` | Folder and tag management |
| `I18n.kt`, `I18nData.kt` | Translation system and the dictionary |
| `Theme.kt`, `Components.kt` | Theme, colors, shared widgets |

---

## Installation (building the APK)

The package contains **no build resources**. When you build, the JDK, the Android SDK, Gradle and all libraries are **downloaded from the internet** (about 3 GB the first time).

### Requirements
- Windows, Linux (x86-64) or macOS
- An internet connection during the build (components are downloaded when missing)
- About 3 GB of free disk space and 5 to 15 minutes for the first build

### Steps
1. Extract the whole `Notium-android` folder somewhere normal (not from inside the zip).
2. Open the `Notium-android` folder and run the script for your system:

| System | Script |
|---|---|
| Windows | double-click `Build-Notium-android.bat` |
| Linux | run `./Build-Notium-android.sh` in a terminal |
| macOS | double-click `Build-Notium-android.command` (if blocked: right-click, Open) |

If the scripts are not executable on Linux or macOS:
```bash
chmod +x Build-Notium-android.sh Build-Notium-android.command build-tools/build-engine.sh src/gradlew
```

3. The terminal shows a cyan progress bar and the download steps.
4. When it ends, the APK is in the `Notium-android-APK` folder, next to the `Notium-android` folder:
```
Notium-android-APK/Notium.apk
```
5. Copy `Notium.apk` to your phone and open it. Allow "Install from unknown sources" when Android asks.

### What is downloaded and where
Everything goes into `build-tools/tools` (inside this folder): Eclipse Temurin JDK 17, Android command-line tools, Android SDK platform 35, build-tools 34.0.0, Gradle and the app libraries. Later builds reuse it and only download what is missing. On Windows you are asked at the end whether to delete it; on Linux and macOS you can delete the folder yourself.

### Updating the app
The APK is signed with a local key in `build-tools/signing`. Keep that folder: with the same key a new version installs over the old one without losing notes.

### Problems
| Problem | What to do |
|---|---|
| Download failed | Check the internet connection (and any proxy or firewall) and run the script again |
| Compilation error | Read the red lines in the terminal or open `build-tools/build.log` and send them to the developer |
| Android refuses the APK | Allow unknown sources; if an older version with a different key is installed, uninstall it first |

---

## Running the app

1. Open **Notium** on the phone.
2. Pick a language, then create your main password (or import a backup first).
3. You land on the home page. Tap **New note** to start writing.
4. Use the menu (top left) to switch between All notes, Favorites, Pinned, Archive, Trash, folders, and tags.
5. Open **Settings** from the menu to turn on PIN or biometrics, change the theme, and create a backup.

---

## Usage examples

**Write a checklist**
Open a new note, tap the checklist button, type "Milk", press Enter, type "Bread". Tap a square to tick the item. No Markdown symbols appear.

**Let the title write itself**
Leave the title empty and type "Call the bank tomorrow morning. Ask about the card." The note is listed as "Call the bank tomorrow morning".

**Select a word or a sentence**
Double tap a word to select it. Triple tap to select the whole sentence.

**Organize with folders and tags**
In the editor, tap the folder chip and choose "New folder...". Tap "+ Tag" to add tags. Each note card shows the folder first, then the tags, and the last modification date on the right.

**Archive or delete many notes**
Long press a note, tap other notes to add them, then use the top bar: star, archive, export, or trash. A bottom message offers **Undo** for 5 seconds.

**Export notes to files**
Select three notes, tap the export button, choose "Export to PDF". Three PDF files appear in the Download folder, one per note. The same menu offers TXT and JSON.

**Create an encrypted backup**
Settings > Backup > "Create encrypted backup". Choose a backup password (it is independent from the app password) and where to save the `.notium` file.

**Restore on a new phone**
Install the APK, choose the language, tap "Import a backup", pick the `.notium` file, enter the backup password, check the summary, set a new main password, and tap "Create and import". A banner confirms that your notes are back.

---

## Dependencies

| Library | Version | Purpose |
|---|---|---|
| Kotlin | 2.0.21 | Language |
| Android Gradle Plugin | 8.7.3 | Build |
| Gradle | 8.10.2 | Build |
| Jetpack Compose BOM | 2024.10.01 | UI toolkit |
| Material 3 and Material Icons Extended | via BOM | Design system and icons |
| AndroidX Core KTX | 1.15.0 | Android extensions |
| AndroidX Core SplashScreen | 1.0.1 | Launch screen |
| AndroidX Activity Compose | 1.9.3 | Activity integration |
| Lifecycle ViewModel and Runtime Compose | 2.8.7 | MVVM |
| DataStore Preferences | 1.1.1 | Settings storage |
| AndroidX Biometric | 1.1.0 | Fingerprint and face unlock |
| SQLCipher for Android | 4.6.1 | Encrypted database |
| AndroidX SQLite KTX | 2.4.0 | SQLite helpers |
| Argon2Kt | 1.6.0 | Argon2id key derivation |
| AndroidX ProfileInstaller | 1.4.1 | Faster startup |

Build tools (downloaded at build time into `build-tools/tools`): Eclipse Temurin JDK 17, Android SDK platform 35, Android build-tools 34.0.0, Android command-line tools.

---

## Project structure

```
Notium-android/
├── README.md                          this file
├── Build-Notium-android.bat           build script for Windows
├── Build-Notium-android.sh            build script for Linux
├── Build-Notium-android.command       build script for macOS
├── build-tools/                       build engine, signing key, tools downloaded at build time
└── src/                               complete source code (Gradle project)
    └── app/src/main/java/com/notium/app/
        ├── MainActivity.kt, NotiumApp.kt, AppViewModel.kt
        ├── backup/      encrypted backup format
        ├── data/        database, repository, models, preferences
        ├── security/    cryptography, vault, Keystore, biometrics
        └── ui/          Compose screens, editor, theme, translations
```

---

## Limitations

- No cloud sync, no automatic backup: backups are files that you create and keep.
- The restore replaces all notes (there is no merge).
- A forgotten password cannot be recovered.
- Changing the password re-wraps the master key; it does not re-encrypt the whole database.
- The Linux build scripts support x86-64 processors only.
- Screen reader descriptions follow the selected app language, but note titles and folder names are your own text and are never translated.
- Not included, because they would need network access or extra permissions: voice recording, handwriting, cloud sync, online AI features.

---

## Troubleshooting

| Problem | What to do |
|---|---|
| The build fails | Read the red error lines in the terminal, or open `build-tools/build.log` , and send them to the developer |
| The build cannot download files | Check the internet connection and run the script again |
| Android refuses to install the APK | Allow installation from unknown sources. If an older version signed with a different key is installed, uninstall it first |
| The app closes at start | A crash report screen appears at the next start: copy the text and send it |
| Backup import says "wrong password or damaged file" | Check the backup password, which is not the app password, and that the file is complete |
| Forgot the password | It cannot be recovered. Restore an encrypted backup after reinstalling the app |

---

*Notium keeps your notes on your phone, encrypted, and nowhere else.*
